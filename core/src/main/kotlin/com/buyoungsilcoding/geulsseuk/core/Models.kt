package com.buyoungsilcoding.geulsseuk.core

/** AI에 보내는 입력 조각: 글 또는 사진. */
sealed interface Part {
    data class Text(val text: String) : Part
    class Image(val bytes: ByteArray, val mime: String = "image/jpeg") : Part
}

/** 글을 이루는 덩어리: 문단 또는 사진. 사진 번호는 1부터 시작합니다. */
sealed interface PostBlock {
    data class Paragraph(val text: String) : PostBlock
    data class Photo(val number: Int, val caption: String = "") : PostBlock
}

data class Post(
    val title: String,
    val blocks: List<PostBlock>,
    val tags: List<String>,
) {
    /**
     * 없는 사진 번호와 중복 사진은 빼고, 글에서 빠진 사진은 끝에 붙여서
     * "모든 사진이 정확히 한 번씩" 들어가도록 보정합니다.
     */
    fun normalized(photoCount: Int): Post {
        val seen = mutableSetOf<Int>()
        val kept = blocks.filter { b ->
            when (b) {
                is PostBlock.Paragraph -> true
                is PostBlock.Photo -> b.number in 1..photoCount && seen.add(b.number)
            }
        }.toMutableList()
        for (n in 1..photoCount) {
            if (n !in seen) kept += PostBlock.Photo(n)
        }
        return copy(blocks = kept)
    }

    /** 네이버 에디터에 붙여넣기용 텍스트. 사진 위치는 [사진 N]으로 표시합니다. */
    fun toPlainText(): String = buildString {
        appendLine(title)
        appendLine()
        for (b in blocks) {
            when (b) {
                is PostBlock.Paragraph -> appendLine(b.text)
                is PostBlock.Photo -> appendLine(
                    "[사진 ${b.number}]" + if (b.caption.isBlank()) "" else " ${b.caption}"
                )
            }
            appendLine()
        }
        if (tags.isNotEmpty()) appendLine(tags.joinToString(" ") { "#$it" })
    }.trimEnd()
}
