package de.jfd.straingauge

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF3D7772),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2E9E2),
    onPrimaryContainer = Color(0xFF123B36),

    secondary = Color(0xFF54746D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8EFEB),
    onSecondaryContainer = Color(0xFF263F4A),

    background = Color(0xFFFAF8F3),
    onBackground = Color(0xFF263F4A),
    surface = Color(0xFFFAF8F3),
    onSurface = Color(0xFF263F4A),
    onSurfaceVariant = Color(0xFF52615F),

    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF2F0EA),
    surfaceContainerHigh = Color(0xFFECEAE4),
    surfaceContainerHighest = Color(0xFFE5E3DD),

    outline = Color(0xFF74817D),
    outlineVariant = Color(0xFFCCD5CE),

    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),

    surfaceTint = Color(0xFF3D7772)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9DD2C4),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF245047),
    onPrimaryContainer = Color(0xFFBBECDD),

    secondary = Color(0xFFB6CCC3),
    onSecondary = Color(0xFF243830),
    secondaryContainer = Color(0xFF263B35),
    onSecondaryContainer = Color(0xFFDEEAE3),

    background = Color(0xFF141A18),
    onBackground = Color(0xFFE4EAE5),
    surface = Color(0xFF141A18),
    onSurface = Color(0xFFE4EAE5),
    onSurfaceVariant = Color(0xFFBBC7C0),

    surfaceContainerLowest = Color(0xFF0E1412),
    surfaceContainerLow = Color(0xFF1C2420),
    surfaceContainer = Color(0xFF222B26),
    surfaceContainerHigh = Color(0xFF2C352F),
    surfaceContainerHighest = Color(0xFF364039),

    outline = Color(0xFF8B9990),
    outlineVariant = Color(0xFF435048),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    surfaceTint = Color(0xFF9DD2C4)
)

@Composable
fun StrainGaugeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = Typography()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colors.background,
            contentColor = colors.onBackground
        ) {
            content()
        }
    }
}

@Composable
fun metricColorsForTheme(): List<Color> {
    return if (isSystemInDarkTheme()) {
        listOf(
            Color(0xFFE3A675),
            Color(0xFFDCA6B7),
            Color(0xFF8CC9B7),
            Color(0xFF99BEDD)
        )
    } else {
        listOf(
            Color(0xFFB8794D),
            Color(0xFF9C6574),
            Color(0xFF427F76),
            Color(0xFF547C9D)
        )
    }
}