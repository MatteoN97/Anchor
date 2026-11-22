package ch.bfh.anchor.platform

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.format.DayOfWeekNames
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

/** The local hour, 0–23, for time-of-day greetings. */
fun currentHour(): Int =
    Instant.fromEpochMilliseconds(nowMillis()).toLocalDateTime(TimeZone.currentSystemDefault()).hour

private val fullMonths: Map<String, MonthNames> = mapOf(
    "en" to MonthNames.ENGLISH_FULL,
    "fr" to MonthNames(listOf("janvier", "février", "mars", "avril", "mai", "juin",
        "juillet", "août", "septembre", "octobre", "novembre", "décembre")),
    "de" to MonthNames(listOf("Januar", "Februar", "März", "April", "Mai", "Juni",
        "Juli", "August", "September", "Oktober", "November", "Dezember")),
    "it" to MonthNames(listOf("gennaio", "febbraio", "marzo", "aprile", "maggio", "giugno",
        "luglio", "agosto", "settembre", "ottobre", "novembre", "dicembre"))
)

/** Monday first, as kotlinx-datetime expects. */
private val fullDays: Map<String, DayOfWeekNames> = mapOf(
    "en" to DayOfWeekNames.ENGLISH_FULL,
    "fr" to DayOfWeekNames(listOf("lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche")),
    "de" to DayOfWeekNames(listOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag")),
    "it" to DayOfWeekNames(listOf("lunedì", "martedì", "mercoledì", "giovedì", "venerdì", "sabato", "domenica"))
)

private val shortDays: Map<String, DayOfWeekNames> = mapOf(
    "en" to DayOfWeekNames.ENGLISH_ABBREVIATED,
    "fr" to DayOfWeekNames(listOf("lun.", "mar.", "mer.", "jeu.", "ven.", "sam.", "dim.")),
    "de" to DayOfWeekNames(listOf("Mo.", "Di.", "Mi.", "Do.", "Fr.", "Sa.", "So.")),
    "it" to DayOfWeekNames(listOf("lun", "mar", "mer", "gio", "ven", "sab", "dom"))
)

/**
 * The Home header: "Saturday, 21 November", "samedi 21 novembre", "Samstag, 21. November",
 * "sabato 21 novembre".
 */
private fun longDayFormat(language: String) = LocalDateTime.Format {
    val days = fullDays[language] ?: DayOfWeekNames.ENGLISH_FULL
    val months = fullMonths[language] ?: MonthNames.ENGLISH_FULL
    when (language) {
        "fr", "it" -> { dayOfWeek(days); char(' '); day(Padding.NONE); char(' '); monthName(months) }
        "de" -> { dayOfWeek(days); chars(", "); day(Padding.NONE); chars(". "); monthName(months) }
        else -> { dayOfWeek(days); chars(", "); day(Padding.NONE); char(' '); monthName(months) }
    }
}

fun formatLongDay(epochMillis: Long, language: String = "en"): String =
    longDayFormat(language).format(
        Instant.fromEpochMilliseconds(epochMillis)
            .toLocalDateTime(TimeZone.currentSystemDefault())
    )

/**
 * The note editor's date line: "Sat, Nov 21 · 21:48", "sam. 21 nov. · 21:48",
 * "Sa., 21. Nov. · 21:48", "sab 21 nov · 21:48".
 */
private fun noteDateTimeFormat(language: String) = LocalDateTime.Format {
    val days = shortDays[language] ?: DayOfWeekNames.ENGLISH_ABBREVIATED
    val months = shortMonths[language] ?: MonthNames.ENGLISH_ABBREVIATED
    when (language) {
        "fr", "it" -> { dayOfWeek(days); char(' '); day(Padding.NONE); char(' '); monthName(months) }
        "de" -> { dayOfWeek(days); chars(", "); day(Padding.NONE); chars(". "); monthName(months) }
        else -> { dayOfWeek(days); chars(", "); monthName(months); char(' '); day(Padding.NONE) }
    }
    chars(" · ")
    hour()
    char(':')
    minute()
}

fun formatNoteDateTime(epochMillis: Long, language: String = "en"): String =
    noteDateTimeFormat(language).format(
        Instant.fromEpochMilliseconds(epochMillis)
            .toLocalDateTime(TimeZone.currentSystemDefault())
    )

/**
 * The local calendar day containing [epochMillis], as start (inclusive) and end (exclusive)
 * epoch millis. Not always 24 hours: a daylight-saving day is 23 or 25.
 */
fun localDayBounds(epochMillis: Long): Pair<Long, Long> {
    val zone = TimeZone.currentSystemDefault()
    val date = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(zone).date
    val start = date.atStartOfDayIn(zone).toEpochMilliseconds()
    val end = date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone).toEpochMilliseconds()
    return start to end
}

/** "09:07": the local time of day, 24-hour clock in every language. */
fun formatTimeOfDay(epochMillis: Long): String =
    LocalDateTime.Format { hour(); char(':'); minute() }.format(
        Instant.fromEpochMilliseconds(epochMillis)
            .toLocalDateTime(TimeZone.currentSystemDefault())
    )
