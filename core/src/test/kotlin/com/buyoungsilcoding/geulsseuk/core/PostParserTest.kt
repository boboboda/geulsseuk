package com.buyoungsilcoding.geulsseuk.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PostParserTest {

    private val good = """
        {"title": "제주 여행 1일차", "blocks": [
          {"type": "text", "text": "도입 문단"},
          {"type": "image", "index": 1, "caption": "바닷가"},
          {"type": "text", "text": "본문 문단"},
          {"type": "image", "index": "2"}
        ], "tags": ["#제주", "여행", "제주", ""]}
    """.trimIndent()

    @Test
    fun parsesPlainJson() {
        val post = PostParser.parse(good, photoCount = 2)
        assertEquals("제주 여행 1일차", post.title)
        assertEquals(4, post.blocks.size)
        assertEquals(PostBlock.Photo(1, "바닷가"), post.blocks[1])
        assertEquals(PostBlock.Photo(2, ""), post.blocks[3]) // 문자열 번호도 허용
        assertEquals(listOf("제주", "여행"), post.tags) // '#' 제거, 중복·빈 값 제거
    }

    @Test
    fun parsesJsonInsideCodeFenceAndChatter() {
        val raw = "알겠습니다. 결과입니다.\n```json\n$good\n```\n도움이 되길 바랍니다."
        assertEquals("제주 여행 1일차", PostParser.parse(raw, 2).title)
    }

    @Test
    fun missingPhotosAreAppendedAndInvalidOnesDropped() {
        val raw = """{"title":"t","blocks":[{"type":"text","text":"a"},{"type":"image","index":9},{"type":"image","index":1},{"type":"image","index":1}]}"""
        val post = PostParser.parse(raw, photoCount = 3)
        val photos = post.blocks.filterIsInstance<PostBlock.Photo>().map { it.number }
        assertEquals(listOf(1, 2, 3), photos) // 9번 제거, 중복 1번 제거, 빠진 2·3번 추가
    }

    @Test
    fun rejectsBrokenOrEmptyOutput() {
        assertFailsWith<PostParseException> { PostParser.parse("그냥 말로만 답했어요", 1) }
        assertFailsWith<PostParseException> { PostParser.parse("""{"title":"","blocks":[]}""", 1) }
        assertFailsWith<PostParseException> { PostParser.parse("""{"title":"t","blocks":[{"type":"image","index":1}]}""", 1) }
        assertFailsWith<PostParseException> { PostParser.parse("{ 깨진 json", 1) }
    }

    @Test
    fun plainTextMarksPhotoPositions() {
        val text = PostParser.parse(good, 2).toPlainText()
        assertTrue(text.startsWith("제주 여행 1일차"))
        assertTrue("[사진 1] 바닷가" in text)
        assertTrue("[사진 2]" in text)
        assertTrue(text.endsWith("#제주 #여행"))
    }
}
