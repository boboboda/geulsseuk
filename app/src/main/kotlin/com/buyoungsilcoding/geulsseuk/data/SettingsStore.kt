package com.buyoungsilcoding.geulsseuk.data

import android.content.Context
import com.buyoungsilcoding.geulsseuk.core.ai.AiKind
import java.time.LocalDate

/** 설정 저장소. 비밀 값(API 키, 네이버 API 연결 암호)은 암호화해서 저장합니다. */
class SettingsStore(context: Context) {
    private val sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    companion object {
        const val DAILY_LIMIT = 30
    }

    var aiKind: AiKind
        get() = runCatching { AiKind.valueOf(sp.getString("ai_kind", null) ?: "") }
            .getOrDefault(AiKind.GEMINI)
        set(v) = sp.edit().putString("ai_kind", v.name).apply()

    fun apiKey(k: AiKind): String = KeystoreCipher.decrypt(sp.getString("key_${k.name}", "") ?: "")
    fun setApiKey(k: AiKind, v: String) =
        sp.edit().putString("key_${k.name}", KeystoreCipher.encrypt(v.trim())).apply()

    fun model(k: AiKind): String = sp.getString("model_${k.name}", null) ?: k.defaultModel
    fun setModel(k: AiKind, v: String) = sp.edit().putString("model_${k.name}", v.trim()).apply()

    fun baseUrl(k: AiKind): String = sp.getString("base_${k.name}", null) ?: k.defaultBaseUrl
    fun setBaseUrl(k: AiKind, v: String) = sp.edit().putString("base_${k.name}", v.trim()).apply()

    var naverId: String
        get() = sp.getString("naver_id", "") ?: ""
        set(v) = sp.edit().putString("naver_id", v.trim()).apply()

    var naverApiPassword: String
        get() = KeystoreCipher.decrypt(sp.getString("naver_pw", "") ?: "")
        set(v) = sp.edit().putString("naver_pw", KeystoreCipher.encrypt(v.trim())).apply()

    var topic: String
        get() = sp.getString("topic", "") ?: ""
        set(v) = sp.edit().putString("topic", v).apply()

    var styleSample: String
        get() = sp.getString("style", "") ?: ""
        set(v) = sp.edit().putString("style", v).apply()

    /** true면 공개 발행, false면 네이버에서 정한 비공개/임시 상태로 올림(테스트로 확인 필요). */
    var publishPublic: Boolean
        get() = sp.getBoolean("publish_public", false)
        set(v) = sp.edit().putBoolean("publish_public", v).apply()

    var consentAccepted: Boolean
        get() = sp.getBoolean("consent", false)
        set(v) = sp.edit().putBoolean("consent", v).apply()

    /** 오늘 AI 호출 1회를 쓰고, 한도를 넘었으면 false. (비용·무료 한도 보호용) */
    @Synchronized
    fun tryConsumeCall(): Boolean {
        val today = LocalDate.now().toString()
        val used = if (sp.getString("calls_day", "") == today) sp.getInt("calls_n", 0) else 0
        if (used >= DAILY_LIMIT) return false
        sp.edit().putString("calls_day", today).putInt("calls_n", used + 1).apply()
        return true
    }
}
