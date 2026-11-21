package ch.bfh.anchor.platform

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Short month names per language. kotlinx-datetime has no locale data, and only four languages
 * are needed, so they are spelled out here rather than pulled from the platform.
 */
private val shortMonths: Map<String, MonthNames> = mapOf(
    "en" to MonthNames.ENGLISH_ABBREVIATED,
    "fr" to MonthNames(listOf("janv.", "févr.", "mars", "avr.", "mai", "juin",
        "juil.", "août", "sept.", "oct.", "nov.", "déc.")),
    "de" to MonthNames(listOf("Jan.", "Feb.", "März", "Apr.", "Mai", "Juni",
        "Juli", "Aug.", "Sept.", "Okt.", "Nov.", "Dez.")),
    "it" to MonthNames(listOf("gen", "feb", "mar", "apr", "mag", "giu",
        "lug", "ago", "set", "ott", "nov", "dic"))
)

/** "Nov 21, 2026" in English, day first elsewhere: "21 nov. 2026", "21. Nov. 2026", "21 nov 2026". */
private fun noteDateFormat(language: String) = LocalDateTime.Format {
    val months = shortMonths[language] ?: MonthNames.ENGLISH_ABBREVIATED
    when (language) {
        "fr", "it" -> { day(Padding.NONE); char(' '); monthName(months); char(' '); year() }
        "de" -> { day(Padding.NONE); chars(". "); monthName(months); char(' '); year() }
        else -> { monthName(months); char(' '); day(Padding.NONE); chars(", "); year() }
    }
}

/** [language] is an ISO 639 code such as "fr"; anything unknown is formatted in English. */
fun formatNoteDate(epochMillis: Long, language: String = "en"): String =
    noteDateFormat(language).format(
        Instant.fromEpochMilliseconds(epochMillis)
            .toLocalDateTime(TimeZone.currentSystemDefault())
    )

/**
 * Calendar days between [epochMillis] and today, in the device's time zone. Counting 24-hour
 * blocks instead called a session from 23:00 last night "today" at 08:00.
 */
fun daysSince(epochMillis: Long): Int {
    val zone = TimeZone.currentSystemDefault()
    val then = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(zone).date
    val today = Instant.fromEpochMilliseconds(nowMillis()).toLocalDateTime(zone).date
    return then.daysUntil(today)
}

/** Compact local timestamp for the mood history; no sensitive details in notifications. */
private fun moodDateFormat(language: String) = LocalDateTime.Format {
    val months = shortMonths[language] ?: MonthNames.ENGLISH_ABBREVIATED
    when (language) {
        "fr", "it" -> { day(Padding.NONE); char(' '); monthName(months) }
        "de" -> { day(Padding.NONE); chars(". "); monthName(months) }
        else -> { monthName(months); char(' '); day(Padding.NONE) }
    }
    chars(", ")
    hour()
    char(':')
    minute()
}

fun formatMoodDateTime(epochMillis: Long, language: String = "en"): String =
    moodDateFormat(language).format(
        Instant.fromEpochMilliseconds(epochMillis)
            .toLocalDateTime(TimeZone.currentSystemDefault())
    )
