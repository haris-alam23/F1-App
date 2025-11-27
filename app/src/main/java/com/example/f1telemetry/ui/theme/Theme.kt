package com.example.f1telemetry.ui.theme

import android.R
import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// In ui/theme/Color.kt
val F1Background = Color(0xFF1d1d1d)
val F1Red = Color(0xFF8d0000)
val F1Grey = Color(0xFF656464)
val F1LightGrey = Color(0xFFd4d4d4)
val F1White = Color(0xFFFFFFFF)


private val DarkColorScheme = darkColorScheme(
    primary = F1Red,
    onPrimary = F1White,
    background = F1Background,
    onBackground = F1LightGrey,
    surface = F1Grey,
    onSurface = F1White,
    error = F1Red.copy(alpha = 0.8f)
)

private val LightColorScheme = lightColorScheme(
    primary = F1Red,
    onPrimary = F1White,
    secondary = F1Red,
    onSecondary = Color.Black,
    background = Color(0xFFF5F5F5),
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun F1TelemetryTheme(
    darkTheme: Boolean = true,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}