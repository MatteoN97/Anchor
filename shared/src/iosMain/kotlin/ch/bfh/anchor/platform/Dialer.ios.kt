package ch.bfh.anchor.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@Composable
actual fun rememberPhoneDialer(): (number: String) -> Unit = remember {
    { number ->
        NSURL.URLWithString("tel:${dialable(number)}")?.let { url ->
            // The one-argument openURL(_:) is deprecated and no longer opens anything on recent
            // iOS: tapping "Call" did nothing. iOS still asks before the call is placed.
            UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
        }
    }
}
