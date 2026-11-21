package ch.bfh.anchor.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleCountryCode
import platform.Foundation.currentLocale

/** The Region set in iOS Settings; carrier country APIs are deprecated and return placeholders. */
@Composable
actual fun rememberDeviceRegion(): String? = remember {
    (NSLocale.currentLocale.objectForKey(NSLocaleCountryCode) as? String)
        ?.takeIf { it.isNotBlank() }
        ?.uppercase()
}
