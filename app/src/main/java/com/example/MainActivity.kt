package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.DayWheelViewModel
import com.example.ui.screens.MainScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

enum class AppScreen {
    MAIN,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val viewModel: DayWheelViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf(AppScreen.MAIN) }

                    when (currentScreen) {
                        AppScreen.MAIN -> MainScreen(
                            viewModel = viewModel,
                            onOpenSettings = { currentScreen = AppScreen.SETTINGS }
                        )
                        AppScreen.SETTINGS -> SettingsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { currentScreen = AppScreen.MAIN }
                        )
                    }
                }
            }
        }
    }
}
