package ch.bfh.anchor.settings

import ch.bfh.anchor.model.BreathingPattern
import com.russhwolf.settings.Settings

class SettingsManager(private val settings: Settings) {

    var userName: String
        get() = settings.getString(KEY_USER_NAME, "")
        set(value) {
            settings.putString(KEY_USER_NAME, value)
        }

    /** Pattern used by the breathing step of the crisis flow. */
    var crisisBreathingPattern: BreathingPattern
        get() {
            val stored = settings.getString(KEY_CRISIS_BREATHING_PATTERN, "")
            return BreathingPattern.entries.firstOrNull { it.name == stored }
                ?: BreathingPattern.Calm
        }
        set(value) {
            settings.putString(KEY_CRISIS_BREATHING_PATTERN, value.name)
        }

    private companion object {
        const val KEY_USER_NAME = "user_name"
        const val KEY_CRISIS_BREATHING_PATTERN = "crisis_breathing_pattern"
    }
}
