package com.buyoungsilcoding.geulsseuk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.buyoungsilcoding.geulsseuk.ui.AppRoot
import com.buyoungsilcoding.geulsseuk.ui.GeulsseukTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GeulsseukTheme { AppRoot(vm) }
        }
    }
}
