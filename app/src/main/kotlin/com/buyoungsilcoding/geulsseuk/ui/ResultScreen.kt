package com.buyoungsilcoding.geulsseuk.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.buyoungsilcoding.geulsseuk.MainViewModel
import com.buyoungsilcoding.geulsseuk.core.PostBlock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(vm: MainViewModel) {
    val post = vm.post ?: return
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var confirm by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("글 확인·수정") },
                navigationIcon = { TextButton(onClick = vm::back) { Text("‹ 뒤로") } },
                actions = { TextButton(onClick = vm::openSettings) { Text("설정") } },
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = post.title,
                    onValueChange = vm::setTitle,
                    label = { Text("제목") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            itemsIndexed(post.blocks) { i, b ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    when (b) {
                        is PostBlock.Paragraph -> OutlinedTextField(
                            value = b.text,
                            onValueChange = { vm.setParagraph(i, it) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        is PostBlock.Photo -> {
                            val item = vm.photoImage(b.number)
                            if (item != null) {
                                AsyncImage(
                                    model = item.uri,
                                    contentDescription = "사진 ${b.number}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                )
                            } else {
                                Text("[사진 ${b.number}] (사진을 찾을 수 없어요)")
                            }
                            OutlinedTextField(
                                value = b.caption,
                                onValueChange = { vm.setCaption(i, it) },
                                label = { Text("사진 설명 (비워도 돼요)") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    Row {
                        TextButton(onClick = { vm.moveBlock(i, -1) }, enabled = i > 0) { Text("▲") }
                        TextButton(onClick = { vm.moveBlock(i, 1) }, enabled = i < post.blocks.lastIndex) { Text("▼") }
                        TextButton(onClick = { vm.deleteBlock(i) }) { Text("삭제") }
                    }
                }
            }
            item {
                Text(
                    post.tags.joinToString(" ") { "#$it" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth()) { Text("네이버 블로그에 올리기") }
                    OutlinedButton(
                        onClick = { clipboard.setText(AnnotatedString(post.toPlainText())); copied = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("전체 복사 (네이버 앱에 직접 붙여넣기)") }
                    TextButton(onClick = vm::reset, modifier = Modifier.fillMaxWidth()) { Text("새 글 쓰기") }
                }
            }
        }
    }

    if (confirm) {
        val pub = vm.settings.publishPublic
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("네이버에 올릴까요?") },
            text = {
                Text(
                    if (pub) "사진과 글이 올라가고 바로 공개돼요. 올린 뒤에는 이 앱에서 수정할 수 없어요."
                    else "사진과 글을 올려요. 공개하지 않는 설정이라 네이버 블로그에서 어떻게 보이는지 확인해 주세요. 올린 뒤에는 이 앱에서 수정할 수 없어요."
                )
            },
            confirmButton = { TextButton(onClick = { confirm = false; vm.upload() }) { Text("올리기") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("취소") } },
        )
    }
    if (copied) {
        AlertDialog(
            onDismissRequest = { copied = false },
            text = { Text("복사했어요. 사진 위치는 [사진 N]으로 표시돼 있어요.") },
            confirmButton = { TextButton(onClick = { copied = false }) { Text("확인") } },
        )
    }
    vm.uploadedPostId?.let {
        AlertDialog(
            onDismissRequest = vm::dismissUploaded,
            title = { Text("올렸어요") },
            text = { Text("네이버 블로그에서 글을 확인하고, 필요하면 거기서 고쳐 주세요.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.dismissUploaded()
                    val id = vm.settings.naverId
                    if (id.isNotBlank()) {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://blog.naver.com/" + Uri.encode(id)))
                        )
                    }
                }) { Text("내 블로그 열기") }
            },
            dismissButton = { TextButton(onClick = vm::dismissUploaded) { Text("닫기") } },
        )
    }
}
