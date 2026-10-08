package com.buyoungsilcoding.geulsseuk.data

import com.buyoungsilcoding.geulsseuk.core.HttpResponse
import com.buyoungsilcoding.geulsseuk.core.HttpTransport
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/** 실제 네트워크 전송. 블로킹이므로 반드시 백그라운드(Dispatchers.IO)에서 호출하세요. */
class OkHttpTransport : HttpTransport {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    override fun post(url: String, headers: Map<String, String>, body: String): HttpResponse {
        val ct = headers.entries.firstOrNull { it.key.equals("Content-Type", true) }?.value
            ?: "application/json; charset=utf-8"
        val builder = Request.Builder().url(url)
        headers.forEach { (k, v) -> if (!k.equals("Content-Type", true)) builder.header(k, v) }
        builder.post(body.toRequestBody(ct.toMediaType()))
        client.newCall(builder.build()).execute().use { r ->
            return HttpResponse(r.code, r.body?.string().orEmpty())
        }
    }
}
