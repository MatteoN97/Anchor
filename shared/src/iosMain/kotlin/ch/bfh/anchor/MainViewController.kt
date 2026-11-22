package ch.bfh.anchor

import androidx.compose.ui.window.ComposeUIViewController
import ch.bfh.anchor.data.createDatabase
import ch.bfh.anchor.notifications.IosMoodReminderController
import ch.bfh.anchor.notifications.IosMoodReminders
import ch.bfh.anchor.settings.createSettingsManager

// One of each for the whole process: the screens and the notification handler must share the
// database, or a mood recorded from a notification would not show up in an open tracker.
private val database by lazy { createDatabase() }
private val settingsManager by lazy { createSettingsManager() }
private val reminderController by lazy { IosMoodReminderController(settingsManager) }

/** Called from the Swift app's init, before launch finishes, so notification answers arrive. */
@Suppress("unused") // called from Swift
fun setUpMoodReminders() {
    IosMoodReminders.install(database, settingsManager)
}

@Suppress("unused", "FunctionName") // entry point called from Swift
fun MainViewController() = ComposeUIViewController {
    App(
        database = database,
        settingsManager = settingsManager,
        reminderController = reminderController,
        openMoodRequest = IosMoodReminders.openMoodRequests.intValue
    )
}
