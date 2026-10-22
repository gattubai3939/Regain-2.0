package com.regain2.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Green = Color(0xFF7ED957)
val Orange = Color(0xFFFF8A00)
val Grey = Color(0xFF8A8A8A)
val Surface1 = Color(0xFF1A1A1A)
val Bg = Color(0xFF0E0E0E)

private val scheme = darkColorScheme(
    primary = Green, onPrimary = Color.Black,
    background = Bg, onBackground = Color.White,
    surface = Surface1, onSurface = Color.White,
    surfaceVariant = Surface1
)

@Composable
fun RegainTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
