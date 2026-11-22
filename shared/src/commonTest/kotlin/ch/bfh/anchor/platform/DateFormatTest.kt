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

    // 5 March 2026 is a Thursday.
    @Test
    fun homeHeaderNamesTheWeekdayInEachLanguage() {
        assertEquals("Thursday, 5 March", formatLongDay(march5, "en"))
        assertEquals("jeudi 5 mars", formatLongDay(march5, "fr"))
        assertEquals("Donnerstag, 5. März", formatLongDay(march5, "de"))
        assertEquals("giovedì 5 marzo", formatLongDay(march5, "it"))
        assertEquals("Thursday, 5 March", formatLongDay(march5, "es"))
    }

    @Test
    fun noteEditorDateLineHasWeekdayAndTime() {
        assertEquals("Thu, Mar 5 · 09:07", formatNoteDateTime(march5, "en"))
        assertEquals("jeu. 5 mars · 09:07", formatNoteDateTime(march5, "fr"))
        assertEquals("Do., 5. März · 09:07", formatNoteDateTime(march5, "de"))
        assertEquals("gio 5 mar · 09:07", formatNoteDateTime(march5, "it"))
    }

    @Test
    fun theDayViewCoversTheLocalCalendarDay() {
        val (start, end) = localDayBounds(march5)
        val zone = TimeZone.currentSystemDefault()
        assertEquals(LocalDateTime(2026, 3, 5, 0, 0).toInstant(zone).toEpochMilliseconds(), start)
        assertEquals(LocalDateTime(2026, 3, 6, 0, 0).toInstant(zone).toEpochMilliseconds(), end)
        assertEquals("09:07", formatTimeOfDay(march5))
    }
}
