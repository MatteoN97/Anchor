package ch.bfh.anchor.model

import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MoodEntryTest {
    @Test
    fun allFiveScoresHaveStableLabelsAndEmojis() {
        assertEquals(
            listOf(Res.string.mood_not_great, Res.string.mood_low, Res.string.mood_okay,
                Res.string.mood_good, Res.string.mood_great),
            MoodScale.scores.map(MoodScale::labelRes))
        MoodScale.scores.forEach { score ->
            assertEquals(true, MoodScale.emoji(score).isNotBlank())
        }
        assertEquals(
            listOf(Res.string.mood_great, Res.string.mood_good, Res.string.mood_okay,
                Res.string.mood_low, Res.string.mood_not_great),
            MoodScale.displayScores.map(MoodScale::labelRes))
        assertEquals(listOf("😊", "🙂", "😐", "😔", "😢"),
            MoodScale.displayScores.map(MoodScale::emoji))
    }

    @Test
    fun outsideScaleIsRejected() {
        assertFailsWith<IllegalArgumentException> { MoodEntry(score = 0) }
        assertFailsWith<IllegalArgumentException> { MoodEntry(score = 6) }
        assertFailsWith<IllegalArgumentException> { MoodScale.labelRes(-1) }
    }

    @Test
    fun crisisRatingsConvertWithoutChangingLegacyMeaning() {
        assertEquals(listOf(5, 4, 3, 2, 1),
            (1..5).map(MoodScale::fromCrisisRating))
        assertFailsWith<IllegalArgumentException> { MoodScale.fromCrisisRating(6) }
    }

    @Test
    fun notificationRecordsKeepPromptIdSeparateFromAppEntries() {
        val app = MoodEntry(score = 3)
        val fromReminder = MoodEntry(score = 5, source = MoodEntry.SOURCE_NOTIFICATION,
            promptId = "test-prompt")
        assertEquals(null, app.promptId)
        assertEquals(MoodEntry.SOURCE_APP, app.source)
        assertEquals("test-prompt", fromReminder.promptId)
    }

    @Test
    fun onlyOneInAppCheckInPerHour() {
        val hour = MoodCheckIn.INTERVAL_MILLIS
        val first = MoodEntry(recordedAt = 10 * hour, score = 3)
        assertEquals(null, MoodCheckIn.blocking(emptyList(), 10 * hour))
        assertEquals(first, MoodCheckIn.blocking(listOf(first), 10 * hour + hour - 1))
        assertEquals(null, MoodCheckIn.blocking(listOf(first), 11 * hour))
        assertEquals(11 * hour, MoodCheckIn.nextAllowedAt(first))
    }

    @Test
    fun remindersAndCrisisCheckInsDoNotBlockTheTracker() {
        val hour = MoodCheckIn.INTERVAL_MILLIS
        val others = listOf(
            MoodEntry(recordedAt = 10 * hour, score = 3, source = MoodEntry.SOURCE_NOTIFICATION, promptId = "p"),
            MoodEntry(recordedAt = 10 * hour, score = 2, source = MoodEntry.SOURCE_CRISIS)
        )
        assertEquals(null, MoodCheckIn.blocking(others, 10 * hour + 1))
    }

    @Test
    fun theLatestInAppCheckInIsTheOneThatCounts() {
        val hour = MoodCheckIn.INTERVAL_MILLIS
        val old = MoodEntry(recordedAt = 8 * hour, score = 1)
        val recent = MoodEntry(recordedAt = 10 * hour, score = 5)
        assertEquals(recent, MoodCheckIn.blocking(listOf(recent, old), 10 * hour + 5))
    }
}
