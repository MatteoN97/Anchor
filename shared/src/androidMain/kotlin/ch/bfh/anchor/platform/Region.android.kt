package ch.bfh.anchor.platform

import android.telephony.TelephonyManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Prefers the SIM's country, which follows where the user actually lives, over the locale,
 * which only reflects their language settings. Neither call needs a permission.
 */
@Composable
actual fun rememberDeviceRegion(): String? {
    val context = LocalContext.current
    return remember(context) {
        val telephony = context.getSystemService(TelephonyManager::class.java)
        listOf(telephony?.simCountryIso, telephony?.networkCountryIso, Locale.getDefault().country)
            .firstOrNull { !it.isNullOrBlank() }
            ?.uppercase()
    }
}
