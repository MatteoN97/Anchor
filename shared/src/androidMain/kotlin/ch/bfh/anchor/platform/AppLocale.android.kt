package ch.bfh.anchor.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

actual object LocalAppLocale {
    /** The phone's own locale, captured before Anchor first replaces it. */
    private var systemDefault: Locale? = null

    @Suppress("DEPRECATION") // updateConfiguration is what keeps Android's own resources in step
    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val configuration = LocalConfiguration.current
        val system = systemDefault ?: Locale.getDefault().also { systemDefault = it }
        val locale = if (value == null) system else Locale.forLanguageTag(value)
        Locale.setDefault(locale)
        configuration.setLocale(locale)
        val resources = LocalContext.current.resources
        resources.updateConfiguration(configuration, resources.displayMetrics)
        return LocalConfiguration.provides(configuration)
    }
}
