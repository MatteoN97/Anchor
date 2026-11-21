package ch.bfh.anchor.model

import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.feeling_angry
import ch.bfh.anchor.resources.feeling_angry_phrase
import ch.bfh.anchor.resources.feeling_anxious
import ch.bfh.anchor.resources.feeling_anxious_phrase
import ch.bfh.anchor.resources.feeling_exhausted
import ch.bfh.anchor.resources.feeling_exhausted_phrase
import ch.bfh.anchor.resources.feeling_numb
import ch.bfh.anchor.resources.feeling_numb_phrase
import ch.bfh.anchor.resources.feeling_sad
import ch.bfh.anchor.resources.feeling_sad_phrase
import org.jetbrains.compose.resources.StringResource

/**
 * [labelRes] is the picker label, [phraseRes] the lower-case form used mid-sentence. Both are
 * resources rather than strings: lower-casing a label works in English and nowhere else, and
 * these are the labels the user reads on the worst day they have.
 */
enum class Feeling(
    val labelRes: StringResource,
    val phraseRes: StringResource,
    val emoji: String,
    val matchingCategories: List<String>,
    val skipBreathing: Boolean = false
) {
    ANXIOUS(
        labelRes = Res.string.feeling_anxious,
        phraseRes = Res.string.feeling_anxious_phrase,
        emoji = "😰",
        matchingCategories = listOf("Breathing", "Grounding"),
        skipBreathing = false
    ),
    SAD(
        labelRes = Res.string.feeling_sad,
        phraseRes = Res.string.feeling_sad_phrase,
        emoji = "😢",
        matchingCategories = listOf("Social", "Grounding"),
        skipBreathing = true
    ),
    ANGRY(
        labelRes = Res.string.feeling_angry,
        phraseRes = Res.string.feeling_angry_phrase,
        emoji = "😡",
        matchingCategories = listOf("Physical"),
        skipBreathing = true
    ),
    NUMB(
        labelRes = Res.string.feeling_numb,
        phraseRes = Res.string.feeling_numb_phrase,
        emoji = "😶",
        matchingCategories = listOf("Grounding", "Physical"),
        skipBreathing = false
    ),
    EXHAUSTED(
        labelRes = Res.string.feeling_exhausted,
        phraseRes = Res.string.feeling_exhausted_phrase,
        emoji = "😴",
        matchingCategories = listOf("Grounding"),
        skipBreathing = true
    )
}
