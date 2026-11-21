package ch.bfh.anchor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ch.bfh.anchor.data.createDatabase
import ch.bfh.anchor.settings.createSettingsManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val database = createDatabase(applicationContext)
        val settingsManager = createSettingsManager(applicationContext)
        setContent {
            App(database = database, settingsManager = settingsManager)
        }
    }
}
