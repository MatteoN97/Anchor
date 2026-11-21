package ch.bfh.anchor.platform

import androidx.compose.runtime.Composable

/** Opens the system dialer pre-filled with [number], without placing the call. */
@Composable
expect fun rememberPhoneDialer(): (number: String) -> Unit
