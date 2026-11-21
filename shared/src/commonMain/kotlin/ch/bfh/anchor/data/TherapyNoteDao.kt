package ch.bfh.anchor.data

import androidx.room.*
import ch.bfh.anchor.model.TherapyNote
import kotlinx.coroutines.flow.Flow

@Dao
interface TherapyNoteDao {
    @Query("SELECT * FROM therapy_notes ORDER BY date DESC")
    fun getAll(): Flow<List<TherapyNote>>

    @Insert
    suspend fun insert(note: TherapyNote)

    @Update
    suspend fun update(note: TherapyNote)

    @Delete
    suspend fun delete(note: TherapyNote)
}
