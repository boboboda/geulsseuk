package com.buyoungsilcoding.geulsseuk.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ConsentScreen(onAccept: () -> Unit) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp)
    ) {
        Text("글쓱에 오신 걸 환영해요", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Text("사진과 메모로 블로그 글 초안을 만들어 드려요. 시작하기 전에 꼭 알아 두세요.")
        Spacer(Modifier.height(16.dp))
        Text("• 사진과 메모는 내가 고른 AI 서비스(Gemini, Claude 등)로 전송돼요. 이 앱은 별도 서버를 두지 않아요.")
        Spacer(Modifier.height(8.dp))
        Text("• 일부 AI의 무료 한도에서는 보낸 내용이 서비스 개선에 쓰일 수 있어요. 남에게 보이면 안 되는 사진(얼굴, 주소, 서류 등)은 올리지 마세요.")
        Spacer(Modifier.height(8.dp))
        Text("• API 키와 네이버 API 연결 암호는 이 기기 안에 암호화되어 저장되고, 해당 서비스로만 전송돼요.")
        Spacer(Modifier.height(8.dp))
        Text("• AI가 쓴 글은 사실과 다를 수 있어요. 올리기 전에 꼭 직접 읽고 고쳐 주세요.")
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAccept, modifier = Modifier.fillMaxWidth()) { Text("확인했어요") }
    }
}
