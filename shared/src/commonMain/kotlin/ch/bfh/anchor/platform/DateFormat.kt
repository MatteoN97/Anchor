package ch.bfh.anchor.platform



import kotlinx.datetime.LocalDateTime

import kotlinx.datetime.TimeZone

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



/** Whole days elapsed since [epochMillis], matching TimeUnit.MILLISECONDS.toDays. */

fun daysSince(epochMillis: Long): Int =

    ((nowMillis() - epochMillis) / 86_400_000L).toInt()


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
