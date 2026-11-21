package ch.bfh.anchor.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@Composable
actual fun rememberPhoneDialer(): (number: String) -> Unit = remember {
    { number ->
        NSURL.URLWithString("tel:$number")?.let { url ->
            UIApplication.sharedApplication.openURL(url)
        }
    }
}
