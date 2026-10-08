package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.motoai.ui.AppScreen
import com.example.motoai.ui.MainViewModel
import com.example.motoai.ui.components.ActionConfirmationDialog
import com.example.motoai.ui.screens.HomeScreen
import com.example.motoai.ui.screens.ImageGenerationScreen
import com.example.motoai.ui.screens.SettingsScreen
import com.example.motoai.ui.screens.VideoGenerationScreen
import com.example.ui.theme.MotoBackground
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()
                val confirmationState by viewModel.confirmationState.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MotoBackground
                ) { innerPadding ->
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "ScreenTransition",
                        modifier = Modifier.padding(innerPadding)
                    ) { screen ->
                        when (screen) {
                            AppScreen.HOME -> HomeScreen(viewModel = viewModel)
                            AppScreen.IMAGE_GEN -> ImageGenerationScreen(viewModel = viewModel)
                            AppScreen.VIDEO_GEN -> VideoGenerationScreen(viewModel = viewModel)
                            AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                        }
                    }

                    // Security & Action confirmation dialog (Calls, sensitive actions)
                    ActionConfirmationDialog(state = confirmationState)
                }
            }
        }
    }
}
