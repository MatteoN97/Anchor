package ch.bfh.anchor.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ch.bfh.anchor.platform.randomId

@Entity(tableName = "coping_tools")
data class CopingTool(
    @PrimaryKey val id: String = randomId(),
    val name: String,
    val category: String,
    val description: String,
    @ColumnInfo(defaultValue = "1") val isActive: Boolean = true
)
