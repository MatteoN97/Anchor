package ch.bfh.anchor.model

/**
 * The languages Anchor is translated into. [System] follows the phone's language, falling back
 * to English when the phone uses a language Anchor does not have.
 *
 * [nativeName] is deliberately not translated: a language is listed under its own name, so
 * someone who switched to a language they cannot read can still find their way back.
 */
enum class AppLanguage(val code: String?, val nativeName: String) {
    System(null, ""),
    English("en", "English"),
    French("fr", "Français"),
    German("de", "Deutsch"),
    Italian("it", "Italiano");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code != null && it.code == code } ?: System
    }
}
