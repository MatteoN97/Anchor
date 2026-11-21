package ch.bfh.anchor.settings

import com.russhwolf.settings.Settings

class SettingsManager(private val settings: Settings) {

    var userName: String
        get() = settings.getString(KEY_USER_NAME, "")
        set(value) {
            settings.putString(KEY_USER_NAME, value)
        }

    private companion object {
        const val KEY_USER_NAME = "user_name"
    }
}
