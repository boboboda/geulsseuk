package com.buyoungsilcoding.geulsseuk

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.buyoungsilcoding.geulsseuk.core.AiException
import com.buyoungsilcoding.geulsseuk.core.Part
import com.buyoungsilcoding.geulsseuk.core.Post
import com.buyoungsilcoding.geulsseuk.core.PostBlock
import com.buyoungsilcoding.geulsseuk.core.ai.AiKind
import com.buyoungsilcoding.geulsseuk.core.ai.AiProviders
import com.buyoungsilcoding.geulsseuk.core.ai.PostGenerator
import com.buyoungsilcoding.geulsseuk.core.naver.HtmlBuilder
import com.buyoungsilcoding.geulsseuk.core.naver.NaverBlogClient
import com.buyoungsilcoding.geulsseuk.core.naver.NaverException
import com.buyoungsilcoding.geulsseuk.data.ImageProcessor
import com.buyoungsilcoding.geulsseuk.data.OkHttpTransport
import com.buyoungsilcoding.geulsseuk.data.SettingsStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen { COMPOSE, RESULT, SETTINGS }

/** 화면에 보이는 사진 한 장: 미리보기용 uri + 줄인 이미지. */
class PhotoItem(val uri: Uri, val image: Part.Image)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    val settings = SettingsStore(app)
    private val transport = OkHttpTransport()

    var screen by mutableStateOf(Screen.COMPOSE)
    var consentAccepted by mutableStateOf(settings.consentAccepted)
        private set

    var photos by mutableStateOf<List<PhotoItem>>(emptyList())
        private set
    var memo by mutableStateOf("")

    /** 진행 중 작업 설명. null이면 작업 없음. */
    var busy by mutableStateOf<String?>(null)
        private set
    var notice by mutableStateOf<String?>(null)
        private set

    var post by mutableStateOf<Post?>(null)
        private set
    /** 업로드 성공 후 글 번호(비어 있을 수 있음). null이면 아직 안 올림. */
    var uploadedPostId by mutableStateOf<String?>(null)
        private set

    fun acceptConsent() {
        settings.consentAccepted = true
        consentAccepted = true
    }

    fun dismissNotice() { notice = null }
    fun dismissUploaded() { uploadedPostId = null }

    // ---- 사진 ----
    fun addPhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            busy = "사진을 줄이는 중…"
            try {
                val added = withContext(Dispatchers.IO) {
                    uris.mapNotNull { u ->
                        runCatching { PhotoItem(u, ImageProcessor.process(getApplication(), u)) }.getOrNull()
                    }
                }
                val room = MAX_PHOTOS - photos.size
                photos = photos + added.take(room)
                if (added.size < uris.size) notice = "읽지 못한 사진은 빼고 담았어요."
                if (added.size > room) notice = "사진은 한 번에 최대 ${MAX_PHOTOS}장까지예요."
            } finally {
                busy = null
            }
        }
    }

    fun removePhoto(index: Int) {
        photos = photos.filterIndexed { i, _ -> i != index }
    }

    // ---- 글 만들기 ----
    fun generate() {
        if (photos.isEmpty() && memo.isBlank()) {
            notice = "사진이나 메모를 하나 이상 넣어 주세요."; return
        }
        val kind = settings.aiKind
        val key = settings.apiKey(kind)
        if (key.isBlank()) { notice = "설정에서 ${kind.label} API 키를 먼저 넣어 주세요."; screen = Screen.SETTINGS; return }
        if (!settings.tryConsumeCall()) { notice = "오늘 쓸 수 있는 횟수(${SettingsStore.DAILY_LIMIT}회)를 다 썼어요."; return }

        viewModelScope.launch {
            busy = "AI가 글을 쓰는 중… (최대 1~2분)"
            try {
                val provider = AiProviders.create(kind, key, settings.model(kind), settings.baseUrl(kind), transport)
                val result = withContext(Dispatchers.IO) {
                    PostGenerator(provider).generate(settings.topic, settings.styleSample, memo, photos.map { it.image })
                }
                post = result
                uploadedPostId = null
                screen = Screen.RESULT
            } catch (e: CancellationException) {
                throw e
            } catch (e: AiException) {
                notice = e.message
            } catch (e: Exception) {
                notice = "글을 만들지 못했어요: ${e.message ?: e::class.simpleName}"
            } finally {
                busy = null
            }
        }
    }

    // ---- 글 편집 ----
    fun setTitle(t: String) { post = post?.copy(title = t) }

    fun setParagraph(index: Int, text: String) = editBlocks { l ->
        if (l.getOrNull(index) is PostBlock.Paragraph) l[index] = PostBlock.Paragraph(text)
    }

    fun setCaption(index: Int, caption: String) = editBlocks { l ->
        (l.getOrNull(index) as? PostBlock.Photo)?.let { l[index] = it.copy(caption = caption) }
    }

    fun moveBlock(index: Int, delta: Int) = editBlocks { l ->
        val to = index + delta
        if (index in l.indices && to in l.indices) {
            val b = l.removeAt(index); l.add(to, b)
        }
    }

    fun deleteBlock(index: Int) = editBlocks { l -> if (index in l.indices) l.removeAt(index) }

    private inline fun editBlocks(change: (MutableList<PostBlock>) -> Unit) {
        val p = post ?: return
        val l = p.blocks.toMutableList()
        change(l)
        post = p.copy(blocks = l)
    }

    fun photoImage(number: Int): PhotoItem? = photos.getOrNull(number - 1)

    // ---- 네이버 업로드 ----
    fun upload() {
        val p = post ?: return
        val id = settings.naverId
        val pw = settings.naverApiPassword
        if (id.isBlank() || pw.isBlank()) {
            notice = "설정에서 네이버 ID와 API 연결 암호를 먼저 넣어 주세요."; screen = Screen.SETTINGS; return
        }
        val used = p.blocks.filterIsInstance<PostBlock.Photo>().map { it.number }.distinct()
        viewModelScope.launch {
            busy = "네이버에 올리는 중…"
            try {
                val client = NaverBlogClient(id, pw, transport)
                val newId = withContext(Dispatchers.IO) {
                    val urls = HashMap<Int, String>()
                    for (n in used) {
                        val img = photos.getOrNull(n - 1)?.image ?: continue
                        withContext(Dispatchers.Main) { busy = "사진 올리는 중… ($n/${photos.size})" }
                        urls[n] = client.uploadImage("photo_$n.jpg", img.mime, img.bytes)
                    }
                    client.newPost(p.title, HtmlBuilder.build(p, urls), settings.publishPublic)
                }
                uploadedPostId = newId
            } catch (e: CancellationException) {
                throw e
            } catch (e: NaverException) {
                notice = e.message
            } catch (e: Exception) {
                notice = "올리지 못했어요: ${e.message ?: e::class.simpleName}"
            } finally {
                busy = null
            }
        }
    }

    /** 새 글 쓰기: 사진·메모·결과를 비웁니다. */
    fun reset() {
        photos = emptyList(); memo = ""; post = null; uploadedPostId = null; screen = Screen.COMPOSE
    }

    // ---- 설정 화면용: API 키 시험 ----
    fun testKey() {
        val kind = settings.aiKind
        val key = settings.apiKey(kind)
        if (key.isBlank()) { notice = "먼저 API 키를 넣어 주세요."; return }
        viewModelScope.launch {
            busy = "연결을 확인하는 중…"
            try {
                val provider = AiProviders.create(kind, key, settings.model(kind), settings.baseUrl(kind), transport)
                withContext(Dispatchers.IO) {
                    provider.complete("한 단어로만 답하세요.", listOf(Part.Text("안녕이라고 답하세요.")))
                }
                notice = "연결이 잘 돼요!"
            } catch (e: CancellationException) {
                throw e
            } catch (e: AiException) {
                notice = e.message
            } catch (e: Exception) {
                notice = "연결하지 못했어요: ${e.message ?: e::class.simpleName}"
            } finally {
                busy = null
            }
        }
    }

    private var returnTo = Screen.COMPOSE

    fun openSettings() { returnTo = screen.takeIf { it != Screen.SETTINGS } ?: Screen.COMPOSE; screen = Screen.SETTINGS }
    fun back() {
        screen = when (screen) {
            Screen.SETTINGS -> if (returnTo == Screen.RESULT && post == null) Screen.COMPOSE else returnTo
            Screen.RESULT -> Screen.COMPOSE
            Screen.COMPOSE -> Screen.COMPOSE
        }
    }

    companion object {
        const val MAX_PHOTOS = 10
    }
}
