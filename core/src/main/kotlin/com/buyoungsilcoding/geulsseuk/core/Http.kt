package com.buyoungsilcoding.geulsseuk.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

data class HttpResponse(val status: Int, val body: String) {
    val isOk: Boolean get() = status in 200..299
}

/**
 * 네트워크 접근을 추상화한 인터페이스. 앱에서는 OkHttp로, 테스트에서는 가짜 구현으로 바꿉니다.
 * 동기 호출이므로 앱에서는 반드시 IO 스레드에서 부르세요.
 * 연결 실패 같은 네트워크 오류는 java.io.IOException으로 던집니다.
 */
fun interface HttpTransport {
    fun post(url: String, headers: Map<String, String>, body: String): HttpResponse
}

/** 사용자에게 그대로 보여줘도 되는 쉬운 한국어 메시지를 담는 오류. API 키는 절대 메시지에 넣지 않습니다. */
class AiException(message: String, val status: Int? = null, cause: Throwable? = null) :
    Exception(message, cause)

fun friendlyHttpError(status: Int): String = when (status) {
    400 -> "요청이 올바르지 않아요. 설정의 모델 이름이나 사진 수를 확인해 주세요."
    401, 403 -> "API 키가 올바르지 않거나 권한이 없어요. 설정에서 키를 다시 확인해 주세요."
    404 -> "모델 이름이나 주소를 찾지 못했어요. 설정의 모델 이름을 확인해 주세요."
    413 -> "사진이 너무 크거나 많아요. 사진 수를 줄여 주세요."
    429 -> "사용 한도를 넘었어요. 잠시 뒤에 다시 시도하거나 다른 키/모델을 써 주세요."
    in 500..599 -> "AI 서버에 문제가 있어요. 잠시 뒤에 다시 시도해 주세요."
    else -> "요청에 실패했어요 (코드 $status)."
}

internal val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true }

internal fun HttpResponse.requireOkJson(): JsonObject {
    if (!isOk) throw AiException(friendlyHttpError(status), status)
    return try {
        lenientJson.parseToJsonElement(body).jsonObject
    } catch (e: Exception) {
        throw AiException("AI 응답을 읽지 못했어요. 주소와 모델 이름을 확인해 주세요.", cause = e)
    }
}
