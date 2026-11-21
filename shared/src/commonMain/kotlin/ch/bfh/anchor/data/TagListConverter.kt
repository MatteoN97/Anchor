package ch.bfh.anchor.data

import androidx.room.TypeConverter
import ch.bfh.anchor.model.Tags

/** Maps [ch.bfh.anchor.model.TherapyNote.tags] to the single TEXT column it is stored in. */
class TagListConverter {
    @TypeConverter
    fun fromTagList(tags: List<String>): String = Tags.encode(tags)

    @TypeConverter
    fun toTagList(stored: String): List<String> = Tags.decode(stored)
}
