package ch.bfh.anchor.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import ch.bfh.anchor.platform.nowMillis
import ch.bfh.anchor.platform.randomId

/** Mood records share one stable scale (1 = Not great, 5 = Great).
 * Crisis check-ins keep their existing reversed numeric scale in their own table,
 * but their completed results are also recorded here with a converted score. */
@Entity(tableName = "mood_entries", indices = [Index(value = ["promptId"], unique = true)])
data class MoodEntry(
    @PrimaryKey val id: String = randomId(),
    val recordedAt: Long = nowMillis(),
    val score: Int,
    val source: String = SOURCE_APP,
    /** A notification's stable ID; unique to make receiver retries / repeated taps idempotent. */
    val promptId: String? = null
) {
    init { require(score in 1..5) { "Mood score must be between 1 and 5" } }

    companion object {
        const val SOURCE_APP = "APP"
        const val SOURCE_NOTIFICATION = "NOTIFICATION"
        const val SOURCE_TEST_NOTIFICATION = "TEST_NOTIFICATION"
        const val SOURCE_CRISIS = "CRISIS"
    }
}

object MoodScale {
    val scores: IntRange = 1..5
    /** Display from positive to negative, matching Anchor's existing support check-in. */
    val displayScores: List<Int> = listOf(5, 4, 3, 2, 1)

    /** Legacy crisis answers are 1 = Great through 5 = Not great. */
    fun fromCrisisRating(rating: Int): Int {
        require(rating in 1..5) { "Crisis rating must be between 1 and 5" }
        return 6 - rating
    }
    fun label(score: Int): String = when (score) {
        1 -> "Not great"
        2 -> "Low"
        3 -> "Okay"
        4 -> "Good"
        5 -> "Great"
        else -> throw IllegalArgumentException("Unknown mood: $score")
    }
    fun emoji(score: Int): String = when (score) {
        1 -> "😢"
        2 -> "😔"
        3 -> "😐"
        4 -> "🙂"
        5 -> "😊"
        else -> throw IllegalArgumentException("Unknown mood: $score")
    }
}
