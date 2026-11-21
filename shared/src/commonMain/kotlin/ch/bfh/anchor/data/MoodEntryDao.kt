package ch.bfh.anchor.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ch.bfh.anchor.model.MoodEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodEntryDao {
    @Query("SELECT * FROM mood_entries ORDER BY recordedAt DESC")
    fun getAll(): Flow<List<MoodEntry>>

    /** Returns -1 when the prompt was already saved. A second tap cannot create a second row. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: MoodEntry): Long
}
