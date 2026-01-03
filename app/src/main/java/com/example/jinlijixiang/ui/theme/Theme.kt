package com.example.jinlijixiang.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Blue700,
    onPrimary = White,
    primaryContainer = Blue100,
    onPrimaryContainer = Blue800,
    secondary = Orange600,
    onSecondary = White,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFFFE0B2),
    onSecondaryContainer = Orange700,
    tertiary = Green500,
    onTertiary = White,
    background = Grey50,
    onBackground = Grey900,
    surface = White,
    onSurface = Grey900,
    surfaceVariant = Grey100,
    onSurfaceVariant = Grey700,
    error = Red500,
    onError = White
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue200,
    onPrimary = Blue800,
    primaryContainer = Blue700,
    onPrimaryContainer = Blue100,
    secondary = Orange500,
    onSecondary = Grey900,
    secondaryContainer = Orange700,
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFFFE0B2),
    tertiary = Green500,
    onTertiary = Grey900,
    background = Grey900,
    onBackground = Grey100,
    surface = Grey800,
    onSurface = Grey100,
    surfaceVariant = Grey700,
    onSurfaceVariant = Grey300,
    error = Red500,
    onError = Grey900
)

@Composable
fun JinlijixiangTheme(
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
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
