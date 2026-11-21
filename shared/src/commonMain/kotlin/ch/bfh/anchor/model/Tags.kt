package ch.bfh.anchor.model

/**
 * Encoding for the single TEXT column that holds a note's tags.
 *
 * Day 1 joined the tags with a comma, so any tag containing a comma came back out as two tags.
 * The separator here is the ASCII unit separator (U+001F), which cannot be typed on a phone
 * keyboard, so the round trip is lossless. Migration 4 to 5 rewrites the rows written by the
 * old format.
 */
object Tags {
    val SEPARATOR: String = Char(31).toString()

    fun encode(tags: List<String>): String =
        tags.map { it.trim() }.filter { it.isNotEmpty() }.joinToString(SEPARATOR)

    fun decode(stored: String): List<String> =
        stored.split(SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }
}
