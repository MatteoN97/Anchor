package ch.bfh.anchor.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import ch.bfh.anchor.platform.nowMillis
import ch.bfh.anchor.platform.randomId

@Entity(tableName = "crisis_check_ins")
data class CrisisCheckIn(
    @PrimaryKey val id: String = randomId(),
    val date: Long = nowMillis(),
    val feeling: String,       // Feeling.name()
    val feelingAfter: Int      // 1 (Great) – 5 (Not great)
)
