package ch.bfh.anchor.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages

actual object LocalAppLocale {
    private const val LANGUAGES_KEY = "AppleLanguages"

    /**
     * The phone's language. The override from a previous launch is cleared first, otherwise
     * preferredLanguages would report Anchor's own choice back as the system's.
     */
    private val systemDefault: String = run {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(LANGUAGES_KEY)
        NSLocale.preferredLanguages.firstOrNull() as? String ?: "en"
    }

    private val LocalLanguage = staticCompositionLocalOf { systemDefault }

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val defaults = NSUserDefaults.standardUserDefaults
        if (value == null) defaults.removeObjectForKey(LANGUAGES_KEY)
        else defaults.setObject(listOf(value), LANGUAGES_KEY)
        return LocalLanguage provides (value ?: systemDefault)
    }
}
