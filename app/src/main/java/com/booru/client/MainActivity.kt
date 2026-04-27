package com.booru.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.booru.client.ui.screens.HomeScreen
import com.booru.client.ui.theme.BooruTheme
import com.booru.client.viewmodel.BooruViewModel

/**
 * Main entry point for the Booru Client.
 * Sets up edge-to-edge display, Compose content, and the ViewModel.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: BooruViewModel by lazy {
        BooruViewModel(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BooruTheme {
                HomeScreen(
                    viewModel = viewModel,
                    modifier = Modifier
                )
            }
        }
    }
}
