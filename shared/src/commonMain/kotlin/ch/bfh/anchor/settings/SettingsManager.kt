package ch.bfh.anchor.settings

import com.russhwolf.settings.Settings

class SettingsManager(private val settings: Settings) {
    var userName: String
        get() = settings.getString(KEY_USER_NAME, "")
        set(value) { settings.putString(KEY_USER_NAME, value) }

    var moodRemindersEnabled: Boolean
        get() = settings.getBoolean(KEY_MOOD_ENABLED, false)
        set(value) { settings.putBoolean(KEY_MOOD_ENABLED, value) }

    /** Preserve the original once-a-day reminder time for users who enabled it before this update. */
    private fun previousReminderMinute(): Int? {
        val hour = settings.getInt(KEY_OLD_HOUR, -1)
        if (hour !in 0..23) return null
        val minute = settings.getInt(KEY_OLD_MINUTE, 0).coerceIn(0, 59)
        return hour * 60 + minute
    }

    /** Local minute-of-day (0..1439). New installations default to 09:00–21:00. */
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
        const val KEY_MOOD_ENABLED = "mood_reminders_enabled"
        const val KEY_OLD_HOUR = "mood_reminder_hour"
        const val KEY_OLD_MINUTE = "mood_reminder_minute"
        const val KEY_MOOD_START = "mood_window_start_minutes"
        const val KEY_MOOD_END = "mood_window_end_minutes"
        const val KEY_MOOD_INTERVAL = "mood_reminder_interval_hours"
    }
}
