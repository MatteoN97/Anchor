package ch.bfh.anchor.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import ch.bfh.anchor.platform.nowMillis
import ch.bfh.anchor.platform.randomId

@Entity(tableName = "therapy_notes")
data class TherapyNote(
    @PrimaryKey val id: String = randomId(),
    val date: Long = nowMillis(),
    val body: String,
    val tags: List<String> = emptyList(),
    val flaggedForNextSession: Boolean = false
)
