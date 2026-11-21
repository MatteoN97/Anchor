package ch.bfh.anchor.platform

import androidx.compose.runtime.Composable

/** Prevents the screen from dimming or locking while this composable is in the composition. */
@Composable
expect fun KeepScreenOn()
