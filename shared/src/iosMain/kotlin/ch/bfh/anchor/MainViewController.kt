package ch.bfh.anchor

import androidx.compose.ui.window.ComposeUIViewController
import ch.bfh.anchor.data.createDatabase
import ch.bfh.anchor.settings.createSettingsManager

private val database by lazy { createDatabase() }
private val settingsManager by lazy { createSettingsManager() }

@Suppress("unused", "FunctionName") // entry point called from Swift
fun MainViewController() = ComposeUIViewController {
    App(database = database, settingsManager = settingsManager)
}
