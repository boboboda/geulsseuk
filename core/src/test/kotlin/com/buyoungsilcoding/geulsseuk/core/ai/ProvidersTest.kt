package com.buyoungsilcoding.geulsseuk.core.ai

import com.buyoungsilcoding.geulsseuk.core.AiException
import com.buyoungsilcoding.geulsseuk.core.HttpResponse
import com.buyoungsilcoding.geulsseuk.core.HttpTransport
import com.buyoungsilcoding.geulsseuk.core.Part
import com.buyoungsilcoding.geulsseuk.core.PostBlock
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ProvidersTest {

    private class Fake(val replies: ArrayDeque<HttpResponse>) : HttpTransport {
        var url = ""
        var headers: Map<String, String> = emptyMap()
        val bodies = mutableListOf<String>()
        override fun post(url: String, headers: Map<String, String>, body: String): HttpResponse {
            this.url = url
            this.headers = headers
            bodies += body
            return replies.removeFirst()
        }
        fun lastBody(): JsonObject = Json.parseToJsonElement(bodies.last()).jsonObject
    }

    private fun fake(vararg r: HttpResponse) = Fake(ArrayDeque(r.toList()))

    private val img = Part.Image(byteArrayOf(1, 2, 3), "image/jpeg")
    private val parts = listOf(Part.Text("[사진 1]"), img, Part.Text("메모"))

    private val goodPostJson =
        """{"title":"제목","blocks":[{"type":"text","text":"본문"},{"type":"image","index":1}],"tags":["a"]}"""

    @Test
    fun geminiRequestShapeAndResponse() {
        val reply = """{"candidates":[{"content":{"parts":[{"text":"안녕"},{"text":"하세요"}]}}]}"""
        val t = fake(HttpResponse(200, reply))
        val out = GeminiProvider("KEY", "models/gemini-x", t).complete("시스템", parts)

        assertEquals("안녕하세요", out)
        assertEquals("https://generativelanguage.googleapis.com/v1beta/models/gemini-x:generateContent", t.url)
        assertEquals("KEY", t.headers["x-goog-api-key"])
        assertFalse("KEY" in t.url) // 키는 URL에 넣지 않음
        val body = t.lastBody()
        assertEquals("시스템", body["systemInstruction"]!!.jsonObject["parts"]!!.jsonArray[0].jsonObject["text"]!!.jsonPrimitive.content)
        val ps = body["contents"]!!.jsonArray[0].jsonObject["parts"]!!.jsonArray
        assertEquals("[사진 1]", ps[0].jsonObject["text"]!!.jsonPrimitive.content)
        assertEquals("AQID", ps[1].jsonObject["inline_data"]!!.jsonObject["data"]!!.jsonPrimitive.content)
        assertEquals("application/json", body["generationConfig"]!!.jsonObject["responseMimeType"]!!.jsonPrimitive.content)
    }

    @Test
    fun geminiBlockedPromptGivesFriendlyError() {
        val t = fake(HttpResponse(200, """{"promptFeedback":{"blockReason":"SAFETY"}}"""))
        val e = assertFailsWith<AiException> { GeminiProvider("K", "m", t).complete("s", parts) }
        assertTrue("거부" in e.message.orEmpty())
    }

    @Test
    fun claudeRequestShapeAndResponse() {
        val t = fake(HttpResponse(200, """{"content":[{"type":"text","text":"결과"}]}"""))
        val out = ClaudeProvider("KEY", "claude-sonnet-5-5", t).complete("시스템", parts)

        assertEquals("결과", out)
        assertEquals("https://api.anthropic.com/v1/messages", t.url)
        assertEquals("KEY", t.headers["x-api-key"])
        assertEquals("2023-06-01", t.headers["anthropic-version"])
        val body = t.lastBody()
        assertEquals("claude-sonnet-5-5", body["model"]!!.jsonPrimitive.content)
        assertEquals("시스템", body["system"]!!.jsonPrimitive.content)
        val content = body["messages"]!!.jsonArray[0].jsonObject["content"]!!.jsonArray
        val image = content[1].jsonObject
        assertEquals("image", image["type"]!!.jsonPrimitive.content)
        assertEquals("AQID", image["source"]!!.jsonObject["data"]!!.jsonPrimitive.content)
        assertEquals("image/jpeg", image["source"]!!.jsonObject["media_type"]!!.jsonPrimitive.content)
    }

    @Test
    fun openAiCompatRequestShapeAndResponse() {
        val t = fake(HttpResponse(200, """{"choices":[{"message":{"content":"응답"}}]}"""))
        val out = OpenAiCompatProvider("KEY", "some-model", "https://api.example.com/v1/", t).complete("시스템", parts)

        assertEquals("응답", out)
        assertEquals("https://api.example.com/v1/chat/completions", t.url) // 끝 슬래시 정리
        assertEquals("Bearer KEY", t.headers["Authorization"])
        val msgs = t.lastBody()["messages"]!!.jsonArray
        assertEquals("system", msgs[0].jsonObject["role"]!!.jsonPrimitive.content)
        val user = msgs[1].jsonObject["content"]!!.jsonArray
        assertEquals("data:image/jpeg;base64,AQID", user[1].jsonObject["image_url"]!!.jsonObject["url"]!!.jsonPrimitive.content)
    }

    @Test
    fun openAiCompatAcceptsArrayContent() {
        val t = fake(HttpResponse(200, """{"choices":[{"message":{"content":[{"type":"text","text":"가"},{"type":"text","text":"나"}]}}]}"""))
        assertEquals("가나", OpenAiCompatProvider("K", "m", "https://x", t).complete("s", parts))
    }

    @Test
    fun httpAndNetworkAndSettingErrorsAreFriendlyAndNeverLeakKey() {
        for ((status, word) in listOf(401 to "API 키", 404 to "모델 이름", 429 to "한도", 500 to "서버")) {
            val e = assertFailsWith<AiException> {
                ClaudeProvider("SECRET-KEY", "m", fake(HttpResponse(status, "{}"))).complete("s", parts)
            }
            assertTrue(word in e.message.orEmpty(), "status $status -> ${e.message}")
            assertFalse("SECRET-KEY" in e.message.orEmpty())
        }
        val net = object : HttpTransport {
            override fun post(url: String, headers: Map<String, String>, body: String): HttpResponse =
                throw IOException("x")
        }
        assertTrue("연결" in assertFailsWith<AiException> { GeminiProvider("K", "m", net).complete("s", parts) }.message.orEmpty())

        assertTrue("키" in assertFailsWith<AiException> { GeminiProvider("", "m", fake()).complete("s", parts) }.message.orEmpty())
        assertTrue("모델" in assertFailsWith<AiException> { ClaudeProvider("K", "", fake()).complete("s", parts) }.message.orEmpty())
        assertTrue("주소" in assertFailsWith<AiException> { OpenAiCompatProvider("K", "m", "", fake()).complete("s", parts) }.message.orEmpty())
    }

    @Test
    fun generatorRetriesOnceThenSucceeds() {
        val calls = mutableListOf<List<Part>>()
        val replies = ArrayDeque(listOf("말로만 대답", goodPostJson))
        val provider = object : AiProvider {
            override fun complete(system: String, parts: List<Part>): String {
                calls += parts
                return replies.removeFirst()
            }
        }
        val post = PostGenerator(provider).generate("주제", "", "메모", listOf(img))
        assertEquals(2, calls.size)
        assertTrue((calls[1].last() as Part.Text).text.contains("JSON"))
        assertEquals("제목", post.title)
        assertTrue(post.blocks.last() is PostBlock.Photo)
    }

    @Test
    fun generatorGivesUpAfterSecondFailure() {
        val provider = AiProvider { _, _ -> "계속 말로만" }
        val e = assertFailsWith<AiException> { PostGenerator(provider).generate("", "", "", listOf(img)) }
        assertTrue("형식" in e.message.orEmpty())
    }

    @Test
    fun promptLabelsEachPhotoInOrder() {
        val ps = com.buyoungsilcoding.geulsseuk.core.Prompt.user("오늘 메모", listOf(img, img))
        val labels = ps.filterIsInstance<Part.Text>().map { it.text }.filter { it.startsWith("[사진") }
        assertEquals(listOf("[사진 1]", "[사진 2]"), labels)
        assertTrue(ps.indexOf(ps.first { it is Part.Image }) == 2) // 라벨 바로 다음에 사진
        val sys = com.buyoungsilcoding.geulsseuk.core.Prompt.system("육아", "말투 샘플입니다")
        assertTrue("육아" in sys && "말투 샘플입니다" in sys && "지어내지" in sys)
    }
}

private fun AiProvider(block: (String, List<Part>) -> String): AiProvider = object : AiProvider {
    override fun complete(system: String, parts: List<Part>): String = block(system, parts)
}
