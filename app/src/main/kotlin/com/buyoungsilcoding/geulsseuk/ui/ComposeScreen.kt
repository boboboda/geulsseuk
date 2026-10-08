package com.buyoungsilcoding.geulsseuk.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.buyoungsilcoding.geulsseuk.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeScreen(vm: MainViewModel) {
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MainViewModel.MAX_PHOTOS)
    ) { uris -> vm.addPhotos(uris) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("글쓱") },
                actions = { TextButton(onClick = vm::openSettings) { Text("설정") } },
            )
        },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("오늘의 사진", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(
                onClick = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("사진 고르기 (${vm.photos.size}/${MainViewModel.MAX_PHOTOS})") }

            if (vm.photos.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                    itemsIndexed(vm.photos) { i, p ->
                        Box {
                            AsyncImage(
                                model = p.uri,
                                contentDescription = "사진 ${i + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(8.dp)),
                            )
                            TextButton(
                                onClick = { vm.removePhoto(i) },
                                modifier = Modifier.align(Alignment.TopEnd),
                                contentPadding = PaddingValues(4.dp),
                            ) { Text("✕") }
                        }
                    }
                }
            }

            Text("간단한 메모", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = vm.memo,
                onValueChange = { vm.memo = it },
                placeholder = { Text("예) 오늘 점심에 새로 생긴 칼국수집 갔다. 국물이 시원했고 가격은 9천원.") },
                minLines = 5,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(4.dp))
            Button(onClick = vm::generate, modifier = Modifier.fillMaxWidth()) { Text("글 만들기") }
            if (vm.post != null) {
                TextButton(onClick = { vm.screen = com.buyoungsilcoding.geulsseuk.Screen.RESULT }, modifier = Modifier.fillMaxWidth()) {
                    Text("방금 만든 글 다시 보기")
                }
            }
        }
    }
}
