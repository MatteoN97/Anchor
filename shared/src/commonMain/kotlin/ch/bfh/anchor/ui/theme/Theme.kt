package ch.bfh.anchor.ui.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/** Light, dark, or whatever the phone is set to. Stored by name in SettingsManager. */
enum class ThemeMode { System, Light, Dark }

/** Material You on Android 12+, null everywhere else. */
@Composable
expect fun dynamicColorSchemeOrNull(darkTheme: Boolean): ColorScheme?

/** Whether [dynamicColorSchemeOrNull] can return a scheme on this device (Android 12+). */
expect val dynamicColorAvailable: Boolean

/**
 * Keeps the system chrome in step with a theme chosen in the app rather than by the phone:
 * status-bar icon colour on Android, the window's interface style on iOS (which also gives
 * system sheets, menus and the keyboard the matching appearance).
 */
@Composable
expect fun SystemAppearanceEffect(themeMode: ThemeMode, darkTheme: Boolean)

/**
 * Material 3 with Anchor's calm palette on Android; on iOS, the same MaterialTheme slots filled
 * with iOS-structured colours, San Francisco text styles and iOS shapes (see IosTheme.kt), with
 * ripples turned off. [dynamicColor] (Material You) applies on Android 12+ only, and only when
 * the user asked for it: the default is Anchor's own palette.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnchorTheme(
    themeMode: ThemeMode = ThemeMode.System,
    dynamicColor: Boolean = false,
    isIos: Boolean = LocalIsIos.current,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    SystemAppearanceEffect(themeMode, darkTheme)

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
        ?: if (darkTheme) AnchorDarkColorScheme else AnchorLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AnchorShapes
    ) {
        CompositionLocalProvider(LocalIsIos provides false, content = content)
    }
}
