package com.atemtrainer.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY date DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY date DESC LIMIT 1")
    suspend fun getLatestSession(): SessionEntity?

    @Query("SELECT MAX(targetSeconds) FROM sessions WHERE completed = 1")
    fun getMaxDuration(): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()
}
