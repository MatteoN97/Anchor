package ch.bfh.anchor.settings

import ch.bfh.anchor.model.AppLanguage
import ch.bfh.anchor.model.BreathingPattern
import ch.bfh.anchor.model.HelpContact
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Observable wrapper around the preference store.
 *
 * Exactly one instance exists per platform entry point (MainActivity, MainViewController), so the
 * flow below is the single source of truth on screen and the Settings store behind it only handles
 * persistence. Reading the value non-observably, as the first version did, meant Home kept showing
 * the old name after it was changed in Settings.
 */
class SettingsManager(private val settings: Settings) {

    private val _userName = MutableStateFlow(settings.getString(KEY_USER_NAME, ""))
    val userName: StateFlow<String> = _userName.asStateFlow()

    fun setUserName(value: String) {
        val trimmed = value.trim()
        settings.putString(KEY_USER_NAME, trimmed)
        _userName.value = trimmed
    }

    private val _appLanguage =
        MutableStateFlow(AppLanguage.fromCode(settings.getStringOrNull(KEY_APP_LANGUAGE)))

    /** The language chosen in Settings; [AppLanguage.System] follows the phone. Observable so the UI switches at once. */
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    fun setAppLanguage(language: AppLanguage) {
        val code = language.code
        if (code == null) settings.remove(KEY_APP_LANGUAGE) else settings.putString(KEY_APP_LANGUAGE, code)
        _appLanguage.value = language
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

    /** A crisis line the user chose themselves. Null means use the one for their region. */
    var customCrisisLine: HelpContact?
        get() = getContact(KEY_CRISIS_LINE_NAME, KEY_CRISIS_LINE_NUMBER)
        set(value) = putContact(KEY_CRISIS_LINE_NAME, KEY_CRISIS_LINE_NUMBER, value)

    /** A friend, relative or therapist offered alongside the crisis line. Null if not set. */
    var trustedContact: HelpContact?
        get() = getContact(KEY_TRUSTED_NAME, KEY_TRUSTED_NUMBER)
        set(value) = putContact(KEY_TRUSTED_NAME, KEY_TRUSTED_NUMBER, value)

    private fun getContact(nameKey: String, numberKey: String): HelpContact? {
        val number = settings.getString(numberKey, "")
        if (number.isBlank()) return null
        return HelpContact(name = settings.getString(nameKey, ""), number = number)
    }

    private fun putContact(nameKey: String, numberKey: String, value: HelpContact?) {
        if (value == null || value.number.isBlank()) {
            settings.remove(nameKey)
            settings.remove(numberKey)
        } else {
            settings.putString(nameKey, value.name.trim())
            settings.putString(numberKey, value.number.trim())
        }
    }

    // Optional local mood reminders. Existing once-per-day settings migrate on read.
    var moodRemindersEnabled: Boolean
        get() = settings.getBoolean(KEY_MOOD_ENABLED, false)
        set(value) { settings.putBoolean(KEY_MOOD_ENABLED, value) }

    private fun previousReminderMinute(): Int? {
        val hour = settings.getInt(KEY_OLD_HOUR, -1)
        if (hour !in 0..23) return null
        val minute = settings.getInt(KEY_OLD_MINUTE, 0).coerceIn(0, 59)
        return hour * 60 + minute
    }

    var moodReminderStartMinutes: Int
        get() = settings.getInt(KEY_MOOD_START, previousReminderMinute() ?: 9 * 60)
            .coerceIn(0, 1439)
        set(value) { settings.putInt(KEY_MOOD_START, value.coerceIn(0, 1439)) }

    var moodReminderEndMinutes: Int
        get() = settings.getInt(KEY_MOOD_END, previousReminderMinute() ?: 21 * 60)
            .coerceIn(moodReminderStartMinutes, 1439)
        set(value) { settings.putInt(KEY_MOOD_END, value.coerceIn(0, 1439)) }

    var moodReminderIntervalHours: Int
        get() = settings.getInt(KEY_MOOD_INTERVAL, 3).let { stored ->
            if (stored in listOf(1, 2, 3, 4, 6, 8, 12)) stored else 3
        }
        set(value) { settings.putInt(KEY_MOOD_INTERVAL, value) }

    private companion object {
        const val KEY_USER_NAME = "user_name"
        const val KEY_APP_LANGUAGE = "app_language"
        const val KEY_MOOD_ENABLED = "mood_reminders_enabled"
        const val KEY_OLD_HOUR = "mood_reminder_hour"
        const val KEY_OLD_MINUTE = "mood_reminder_minute"
        const val KEY_MOOD_START = "mood_window_start_minutes"
        const val KEY_MOOD_END = "mood_window_end_minutes"
        const val KEY_MOOD_INTERVAL = "mood_reminder_interval_hours"
        const val KEY_CRISIS_BREATHING_PATTERN = "crisis_breathing_pattern"
        const val KEY_CRISIS_LINE_NAME = "crisis_line_name"
        const val KEY_CRISIS_LINE_NUMBER = "crisis_line_number"
        const val KEY_TRUSTED_NAME = "trusted_contact_name"
        const val KEY_TRUSTED_NUMBER = "trusted_contact_number"
    }
}
