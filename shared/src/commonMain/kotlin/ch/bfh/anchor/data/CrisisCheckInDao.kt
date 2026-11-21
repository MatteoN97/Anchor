package ch.bfh.anchor.data

import androidx.room.*
import ch.bfh.anchor.model.CrisisCheckIn
import kotlinx.coroutines.flow.Flow

@Dao
interface CrisisCheckInDao {
    @Query("SELECT * FROM crisis_check_ins ORDER BY date DESC")
    fun getAll(): Flow<List<CrisisCheckIn>>

    @Insert
    suspend fun insert(checkIn: CrisisCheckIn)
}
