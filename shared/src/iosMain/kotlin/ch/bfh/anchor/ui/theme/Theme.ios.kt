package ch.bfh.anchor.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import platform.UIKit.UIApplication
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIWindow

/** iOS has no system-wide accent colour to derive a scheme from. */
@Composable
actual fun dynamicColorSchemeOrNull(darkTheme: Boolean): ColorScheme? = null

actual val dynamicColorAvailable: Boolean = false

/**
 * Overrides the window's interface style, so the status bar, keyboard and system menus match a
 * theme chosen in the app. "System" hands the decision back to iOS.
 */
@Composable
actual fun SystemAppearanceEffect(themeMode: ThemeMode, darkTheme: Boolean) {
    LaunchedEffect(themeMode) {
        val style = when (themeMode) {
            ThemeMode.System -> UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
            ThemeMode.Light -> UIUserInterfaceStyle.UIUserInterfaceStyleLight
            ThemeMode.Dark -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
        }
        @Suppress("DEPRECATION")
        UIApplication.sharedApplication.windows.forEach { window ->
            (window as? UIWindow)?.overrideUserInterfaceStyle = style
        }
    }
}
