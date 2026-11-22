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

    /** The most recent entry from [source], or null. */
    @Query("SELECT * FROM mood_entries WHERE source = :source ORDER BY recordedAt DESC LIMIT 1")
    suspend fun latest(source: String): MoodEntry?

    @Query("UPDATE mood_entries SET score = :score WHERE id = :id")
    suspend fun updateScore(id: String, score: Int)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entries: List<MoodEntry>)

    @Query("DELETE FROM mood_entries WHERE source = :source")
    suspend fun deleteAllFrom(source: String)
}
