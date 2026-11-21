package ch.bfh.anchor.settings

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings

/**
 * Reads the file PreferenceManager.getDefaultSharedPreferences used before the migration
 * ("<applicationId>_preferences"), so a name saved by the old Android build is still there.
 */
fun createSettingsManager(context: Context): SettingsManager {
    val prefs = context.getSharedPreferences(
        "${context.packageName}_preferences",
        Context.MODE_PRIVATE
    )
    return SettingsManager(SharedPreferencesSettings(prefs))
}
