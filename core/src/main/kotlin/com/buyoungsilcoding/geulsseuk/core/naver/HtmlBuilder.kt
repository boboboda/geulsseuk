package com.buyoungsilcoding.geulsseuk.core.naver

import com.buyoungsilcoding.geulsseuk.core.Post
import com.buyoungsilcoding.geulsseuk.core.PostBlock

/** 글 구조를 네이버에 올릴 HTML 본문으로 바꿉니다. 모든 글자는 이스케이프해서 넣습니다. */
object HtmlBuilder {

    fun build(post: Post, imageUrls: Map<Int, String>): String = buildString {
        for (b in post.blocks) {
            when (b) {
                is PostBlock.Paragraph -> {
                    for (para in b.text.trim().split(Regex("\\n{2,}"))) {
                        if (para.isBlank()) continue
                        append("<p>").append(escape(para.trim()).replace("\n", "<br />")).append("</p>\n")
                    }
                }
                is PostBlock.Photo -> {
                    val url = imageUrls[b.number] ?: continue
                    append("<p style=\"text-align:center;\"><img src=\"")
                        .append(escape(url)).append("\" /></p>\n")
                    if (b.caption.isNotBlank()) {
                        append("<p style=\"text-align:center;font-size:13px;color:#777777;\">")
                            .append(escape(b.caption.trim())).append("</p>\n")
                    }
                }
            }
        }
        if (post.tags.isNotEmpty()) {
            append("<p>").append(post.tags.joinToString(" ") { "#" + escape(it) }).append("</p>\n")
        }
    }

    fun escape(s: String): String = XmlRpc.escape(s)
}
