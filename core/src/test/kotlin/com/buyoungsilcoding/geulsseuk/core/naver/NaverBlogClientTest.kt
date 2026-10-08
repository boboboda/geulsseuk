package com.buyoungsilcoding.geulsseuk.core.naver

import com.buyoungsilcoding.geulsseuk.core.HttpResponse
import com.buyoungsilcoding.geulsseuk.core.HttpTransport
import com.buyoungsilcoding.geulsseuk.core.Post
import com.buyoungsilcoding.geulsseuk.core.PostBlock
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NaverBlogClientTest {

    private class Recorder(val replies: ArrayDeque<() -> HttpResponse>) : HttpTransport {
        val urls = mutableListOf<String>()
        val bodies = mutableListOf<String>()
        override fun post(url: String, headers: Map<String, String>, body: String): HttpResponse {
            urls += url
            bodies += body
            return replies.removeFirst().invoke()
        }
    }

    private fun ok(value: String) = HttpResponse(
        200, "<methodResponse><params><param><value>$value</value></param></params></methodResponse>"
    )

    @Test
    fun uploadsPhotosThenPostsWithUploadedUrls() {
        val uploadReply: () -> HttpResponse =
            { ok("<struct><member><name>url</name><value><string>https://f.example/1.jpg</string></value></member></struct>") }
        val postReply: () -> HttpResponse = { ok("<string>987</string>") }
        val t = Recorder(ArrayDeque(listOf(uploadReply, postReply)))
        val client = NaverBlogClient("myid", "secret-pass", t)

        val url = client.uploadImage("photo_1.jpg", "image/jpeg", byteArrayOf(9, 9))
        assertEquals("https://f.example/1.jpg", url)

        val post = Post("제목", listOf(PostBlock.Paragraph("본문"), PostBlock.Photo(1, "설명")), listOf("태그"))
        val id = client.newPost(post.title, HtmlBuilder.build(post, mapOf(1 to url)), publish = false)
        assertEquals("987", id)

        assertEquals(listOf(NaverBlogClient.DEFAULT_ENDPOINT, NaverBlogClient.DEFAULT_ENDPOINT), t.urls)
        assertTrue("metaWeblog.newMediaObject" in t.bodies[0])
        assertTrue("<name>bits</name>" in t.bodies[0])
        assertTrue("metaWeblog.newPost" in t.bodies[1])
        assertTrue("<boolean>0</boolean>" in t.bodies[1])
        assertTrue("https://f.example/1.jpg" in t.bodies[1])
        // 블로그 ID, 사용자 ID, 암호 순서
        assertTrue(Regex("<string>myid</string>.*<string>myid</string>.*<string>secret-pass</string>").containsMatchIn(t.bodies[1]))
    }

    @Test
    fun faultMessageNeverContainsPassword() {
        val fault = """<methodResponse><fault><value><struct>
            <member><name>faultCode</name><value><int>1</int></value></member>
            <member><name>faultString</name><value><string>bad credentials</string></value></member>
            </struct></value></fault></methodResponse>"""
        val faultReply: () -> HttpResponse = { HttpResponse(200, fault) }
        val client = NaverBlogClient("myid", "secret-pass", Recorder(ArrayDeque(listOf(faultReply))))
        val e = assertFailsWith<NaverException> { client.newPost("t", "<p>x</p>", false) }
        assertFalse("secret-pass" in e.message.orEmpty())
        assertTrue("API 연결 암호" in e.message.orEmpty())
    }

    @Test
    fun mapsNetworkAndHttpAndProtocolErrors() {
        val netReply: () -> HttpResponse = { throw IOException("down") }
        val net = NaverBlogClient("i", "p", Recorder(ArrayDeque(listOf(netReply))))
        assertTrue("연결하지 못했어요" in assertFailsWith<NaverException> { net.newPost("t", "h", false) }.message.orEmpty())

        val httpReply: () -> HttpResponse = { HttpResponse(503, "") }
        val http = NaverBlogClient("i", "p", Recorder(ArrayDeque(listOf(httpReply))))
        assertTrue("503" in assertFailsWith<NaverException> { http.newPost("t", "h", false) }.message.orEmpty())

        val protoReply: () -> HttpResponse = { HttpResponse(200, "<html/>") }
        val proto = NaverBlogClient("i", "p", Recorder(ArrayDeque(listOf(protoReply))))
        assertTrue("바뀌었을 수" in assertFailsWith<NaverException> { proto.newPost("t", "h", false) }.message.orEmpty())
    }

    @Test
    fun htmlBuilderEscapesAndSplitsParagraphs() {
        val post = Post(
            "t",
            listOf(
                PostBlock.Paragraph("첫째 <b>줄</b>\n둘째 줄\n\n새 문단 & 끝"),
                PostBlock.Photo(1, "설명 \"따옴표\""),
                PostBlock.Photo(2), // 업로드 실패해 주소가 없으면 건너뜀
            ),
            listOf("a", "b"),
        )
        val html = HtmlBuilder.build(post, mapOf(1 to "https://f.example/a.jpg?x=1&y=2"))
        assertTrue("<p>첫째 &lt;b&gt;줄&lt;/b&gt;<br />둘째 줄</p>" in html)
        assertTrue("<p>새 문단 &amp; 끝</p>" in html)
        assertTrue("src=\"https://f.example/a.jpg?x=1&amp;y=2\"" in html)
        assertTrue("설명 &quot;따옴표&quot;" in html)
        assertEquals(1, Regex("<img ").findAll(html).count())
        assertTrue(html.trimEnd().endsWith("<p>#a #b</p>"))
    }
}
