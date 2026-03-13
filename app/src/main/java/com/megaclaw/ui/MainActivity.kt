package com.megaclaw.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.megaclaw.ui.theme.MegaclawTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
        } catch (_: Exception) {
            // Some x86 Android systems don't fully support edge-to-edge
        }
        setContent {
            MegaclawTheme {
                ChatScreen()
            }
        }
    }
}
