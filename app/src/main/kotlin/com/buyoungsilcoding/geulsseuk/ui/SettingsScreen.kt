package com.buyoungsilcoding.geulsseuk.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.buyoungsilcoding.geulsseuk.MainViewModel
import com.buyoungsilcoding.geulsseuk.core.ai.AiKind

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel) {
    val s = vm.settings
    val context = LocalContext.current
    var kind by remember { mutableStateOf(s.aiKind) }

    fun open(url: String) {
        if (url.isNotBlank()) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정") },
                navigationIcon = { TextButton(onClick = vm::back) { Text("‹ 뒤로") } },
            )
        },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("AI 선택", style = MaterialTheme.typography.titleMedium)
            AiKind.entries.forEach { k ->
                Row(
                    Modifier.fillMaxWidth().clickable { kind = k; s.aiKind = k },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = kind == k, onClick = { kind = k; s.aiKind = k })
                    Text(k.label)
                }
            }
            Text(kind.hint, style = MaterialTheme.typography.bodySmall)

            // 제공사를 바꾸면 입력칸이 새 값으로 다시 만들어지도록 key 사용
            androidx.compose.runtime.key(kind) {
                var apiKey by remember { mutableStateOf(s.apiKey(kind)) }
                var model by remember { mutableStateOf(s.model(kind)) }
                var baseUrl by remember { mutableStateOf(s.baseUrl(kind)) }

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it; s.setApiKey(kind, it) },
                    label = { Text("API 키") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (kind.keyUrl.isNotBlank()) {
                    TextButton(onClick = { open(kind.keyUrl) }) { Text("API 키 발급받으러 가기") }
                }
                if (kind.usesBaseUrl) {
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it; s.setBaseUrl(kind, it) },
                        label = { Text("서버 주소") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it; s.setModel(kind, it) },
                    label = { Text("모델 이름") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(onClick = vm::testKey, modifier = Modifier.fillMaxWidth()) { Text("연결 확인") }
            }

            HorizontalDivider()
            Text("네이버 블로그", style = MaterialTheme.typography.titleMedium)
            var naverId by remember { mutableStateOf(s.naverId) }
            var naverPw by remember { mutableStateOf(s.naverApiPassword) }
            OutlinedTextField(
                value = naverId,
                onValueChange = { naverId = it; s.naverId = it },
                label = { Text("네이버 ID (블로그 주소의 아이디)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = naverPw,
                onValueChange = { naverPw = it; s.naverApiPassword = it },
                label = { Text("API 연결 암호 (로그인 비밀번호 아님)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = {
                val id = naverId.trim()
                open(if (id.isBlank()) "https://admin.blog.naver.com" else "https://admin.blog.naver.com/" + Uri.encode(id) + "/config/api")
            }) { Text("API 연결 암호 만들러 가기") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("올리자마자 공개")
                    Text("끄면 공개하지 않고 올려요.", style = MaterialTheme.typography.bodySmall)
                }
                var pub by remember { mutableStateOf(s.publishPublic) }
                Switch(checked = pub, onCheckedChange = { pub = it; s.publishPublic = it })
            }

            HorizontalDivider()
            Text("글 스타일", style = MaterialTheme.typography.titleMedium)
            var topic by remember { mutableStateOf(s.topic) }
            var style by remember { mutableStateOf(s.styleSample) }
            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it; s.topic = it },
                label = { Text("블로그 주제 (예: 동네 맛집, 육아)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = style,
                onValueChange = { style = it; s.styleSample = it },
                label = { Text("내 글투 예시 (평소 쓴 글 몇 문장)") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
