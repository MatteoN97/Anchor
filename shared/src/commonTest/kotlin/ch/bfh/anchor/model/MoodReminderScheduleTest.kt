package ch.bfh.anchor.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MoodReminderScheduleTest {
    @Test
    fun windowIncludesEndWhenItFallsOnTheInterval() {
        assertEquals(listOf(540, 720, 900, 1080, 1260),
            MoodReminderSchedule.slots(9 * 60, 21 * 60, 3))
    }

    @Test
    fun irregularEndDoesNotCreateAnOutOfWindowNotification() {
        assertEquals(listOf(540, 720, 900),
            MoodReminderSchedule.slots(9 * 60, 17 * 60, 3))
    }

    @Test
    fun singleTimeWindowSupportsOldDailyReminders() {
        assertEquals(listOf(20 * 60),
            MoodReminderSchedule.slots(20 * 60, 20 * 60, 3))
    }

    @Test
    fun rejectsInvalidSettings() {
        assertFailsWith<IllegalArgumentException> { MoodReminderSchedule.slots(1200, 540, 3) }
        assertFailsWith<IllegalArgumentException> { MoodReminderSchedule.slots(0, 1439, 0) }
    }
}
