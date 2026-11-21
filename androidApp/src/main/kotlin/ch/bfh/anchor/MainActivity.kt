package ch.bfh.anchor

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableIntStateOf
import ch.bfh.anchor.data.createDatabase
import ch.bfh.anchor.notifications.AndroidMoodReminderController
import ch.bfh.anchor.notifications.EXTRA_OPEN_MOOD
import ch.bfh.anchor.settings.createSettingsManager

class MainActivity : ComponentActivity() {
    private val openMoodRequest = mutableIntStateOf(0)
    private var permissionCallback: ((Boolean) -> Unit)? = null
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionCallback?.invoke(granted)
        permissionCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (intent?.getBooleanExtra(EXTRA_OPEN_MOOD, false) == true) {
            openMoodRequest.intValue++
        }
        val database = createDatabase(applicationContext)
        val settingsManager = createSettingsManager(applicationContext)
        val reminderController = AndroidMoodReminderController(this, settingsManager) { callback ->
            permissionCallback = callback
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            App(database = database, settingsManager = settingsManager,
                reminderController = reminderController,
                openMoodRequest = openMoodRequest.intValue)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_MOOD, false)) {
            openMoodRequest.intValue++
        }
    }
}
