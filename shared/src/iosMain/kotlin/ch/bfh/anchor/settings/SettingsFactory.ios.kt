package ch.bfh.anchor.settings

import com.russhwolf.settings.NSUserDefaultsSettings
import platform.Foundation.NSUserDefaults

fun createSettingsManager(): SettingsManager =
    SettingsManager(NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults))
