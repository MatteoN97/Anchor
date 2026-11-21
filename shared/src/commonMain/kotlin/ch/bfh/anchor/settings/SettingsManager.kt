package ch.bfh.anchor.settings

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

    private companion object {
        const val KEY_USER_NAME = "user_name"
    }
}
