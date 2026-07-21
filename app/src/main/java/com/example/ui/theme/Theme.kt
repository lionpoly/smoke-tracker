package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.ui.i18n.AppColorPreset
import com.example.ui.i18n.AppThemeMode

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    colorPreset: AppColorPreset = AppColorPreset.DEFAULT,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val primaryColor = when (colorPreset) {
        AppColorPreset.DEFAULT -> Color(0xFF6750A4)
        AppColorPreset.HEALTH_GREEN -> Color(0xFF1B5E20)
        AppColorPreset.DEEP_BLUE -> Color(0xFF0D47A1)
        AppColorPreset.WARM_AMBER -> Color(0xFFD84315)
    }

    val secondaryColor = when (colorPreset) {
        AppColorPreset.DEFAULT -> Color(0xFF625B71)
        AppColorPreset.HEALTH_GREEN -> Color(0xFF2E7D32)
        AppColorPreset.DEEP_BLUE -> Color(0xFF1976D2)
        AppColorPreset.WARM_AMBER -> Color(0xFFF57C00)
    }

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = primaryColor,
            secondary = secondaryColor,
            tertiary = Pink80,
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E2A),
            onBackground = Color(0xFFE6E1E5),
            onSurface = Color(0xFFE6E1E5),
            surfaceVariant = Color(0xFF2A2A38),
            onSurfaceVariant = Color(0xFFCAC4D0),
            primaryContainer = primaryColor.copy(alpha = 0.35f),
            onPrimaryContainer = Color(0xFFF3EDF7),
            secondaryContainer = secondaryColor.copy(alpha = 0.35f),
            onSecondaryContainer = Color(0xFFF3EDF7)
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            secondary = secondaryColor,
            tertiary = Pink40,
            background = HighDensityBackground,
            surface = HighDensitySurface,
            onBackground = HighDensityTextDark,
            onSurface = HighDensityTextDark,
            surfaceVariant = Color(0xFFF0F2F8),
            onSurfaceVariant = Color(0xFF49454F),
            primaryContainer = primaryColor.copy(alpha = 0.12f),
            onPrimaryContainer = primaryColor,
            secondaryContainer = secondaryColor.copy(alpha = 0.12f),
            onSecondaryContainer = secondaryColor
        )
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
