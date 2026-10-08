package com.buyoungsilcoding.geulsseuk.core.ai

import com.buyoungsilcoding.geulsseuk.core.AiException
import com.buyoungsilcoding.geulsseuk.core.Part

/** 제공사마다 다른 요청 형식을 감싸는 공통 인터페이스. 답변 텍스트(JSON 문자열)만 돌려줍니다. */
interface AiProvider {
    fun complete(system: String, parts: List<Part>): String
}

internal fun requireSettings(apiKey: String, model: String) {
    if (apiKey.isBlank()) throw AiException("API 키가 비어 있어요. 설정에서 키를 입력해 주세요.")
    if (model.isBlank()) throw AiException("모델 이름이 비어 있어요. 설정에서 모델 이름을 입력해 주세요.")
}

internal fun base64(bytes: ByteArray): String = java.util.Base64.getEncoder().encodeToString(bytes)
