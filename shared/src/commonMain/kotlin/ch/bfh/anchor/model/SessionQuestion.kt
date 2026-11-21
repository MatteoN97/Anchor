package ch.bfh.anchor.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import ch.bfh.anchor.platform.nowMillis
import ch.bfh.anchor.platform.randomId

@Entity(tableName = "session_questions")
data class SessionQuestion(
    @PrimaryKey val id: String = randomId(),
    val text: String,
    val date: Long = nowMillis(),
    val answered: Boolean = false
)
