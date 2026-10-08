package com.buyoungsilcoding.geulsseuk.core.naver

import com.buyoungsilcoding.geulsseuk.core.HttpTransport
import java.io.IOException

/** 사용자에게 보여줘도 되는 쉬운 한국어 메시지를 담습니다. API 연결 암호는 절대 담지 않습니다. */
class NaverException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * 네이버 블로그 글쓰기 API 연결(XML-RPC, MetaWeblog 방식) 클라이언트.
 * - 블로그 관리 > 글쓰기 API 연결에서 발급한 'API 연결 암호'를 씁니다. (로그인 비밀번호가 아님)
 * - 이 방식이 지금도 동작하는지는 tools/api_test/naver_api_test.py 로 먼저 확인하세요.
 */
class NaverBlogClient(
    private val blogId: String,
    private val apiPassword: String,
    private val transport: HttpTransport,
    private val endpoint: String = DEFAULT_ENDPOINT,
) {
    companion object {
        const val DEFAULT_ENDPOINT = "https://api.blog.naver.com/xmlrpc"
    }

    /** 사진 1장을 올리고, 본문 <img>에 넣을 주소를 돌려줍니다. */
    fun uploadImage(name: String, mime: String, bytes: ByteArray): String {
        val result = call(
            "metaWeblog.newMediaObject",
            listOf(
                XmlRpcValue.Str(blogId),
                XmlRpcValue.Str(blogId),
                XmlRpcValue.Str(apiPassword),
                XmlRpcValue.Struct(
                    linkedMapOf(
                        "name" to XmlRpcValue.Str(name),
                        "type" to XmlRpcValue.Str(mime),
                        "bits" to XmlRpcValue.Bytes(bytes),
                    )
                ),
            ),
        )
        val url = when (result) {
            is XmlRpcValue.Struct -> (result.members["url"] ?: result.members["URL"]) as? XmlRpcValue.Str
            is XmlRpcValue.Str -> result
            else -> null
        }?.value
        if (url.isNullOrBlank()) throw NaverException("사진을 올렸지만 주소를 받지 못했어요.")
        return url
    }

    /** 글을 등록하고 글 번호를 돌려줍니다. publish=false가 비공개/임시저장 중 무엇인지는 테스트로 확인해야 합니다. */
    fun newPost(title: String, html: String, publish: Boolean): String {
        val result = call(
            "metaWeblog.newPost",
            listOf(
                XmlRpcValue.Str(blogId),
                XmlRpcValue.Str(blogId),
                XmlRpcValue.Str(apiPassword),
                XmlRpcValue.Struct(
                    linkedMapOf(
                        "title" to XmlRpcValue.Str(title),
                        "description" to XmlRpcValue.Str(html),
                    )
                ),
                XmlRpcValue.Bool(publish),
            ),
        )
        return when (result) {
            is XmlRpcValue.Str -> result.value
            is XmlRpcValue.Int32 -> result.value.toString()
            else -> ""
        }
    }

    private fun call(method: String, params: List<XmlRpcValue>): XmlRpcValue {
        val res = try {
            transport.post(
                endpoint,
                mapOf("Content-Type" to "text/xml; charset=utf-8"),
                XmlRpc.buildCall(method, params),
            )
        } catch (e: IOException) {
            throw NaverException("네이버에 연결하지 못했어요. 인터넷 연결을 확인해 주세요.", e)
        }
        if (!res.isOk) throw NaverException("네이버 서버가 오류를 돌려줬어요 (코드 ${res.status}).")

        return try {
            XmlRpc.parseResponse(res.body)
        } catch (f: XmlRpcFault) {
            val detail = f.faultString.take(120)
            throw NaverException(
                "네이버가 요청을 거절했어요. 네이버 ID와 'API 연결 암호'를 확인해 주세요. (코드 ${f.code}${if (detail.isBlank()) "" else ", $detail"})",
                f,
            )
        } catch (e: XmlRpcProtocolException) {
            throw NaverException("네이버 응답을 이해하지 못했어요. 네이버 쪽 방식이 바뀌었을 수 있어요.", e)
        }
    }
}
