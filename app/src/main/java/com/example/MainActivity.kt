package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.localization.ProvideAppLocale
import com.example.navigation.TayyibAppRoot
import com.example.ui.theme.TayyibTheme
import com.example.ui.viewmodel.TayyibViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as TayyibApplication).container

        setContent {
            val viewModel: TayyibViewModel = viewModel(
                factory = TayyibViewModel.Factory(appContainer)
            )
            val settings by viewModel.userSettings.collectAsStateWithLifecycle()

            ProvideAppLocale(language = settings.language) {
                TayyibTheme(themeMode = settings.themeMode) {
                    TayyibAppRoot(viewModel = viewModel)
                }
            }
        }
    }
}
