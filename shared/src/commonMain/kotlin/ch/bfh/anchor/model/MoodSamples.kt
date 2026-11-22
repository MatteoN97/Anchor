package ch.bfh.anchor.model

import ch.bfh.anchor.platform.localDayBounds
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Two weeks of plausible, made-up check-ins so the mood chart has something to show in a demo
 * or a test on a fresh install. Every entry has [MoodEntry.SOURCE_SAMPLE], so they are labelled
 * "Sample" in the history and can all be removed again; real check-ins are never touched.
 */
object MoodSamples {
    private const val HOUR = 3_600_000L
    private const val MINUTE = 60_000L

    /** Check-ins for today (up to [now]) and the [days] - 1 days before, a few per day. */
    fun generate(now: Long, days: Int = 14, seed: Int = 7): List<MoodEntry> {
        val random = Random(seed)
        val today = localDayBounds(now).first
        return (days - 1 downTo 0).flatMap { daysAgo ->
            // Midday of that day, then its real start: correct across daylight-saving changes.
            val dayStart = localDayBounds(today - daysAgo * 24 * HOUR + 12 * HOUR).first
            // A slow wave over the fortnight, so weeks have better and worse stretches.
            val dayMood = 3.2 + 0.9 * sin(daysAgo * 2 * PI / 9)
            val count = 3 + random.nextInt(3)
            (0 until count).mapNotNull { slot ->
                // Spread over the waking hours, 08:00 to 22:00, one slot each.
                val slotLength = 14 * 60 / count
                val minute = 8 * 60 + slot * slotLength + random.nextInt(slotLength - 10)
                val at = dayStart + minute * MINUTE
                if (at > now) return@mapNotNull null
                // Mornings a little lower, a dip mid-afternoon, plus noise.
                val hour = minute / 60.0
                val daily = 0.5 * sin((hour - 9) * PI / 7) - if (hour in 15.0..17.0) 0.5 else 0.0
                val score = (dayMood + daily + random.nextDouble(-0.9, 0.9)).roundToInt().coerceIn(1, 5)
                MoodEntry(recordedAt = at, score = score, source = MoodEntry.SOURCE_SAMPLE)
            }
        }
    }
}
