package ch.bfh.anchor.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue

/**
 * Overrides the language Compose resources are resolved in, for everything composed inside.
 * A null value means "follow the phone". Pattern from the Compose Multiplatform resources docs:
 * Android swaps the process locale and configuration, iOS sets the app's AppleLanguages.
 */
expect object LocalAppLocale {
    @Composable
    infix fun provides(value: String?): ProvidedValue<*>
}
