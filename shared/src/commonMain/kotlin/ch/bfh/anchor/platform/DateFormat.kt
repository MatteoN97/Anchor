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
 * Replaces SimpleDateFormat("MMM d, yyyy") from the Android app.
 *
 * Behaviour delta: month names are always English. The Android version used
 * Locale.getDefault(); kotlinx-datetime has no locale-aware month names.
 */
private val noteDateFormat = LocalDateTime.Format {
    monthName(MonthNames.ENGLISH_ABBREVIATED)
    char(' ')
    day(Padding.NONE)
    chars(", ")
    year()
}

fun formatNoteDate(epochMillis: Long): String =
    noteDateFormat.format(
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
private val moodDateFormat = LocalDateTime.Format {
    monthName(MonthNames.ENGLISH_ABBREVIATED)
    char(' ')
    day(Padding.NONE)
    chars(", ")
    hour()
    char(':')
    minute()
}

fun formatMoodDateTime(epochMillis: Long): String =
    moodDateFormat.format(
        Instant.fromEpochMilliseconds(epochMillis)
            .toLocalDateTime(TimeZone.currentSystemDefault())
    )

/** The local hour, 0–23, for time-of-day greetings. */
fun currentHour(): Int =
    Instant.fromEpochMilliseconds(nowMillis()).toLocalDateTime(TimeZone.currentSystemDefault()).hour

/** "Saturday, 21 November" — the Home header. English only, like the other formats here. */
private val longDayFormat = LocalDateTime.Format {
    dayOfWeek(kotlinx.datetime.format.DayOfWeekNames.ENGLISH_FULL)
    chars(", ")
    day(Padding.NONE)
    char(' ')
    monthName(MonthNames.ENGLISH_FULL)
}

fun formatLongDay(epochMillis: Long): String =
    longDayFormat.format(
        Instant.fromEpochMilliseconds(epochMillis)
            .toLocalDateTime(TimeZone.currentSystemDefault())
    )

/** "Sat, Nov 21 · 21:48" — the note editor's date line. */
private val noteDateTimeFormat = LocalDateTime.Format {
    dayOfWeek(kotlinx.datetime.format.DayOfWeekNames.ENGLISH_ABBREVIATED)
    chars(", ")
    monthName(MonthNames.ENGLISH_ABBREVIATED)
    char(' ')
    day(Padding.NONE)
    chars(" · ")
    hour()
    char(':')
    minute()
}

fun formatNoteDateTime(epochMillis: Long): String =
    noteDateTimeFormat.format(
        Instant.fromEpochMilliseconds(epochMillis)
            .toLocalDateTime(TimeZone.currentSystemDefault())
    )
