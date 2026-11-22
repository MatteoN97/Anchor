package ch.bfh.anchor.data

import androidx.room.*
import ch.bfh.anchor.model.CopingTool
import kotlinx.coroutines.flow.Flow

@Dao
interface CopingToolDao {
    // Inactive tools are deliberately excluded from Home and crisis suggestions.
    @Query("SELECT * FROM coping_tools WHERE isActive = 1 ORDER BY name COLLATE NOCASE")
    fun getActive(): Flow<List<CopingTool>>

    @Query("SELECT * FROM coping_tools WHERE isActive = 0 ORDER BY name COLLATE NOCASE")
    fun getInactive(): Flow<List<CopingTool>>

    @Insert
    suspend fun insert(tool: CopingTool): Long

    // Guided activities have a stable ID; ignore a duplicate if a save is tapped while data loads.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(tool: CopingTool): Long

    @Delete
    suspend fun delete(tool: CopingTool): Int
    @Update
    suspend fun update(tool: CopingTool)
}
