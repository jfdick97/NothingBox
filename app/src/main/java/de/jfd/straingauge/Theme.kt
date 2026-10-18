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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily

val Garamond = FontFamily(
    Font(R.font.eb_garamond)
)

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
    primary = Color(0xFFA7C8E2),
    onPrimary = Color(0xFF142D40),
    primaryContainer = Color(0xFF405B70),
    onPrimaryContainer = Color(0xFFDFEEFA),

    secondary = Color(0xFFB4C7D6),
    onSecondary = Color(0xFF203442),
    secondaryContainer = Color(0xFF293E50),
    onSecondaryContainer = Color(0xFFDFEAF3),

    tertiary = Color(0xFFD3BEDD),
    onTertiary = Color(0xFF392744),
    tertiaryContainer = Color(0xFF51405C),
    onTertiaryContainer = Color(0xFFF0DCF9),

    background = Color(0xFF121A21),
    onBackground = Color(0xFFE4EAF0),

    surface = Color(0xFF121A21),
    onSurface = Color(0xFFE4EAF0),
    surfaceVariant = Color(0xFF35434F),
    onSurfaceVariant = Color(0xFFBCC9D4),

    surfaceContainerLowest = Color(0xFF0D141A),
    surfaceContainerLow = Color(0xFF19252F),
    surfaceContainer = Color(0xFF202E39),
    surfaceContainerHigh = Color(0xFF293845),
    surfaceContainerHighest = Color(0xFF334451),

    outline = Color(0xFF899CAB),
    outlineVariant = Color(0xFF405260),

    inverseSurface = Color(0xFFE4EAF0),
    inverseOnSurface = Color(0xFF27323B),
    inversePrimary = Color(0xFF405B70),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    surfaceTint = Color(0xFFA7C8E2)
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