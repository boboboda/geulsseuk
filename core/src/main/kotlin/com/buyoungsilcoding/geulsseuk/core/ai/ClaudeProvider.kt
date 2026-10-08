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

/** Anthropic Messages API. */
class ClaudeProvider(
    private val apiKey: String,
    private val model: String,
    private val transport: HttpTransport,
    private val maxTokens: Int = 4096,
) : AiProvider {

    override fun complete(system: String, parts: List<Part>): String {
        requireSettings(apiKey, model)
        val body = buildJsonObject {
            put("model", model)
            put("max_tokens", maxTokens)
            put("system", system)
            putJsonArray("messages") {
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
                                        put("type", "image")
                                        putJsonObject("source") {
                                            put("type", "base64")
                                            put("media_type", p.mime)
                                            put("data", base64(p.bytes))
                                        }
                                    }
                                }
                            })
                        }
                    }
                })
            }
        }

        val res = try {
            transport.post(
                "https://api.anthropic.com/v1/messages",
                mapOf(
                    "x-api-key" to apiKey,
                    "anthropic-version" to "2023-06-01",
                    "Content-Type" to "application/json",
                ),
                body.toString(),
            )
        } catch (e: IOException) {
            throw AiException("AI에 연결하지 못했어요. 인터넷 연결을 확인해 주세요.", cause = e)
        }

        val text = res.requireOkJson()["content"]?.jsonArray
            ?.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull }
            ?.joinToString("")
        if (text.isNullOrBlank()) throw AiException("AI 응답이 비어 있어요. 다시 시도해 주세요.")
        return text
    }
}
