package ch.bfh.anchor

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ch.bfh.anchor.data.AnchorDatabase
import ch.bfh.anchor.data.createDatabase
import ch.bfh.anchor.settings.SettingsManager
import ch.bfh.anchor.settings.createSettingsManager

/**
 * One database and one settings manager per process, as MainViewController does on iOS. Built
 * in onCreate they were recreated on every rotation while the ViewModel kept the first database.
 */
private object AppSingletons {
    @Volatile private var database: AnchorDatabase? = null
    @Volatile private var settingsManager: SettingsManager? = null

    fun database(context: Context): AnchorDatabase = database ?: synchronized(this) {
        database ?: createDatabase(context.applicationContext).also { database = it }
    }

    fun settingsManager(context: Context): SettingsManager = settingsManager ?: synchronized(this) {
        settingsManager ?: createSettingsManager(context.applicationContext).also { settingsManager = it }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val database = AppSingletons.database(this)
        val settingsManager = AppSingletons.settingsManager(this)
        setContent {
            App(database = database, settingsManager = settingsManager)
        }
    }
}
