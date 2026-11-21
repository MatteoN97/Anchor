package ch.bfh.anchor.data

import kotlin.test.Test
import kotlin.test.assertEquals

class TagListConverterTest {

    private val converter = TagListConverter()

    @Test
    fun roundTripsThroughTheColumnRepresentation() {
        val tags = listOf("Coping skill", "Discuss, next time")
        assertEquals(tags, converter.toTagList(converter.fromTagList(tags)))
    }

    @Test
    fun readsAnEmptyColumnAsNoTags() {
        assertEquals(emptyList(), converter.toTagList(""))
    }
}
