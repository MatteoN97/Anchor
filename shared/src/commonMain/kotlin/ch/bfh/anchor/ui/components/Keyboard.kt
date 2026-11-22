package ch.bfh.anchor.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/**
 * Scrolls a focused text field back into view once the keyboard has finished opening.
 *
 * A field scrolls itself into view when it gains focus, but on iOS that happens before the
 * keyboard has shrunk the page, so a field low on the screen (the note editor's question input)
 * ended up behind the keyboard: the tap seemed to do nothing. This asks again each time the
 * keyboard height settles, for as long as the field has focus.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.keepVisibleAboveKeyboard(): Modifier {
    val requester = remember { BringIntoViewRequester() }
    var focused by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    LaunchedEffect(focused) {
        if (!focused) return@LaunchedEffect
        snapshotFlow { ime.getBottom(density) }.collectLatest { height ->
            if (height > 0) {
                // Wait until the keyboard animation stops changing the height.
                delay(120)
                requester.bringIntoView()
            }
        }
    }
    return this
        .bringIntoViewRequester(requester)
        .onFocusChanged { focused = it.isFocused }
}

/** Words in [text], split on whitespace. A plain loop: it runs on every keystroke. */
fun countWords(text: String): Int {
    var words = 0
    var inWord = false
    for (c in text) {
        if (c.isWhitespace()) inWord = false
        else if (!inWord) { inWord = true; words++ }
    }
    return words
}
