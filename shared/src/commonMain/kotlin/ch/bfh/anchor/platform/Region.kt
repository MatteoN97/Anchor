package ch.bfh.anchor.platform

import androidx.compose.runtime.Composable

/**
 * The device's country as an upper-case ISO 3166-1 alpha-2 code, or null if unknown.
 * Used only to pick a default crisis line; never leaves the device.
 */
@Composable
expect fun rememberDeviceRegion(): String?
