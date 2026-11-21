package ch.bfh.anchor.platform

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals

class DateFormatTest {
    private val march5 = LocalDateTime(2026, 3, 5, 9, 7)
        .toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

    @Test
    fun noteDatesFollowEachLanguagesConventions() {
        assertEquals("Mar 5, 2026", formatNoteDate(march5, "en"))
        assertEquals("5 mars 2026", formatNoteDate(march5, "fr"))
        assertEquals("5. März 2026", formatNoteDate(march5, "de"))
        assertEquals("5 mar 2026", formatNoteDate(march5, "it"))
    }

    @Test
    fun unknownLanguagesFallBackToEnglish() {
        assertEquals("Mar 5, 2026", formatNoteDate(march5, "es"))
    }

    @Test
    fun moodTimesKeepA24HourClock() {
        assertEquals("Mar 5, 09:07", formatMoodDateTime(march5, "en"))
        assertEquals("5. März, 09:07", formatMoodDateTime(march5, "de"))
    }
}
