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
}
