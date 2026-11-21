package ch.bfh.anchor.ui.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

/** Material You on Android 12+, null everywhere else. */
@Composable
expect fun dynamicColorSchemeOrNull(darkTheme: Boolean): ColorScheme?

/**
 * Material 3 on Android; on iOS, the same MaterialTheme slots filled with iOS system colours,
 * San Francisco text styles and iOS shapes (see IosTheme.kt), with ripples turned off.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnchorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    isIos: Boolean = LocalIsIos.current,
    content: @Composable () -> Unit
) {
    if (isIos) {
        MaterialTheme(
            colorScheme = if (darkTheme) IosDarkColorScheme else IosLightColorScheme,
            typography = IosTypography,
            shapes = IosShapes
        ) {
            CompositionLocalProvider(
                LocalIsIos provides true,
                // Material components draw nothing on press without a ripple configuration; the
                // adaptive wrappers in ui/adaptive add the iOS dim-on-press themselves.
                LocalRippleConfiguration provides null,
                LocalIndication provides IosHighlightIndication(
                    if (darkTheme) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)
                ),
                content = content
            )
        }
        return
    }

    val dynamicScheme = if (dynamicColor) dynamicColorSchemeOrNull(darkTheme) else null
    val colorScheme = dynamicScheme
        ?: if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        CompositionLocalProvider(LocalIsIos provides false, content = content)
    }
}
