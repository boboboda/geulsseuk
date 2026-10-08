package com.buyoungsilcoding.geulsseuk.core

/** AI에 보내는 지시문. 글 품질은 사실상 이 문서와 말투 샘플이 좌우합니다. */
object Prompt {

    fun system(topic: String, style: String): String = buildString {
        appendLine("당신은 네이버 블로그 글을 대신 써 주는 작성 도우미입니다.")
        if (topic.isNotBlank()) appendLine("블로그 주제: ${topic.trim()}")
        if (style.isNotBlank()) {
            appendLine()
            appendLine("[말투 샘플] 아래 글의 말투, 어미, 문장 길이, 이모지 사용 습관을 그대로 따라 쓰세요.")
            appendLine(style.trim())
            appendLine("[말투 샘플 끝]")
        }
        appendLine()
        appendLine("규칙:")
        appendLine("1. 사진과 메모에서 확인되는 내용만 쓰세요. 상호명, 가격, 장소, 날짜처럼 근거가 없는 사실은 지어내지 말고, 모르면 생략하세요.")
        appendLine("2. 구성은 도입, 본문(사진마다 1~2문단), 마무리 순서로 하세요.")
        appendLine("3. 사진은 [사진 N] 번호로 가리킵니다. 모든 사진을 한 번씩만, 글 흐름에 맞는 위치에 배치하세요.")
        appendLine("4. 사진 속 사람의 신원을 추측하거나 얼굴을 자세히 묘사하지 마세요.")
        appendLine("5. 광고처럼 과장하지 말고, 실제로 겪은 사람이 쓴 것처럼 자연스럽게 쓰세요.")
        appendLine("6. 출력은 아래 JSON 하나뿐입니다. 설명이나 코드블록 표시(```)를 붙이지 마세요.")
        appendLine("""{"title": "제목", "blocks": [{"type": "text", "text": "문단"}, {"type": "image", "index": 1, "caption": "사진 설명(선택)"}], "tags": ["태그1", "태그2"]}""")
    }

    /** 사진 앞에 [사진 N] 라벨을 붙여서, AI가 번호로 위치를 답할 수 있게 합니다. */
    fun user(memo: String, photos: List<Part.Image>): List<Part> {
        val parts = mutableListOf<Part>()
        parts += Part.Text("사진 ${photos.size}장과 메모를 드립니다. 사진은 [사진 1]부터 번호 순서대로 첨부됩니다.")
        photos.forEachIndexed { i, p ->
            parts += Part.Text("[사진 ${i + 1}]")
            parts += p
        }
        parts += Part.Text("메모:\n${memo.ifBlank { "(메모 없음)" }}\n\n위 사진과 메모로 블로그 글을 JSON으로 작성해 주세요.")
        return parts
    }

    const val RETRY_NUDGE: String =
        "직전 답변을 JSON으로 읽지 못했습니다. 설명 없이 JSON 객체 하나만 다시 출력해 주세요."
}
