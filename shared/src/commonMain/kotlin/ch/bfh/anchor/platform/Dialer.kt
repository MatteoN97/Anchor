package ch.bfh.anchor.platform

import androidx.compose.runtime.Composable

/** Opens the system dialer pre-filled with [number], without placing the call. */
@Composable
expect fun rememberPhoneDialer(): (number: String) -> Unit

/** Strips spaces, dashes and other formatting so [number] is valid in a `tel:` URI. */
internal fun dialable(number: String): String = number.filter { it.isDigit() || it in "+*#" }
