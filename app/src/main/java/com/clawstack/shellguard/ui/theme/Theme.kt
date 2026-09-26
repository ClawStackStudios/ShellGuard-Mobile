package com.clawstack.shellguard.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = ReefPink,
    onPrimary = TextPrimary,
    primaryContainer = SurfaceContainerDark,
    onPrimaryContainer = TextPrimary,
    secondary = ReefPinkDark,
    onSecondary = TextPrimary,
    background = OceanDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceContainerDark,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    outlineVariant = BorderMedium,
    error = StatusError,
    onError = TextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = ReefPink,
    onPrimary = Color.White,
    background = Color.White,
    onBackground = Color(0xFF111827),
    surface = Color(0xFFF9FAFB),
    onSurface = Color(0xFF111827),
    outline = Color(0xFFE5E7EB)
)

@Composable
fun ShellGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> DarkColorScheme // ShellGuard defaults to Dark Modernist palette
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
