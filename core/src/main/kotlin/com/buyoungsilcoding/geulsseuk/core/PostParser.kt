package com.buyoungsilcoding.geulsseuk.core

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class PostParseException(message: String) : Exception(message)

/** AI가 돌려준 JSON 문자열을 [Post]로 바꿉니다. 코드블록이나 앞뒤 설명이 섞여도 JSON 부분만 뽑아 읽습니다. */
object PostParser {

    fun extractJson(raw: String): String {
        var s = raw.trim()
        if (s.startsWith("```")) {
            s = s.removePrefix("```json").removePrefix("```JSON").removePrefix("```").trim()
            s = s.removeSuffix("```").trim()
        }
        val start = s.indexOf('{')
        val end = s.lastIndexOf('}')
        if (start < 0 || end <= start) throw PostParseException("JSON을 찾지 못했어요.")
        return s.substring(start, end + 1)
    }

    fun parse(raw: String, photoCount: Int): Post {
        val root: JsonObject = try {
            lenientJson.parseToJsonElement(extractJson(raw)) as? JsonObject
                ?: throw PostParseException("글 형식이 올바르지 않아요.")
        } catch (e: PostParseException) {
            throw e
        } catch (e: Exception) {
            throw PostParseException("글 형식을 읽지 못했어요.")
        }

        val title = root["title"].str().trim()
        if (title.isEmpty()) throw PostParseException("제목이 비어 있어요.")

        val blocks = mutableListOf<PostBlock>()
        for (el in (root["blocks"] as? JsonArray).orEmpty()) {
            val b = el as? JsonObject ?: continue
            if (b["type"].str() == "image") {
                val n = (b["index"].str().ifEmpty { b["number"].str() }).toIntOrNull() ?: continue
                blocks += PostBlock.Photo(n, b["caption"].str().trim())
            } else {
                val t = b["text"].str().trim()
                if (t.isNotEmpty()) blocks += PostBlock.Paragraph(t)
            }
        }
        if (blocks.none { it is PostBlock.Paragraph }) throw PostParseException("본문이 비어 있어요.")

        val tags = (root["tags"] as? JsonArray).orEmpty()
            .map { it.str().replace("#", "").trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(10)

        return Post(title, blocks, tags).normalized(photoCount)
    }

    private fun JsonElement?.str(): String = (this as? JsonPrimitive)?.contentOrNull ?: ""
}
