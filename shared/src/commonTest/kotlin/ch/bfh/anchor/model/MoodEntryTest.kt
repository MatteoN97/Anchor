package ch.bfh.anchor.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MoodEntryTest {
    @Test
    fun allFiveScoresHaveStableLabelsAndEmojis() {
        assertEquals(listOf("Not great", "Low", "Okay", "Good", "Great"),
            MoodScale.scores.map(MoodScale::label))
        MoodScale.scores.forEach { score ->
            assertEquals(true, MoodScale.emoji(score).isNotBlank())
        }
        assertEquals(listOf("Great", "Good", "Okay", "Low", "Not great"),
            MoodScale.displayScores.map(MoodScale::label))
        assertEquals(listOf("😊", "🙂", "😐", "😔", "😢"),
            MoodScale.displayScores.map(MoodScale::emoji))
    }

    @Test
    fun outsideScaleIsRejected() {
        assertFailsWith<IllegalArgumentException> { MoodEntry(score = 0) }
        assertFailsWith<IllegalArgumentException> { MoodEntry(score = 6) }
        assertFailsWith<IllegalArgumentException> { MoodScale.label(-1) }
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
