package ch.bfh.anchor.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import platform.UIKit.UIApplication

@Composable
actual fun KeepScreenOn() {
    DisposableEffect(Unit) {
        val app = UIApplication.sharedApplication
        val previous = app.idleTimerDisabled
        app.idleTimerDisabled = true
        onDispose { app.idleTimerDisabled = previous }
    }
}
