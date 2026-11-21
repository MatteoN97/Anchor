package ch.bfh.anchor.data

import androidx.room.*
import ch.bfh.anchor.model.CopingTool
import kotlinx.coroutines.flow.Flow

@Dao
interface CopingToolDao {
    @Query("SELECT * FROM coping_tools")
    fun getAll(): Flow<List<CopingTool>>

    @Insert
    suspend fun insert(tool: CopingTool): Long

    @Delete
    suspend fun delete(tool: CopingTool): Int
    @Update
    suspend fun update(tool: CopingTool)
}
