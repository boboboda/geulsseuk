package com.buyoungsilcoding.geulsseuk.core.ai

import com.buyoungsilcoding.geulsseuk.core.HttpTransport

/**
 * 앱에서 고를 수 있는 AI 제공사.
 * 기본 모델 이름은 바뀔 수 있어서 설정 화면에서 직접 고칠 수 있게 해 둡니다.
 * 비어 있는 기본 모델은 사용자가 공식 문서에서 '이미지 입력을 지원하는 모델'을 확인해 넣어야 합니다.
 */
enum class AiKind(
    val label: String,
    val defaultModel: String,
    val defaultBaseUrl: String,
    val keyUrl: String,
    val hint: String,
) {
    GEMINI(
        "Gemini (Google)", "gemini-2.5-flash-lite", "",
        "https://aistudio.google.com/apikey",
        "무료 한도가 있어 시작하기 좋아요. 무료 한도에서는 입력(사진·메모)이 Google의 모델 개선에 쓰일 수 있어요.",
    ),
    OPENROUTER(
        "OpenRouter", "openrouter/free", "https://openrouter.ai/api/v1",
        "https://openrouter.ai/keys",
        "여러 회사 모델을 한 키로 써요. ':free' 모델은 하루 요청 수 제한이 작고 입력이 기록될 수 있어요.",
    ),
    GROQ(
        "Groq", "", "https://api.groq.com/openai/v1",
        "https://console.groq.com/keys",
        "무료 한도가 있고 빨라요. 모델 이름은 Groq 문서에서 이미지 입력을 지원하는 모델로 확인해 넣어 주세요.",
    ),
    OPENAI(
        "OpenAI", "", "https://api.openai.com/v1",
        "https://platform.openai.com/api-keys",
        "유료 키가 필요해요. 모델 이름은 OpenAI 문서에서 이미지 입력을 지원하는 모델로 확인해 넣어 주세요.",
    ),
    CLAUDE(
        "Claude (Anthropic)", "claude-sonnet-5-5", "",
        "https://console.anthropic.com/",
        "유료 API 키가 필요해요. Claude 앱 구독(Pro)과는 별개예요.",
    ),
    CUSTOM(
        "OpenAI 호환 (직접 입력)", "", "",
        "",
        "주소·키·모델 이름을 모두 직접 입력해요. 'OpenAI 호환' 방식(/chat/completions)을 지원하는 서비스만 돼요.",
    );

    /** 주소를 따로 입력받는 제공사인지 여부. */
    val usesBaseUrl: Boolean get() = this != GEMINI && this != CLAUDE
}

object AiProviders {
    fun create(
        kind: AiKind,
        apiKey: String,
        model: String,
        baseUrl: String,
        transport: HttpTransport,
    ): AiProvider = when (kind) {
        AiKind.GEMINI -> GeminiProvider(apiKey, model, transport)
        AiKind.CLAUDE -> ClaudeProvider(apiKey, model, transport)
        else -> OpenAiCompatProvider(apiKey, model, baseUrl, transport)
    }
}
