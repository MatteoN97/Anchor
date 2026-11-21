package ch.bfh.anchor.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TagsTest {

    @Test
    fun roundTripsAPlainList() {
        val tags = listOf("Insight", "Homework")
        assertEquals(tags, Tags.decode(Tags.encode(tags)))
    }

    @Test
    fun keepsATagContainingACommaInOnePiece() {
        // The day-1 format joined on "," and split this tag back into two.
        val tags = listOf("Sleep, finally", "Progress")
        assertEquals(tags, Tags.decode(Tags.encode(tags)))
    }

    @Test
    fun dropsBlanksAndTrimsOnTheWayIn() {
        assertEquals(listOf("Insight"), Tags.decode(Tags.encode(listOf("  Insight  ", "", "   "))))
    }

    @Test
    fun anEmptyListIsStoredAsAnEmptyStringAndReadBackAsAnEmptyList() {
        assertEquals("", Tags.encode(emptyList()))
        assertTrue(Tags.decode("").isEmpty())
    }

    @Test
    fun readsASingleTagWrittenBeforeTheSeparatorExisted() {
        // Migration 4 to 5 only rewrites the separator, so a lone tag is unchanged by it.
        assertEquals(listOf("Insight"), Tags.decode("Insight"))
    }
}
