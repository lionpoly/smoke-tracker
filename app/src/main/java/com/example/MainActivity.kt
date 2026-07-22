package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.SmokingApp
import com.example.ui.SmokingViewModel
import com.example.ui.SmokingViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private val viewModel: SmokingViewModel by viewModels {
    SmokingViewModelFactory(this)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val themeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
      val colorPreset by viewModel.appColorPreset.collectAsStateWithLifecycle()
      val fontFamilyState by viewModel.appFontFamily.collectAsStateWithLifecycle()

      MyApplicationTheme(
        themeMode = themeMode,
        colorPreset = colorPreset,
        fontFamily = fontFamilyState.fontFamily
      ) {
        SmokingApp(viewModel)
      }
    }
  }
}

