package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.audio.AudioStudioScreen
import com.example.ui.screens.hub.HubScreen
import com.example.ui.screens.photo.PhotoStudioScreen
import com.example.ui.screens.video.VideoStudioScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StudioBackground
import com.example.viewmodel.AppScreen
import com.example.viewmodel.StudioViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = StudioBackground
                ) {
                    MasterMakerApp()
                }
            }
        }
    }
}

@Composable
fun MasterMakerApp(
    viewModel: StudioViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    when (currentScreen) {
        AppScreen.HUB -> HubScreen(viewModel = viewModel)
        AppScreen.PHOTO_STUDIO -> PhotoStudioScreen(viewModel = viewModel)
        AppScreen.AUDIO_STUDIO -> AudioStudioScreen(viewModel = viewModel)
        AppScreen.VIDEO_STUDIO -> VideoStudioScreen(viewModel = viewModel)
    }
}
