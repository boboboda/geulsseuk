package com.buyoungsilcoding.geulsseuk.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.buyoungsilcoding.geulsseuk.MainViewModel
import com.buyoungsilcoding.geulsseuk.Screen

@Composable
fun AppRoot(vm: MainViewModel) {
    Surface(color = MaterialTheme.colorScheme.background) {
        if (!vm.consentAccepted) {
            ConsentScreen(onAccept = vm::acceptConsent)
        } else {
            BackHandler(enabled = vm.screen != Screen.COMPOSE) { vm.back() }
            when (vm.screen) {
                Screen.COMPOSE -> ComposeScreen(vm)
                Screen.RESULT -> ResultScreen(vm)
                Screen.SETTINGS -> SettingsScreen(vm)
            }
        }

        vm.busy?.let { BusyDialog(it) }
        vm.notice?.let { msg ->
            AlertDialog(
                onDismissRequest = vm::dismissNotice,
                text = { Text(msg) },
                confirmButton = { TextButton(onClick = vm::dismissNotice) { Text("확인") } },
            )
        }
    }
}

@Composable
private fun BusyDialog(text: String) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        text = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
                CircularProgressIndicator(modifier = androidx.compose.ui.Modifier.width(28.dp))
                Spacer(androidx.compose.ui.Modifier.width(16.dp))
                Text(text)
            }
        },
    )
}
