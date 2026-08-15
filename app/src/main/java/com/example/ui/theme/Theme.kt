package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = MinimalPrimaryContainer,
    onPrimary = MinimalOnPrimaryContainer,
    primaryContainer = MinimalDarkSurfaceVariant,
    onPrimaryContainer = MinimalPrimaryContainer,
    secondary = MinimalSecondaryContainer,
    onSecondary = MinimalOnSecondaryContainer,
    secondaryContainer = MinimalDarkSurfaceVariant,
    onSecondaryContainer = MinimalSecondaryContainer,
    tertiary = MinimalPrimary,
    background = MinimalDarkBackground,
    onBackground = MinimalDarkTextPrimary,
    surface = MinimalDarkSurface,
    onSurface = MinimalDarkTextPrimary,
    surfaceVariant = MinimalDarkSurfaceVariant,
    onSurfaceVariant = MinimalDarkTextSecondary,
    outline = MinimalDarkBorder,
    outlineVariant = MinimalDarkSurfaceSubtle
)

private val LightColorScheme = lightColorScheme(
    primary = MinimalPrimary,
    onPrimary = MinimalOnPrimary,
    primaryContainer = MinimalPrimaryContainer,
    onPrimaryContainer = MinimalOnPrimaryContainer,
    secondary = MinimalSecondary,
    onSecondary = MinimalOnPrimary,
    secondaryContainer = MinimalSecondaryContainer,
    onSecondaryContainer = MinimalOnSecondaryContainer,
    tertiary = MinimalPrimary,
    background = MinimalLightBackground,
    onBackground = MinimalLightTextPrimary,
    surface = MinimalLightSurface,
    onSurface = MinimalLightTextPrimary,
    surfaceVariant = MinimalLightSurfaceVariant,
    onSurfaceVariant = MinimalLightTextSecondary,
    outline = MinimalLightBorder,
    outlineVariant = MinimalLightSurfaceSubtle
)

@Composable
fun NextAiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our signature theme by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    NextAiTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}

