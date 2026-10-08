package com.buyoungsilcoding.geulsseuk.core.ai

import com.buyoungsilcoding.geulsseuk.core.AiException
import com.buyoungsilcoding.geulsseuk.core.HttpTransport
import com.buyoungsilcoding.geulsseuk.core.Part
import com.buyoungsilcoding.geulsseuk.core.requireOkJson
import java.io.IOException
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** Google Gemini REST (generateContent). 키는 URL이 아니라 헤더로 보냅니다. */
class GeminiProvider(
    private val apiKey: String,
    private val model: String,
    private val transport: HttpTransport,
) : AiProvider {

    override fun complete(system: String, parts: List<Part>): String {
        requireSettings(apiKey, model)
        val body = buildJsonObject {
            putJsonObject("systemInstruction") {
                putJsonArray("parts") { add(buildJsonObject { put("text", system) }) }
            }
            putJsonArray("contents") {
                add(buildJsonObject {
                    put("role", "user")
                    putJsonArray("parts") {
                        parts.forEach { p ->
                            add(buildJsonObject {
                                when (p) {
                                    is Part.Text -> put("text", p.text)
                                    is Part.Image -> putJsonObject("inline_data") {
                                        put("mime_type", p.mime)
                                        put("data", base64(p.bytes))
                                    }
                                }
                            })
                        }
                    }
                })
            }
            putJsonObject("generationConfig") { put("responseMimeType", "application/json") }
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/${model.removePrefix("models/")}:generateContent"
        val res = try {
            transport.post(url, mapOf("x-goog-api-key" to apiKey, "Content-Type" to "application/json"), body.toString())
        } catch (e: IOException) {
            throw AiException("AI에 연결하지 못했어요. 인터넷 연결을 확인해 주세요.", cause = e)
        }

        val root = res.requireOkJson()
        val text = root["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("content")?.jsonObject?.get("parts")?.jsonArray
            ?.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull }
            ?.joinToString("")
        if (text.isNullOrBlank()) {
            val blocked = root["promptFeedback"]?.jsonObject?.get("blockReason") != null
            throw AiException(
                if (blocked) "AI가 이 내용에 대한 답변을 거부했어요. 다른 사진으로 시도해 주세요."
                else "AI 응답이 비어 있어요. 다시 시도해 주세요."
            )
        }
        return text
    }
}
