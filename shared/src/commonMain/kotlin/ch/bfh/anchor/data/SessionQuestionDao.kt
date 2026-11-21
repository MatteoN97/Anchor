package ch.bfh.anchor.data

import androidx.room.*
import ch.bfh.anchor.model.SessionQuestion
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionQuestionDao {
    @Query("SELECT * FROM session_questions ORDER BY answered ASC, date DESC")
    fun getAll(): Flow<List<SessionQuestion>>

    @Insert
    suspend fun insert(question: SessionQuestion)

    @Update
    suspend fun update(question: SessionQuestion)

    @Delete
    suspend fun delete(question: SessionQuestion)
}
