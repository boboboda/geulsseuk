package com.buyoungsilcoding.geulsseuk.core.ai

import com.buyoungsilcoding.geulsseuk.core.AiException
import com.buyoungsilcoding.geulsseuk.core.HttpTransport
import com.buyoungsilcoding.geulsseuk.core.Part
import com.buyoungsilcoding.geulsseuk.core.requireOkJson
import java.io.IOException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * OpenAI 방식(/chat/completions)을 따르는 서비스용: OpenAI, OpenRouter, Groq, 직접 입력.
 * 주소·키·모델 이름만 바꿔서 여러 서비스를 지원합니다.
 */
class OpenAiCompatProvider(
    private val apiKey: String,
    private val model: String,
    private val baseUrl: String,
    private val transport: HttpTransport,
) : AiProvider {

    override fun complete(system: String, parts: List<Part>): String {
        requireSettings(apiKey, model)
        if (baseUrl.isBlank()) throw AiException("서비스 주소가 비어 있어요. 설정에서 주소를 입력해 주세요.")

        val body = buildJsonObject {
            put("model", model)
            putJsonArray("messages") {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", system)
                })
                add(buildJsonObject {
                    put("role", "user")
                    putJsonArray("content") {
                        parts.forEach { p ->
                            add(buildJsonObject {
                                when (p) {
                                    is Part.Text -> {
                                        put("type", "text")
                                        put("text", p.text)
                                    }
                                    is Part.Image -> {
                                        put("type", "image_url")
                                        putJsonObject("image_url") {
                                            put("url", "data:${p.mime};base64,${base64(p.bytes)}")
                                        }
                                    }
                                }
                            })
                        }
                    }
                })
            }
        }

        val url = baseUrl.trim().trimEnd('/') + "/chat/completions"
        val res = try {
            transport.post(
                url,
                mapOf("Authorization" to "Bearer $apiKey", "Content-Type" to "application/json"),
                body.toString(),
            )
        } catch (e: IOException) {
            throw AiException("AI에 연결하지 못했어요. 인터넷 연결을 확인해 주세요.", cause = e)
        }

        val message = res.requireOkJson()["choices"]?.jsonArray?.firstOrNull()?.jsonObject?.get("message")?.jsonObject
        val content = message?.get("content")
        val text = when (content) {
            is JsonPrimitive -> content.contentOrNull
            is JsonArray -> content.mapNotNull { (it.jsonObject["text"] as? JsonPrimitive)?.contentOrNull }.joinToString("")
            else -> null
        }
        if (text.isNullOrBlank()) throw AiException("AI 응답이 비어 있어요. 이미지 입력을 지원하는 모델인지 확인해 주세요.")
        return text
    }
}
