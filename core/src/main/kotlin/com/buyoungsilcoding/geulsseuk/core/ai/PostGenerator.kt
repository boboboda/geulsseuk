package com.buyoungsilcoding.geulsseuk.core.ai

import com.buyoungsilcoding.geulsseuk.core.AiException
import com.buyoungsilcoding.geulsseuk.core.Part
import com.buyoungsilcoding.geulsseuk.core.Post
import com.buyoungsilcoding.geulsseuk.core.PostParseException
import com.buyoungsilcoding.geulsseuk.core.PostParser
import com.buyoungsilcoding.geulsseuk.core.Prompt

/** 사진+메모 → 글 초안. JSON을 못 읽으면 딱 한 번만 다시 요청합니다. */
class PostGenerator(private val provider: AiProvider) {

    fun generate(topic: String, style: String, memo: String, photos: List<Part.Image>): Post {
        val system = Prompt.system(topic, style)
        val parts = Prompt.user(memo, photos)

        val first = provider.complete(system, parts)
        return try {
            PostParser.parse(first, photos.size)
        } catch (firstError: PostParseException) {
            val second = provider.complete(system, parts + Part.Text(Prompt.RETRY_NUDGE))
            try {
                PostParser.parse(second, photos.size)
            } catch (e: PostParseException) {
                throw AiException("AI가 올바른 형식으로 답하지 못했어요. 다시 시도해 주세요.", cause = e)
            }
        }
    }
}
