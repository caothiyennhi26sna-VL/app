package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ThinkingDao {
    @Query("SELECT * FROM thinking_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<ThinkingSessionEntity>>

    @Query("SELECT * FROM thinking_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): ThinkingSessionEntity?

    @Query("SELECT * FROM thinking_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: Long): Flow<List<ThinkingMessageEntity>>

    @Query("SELECT * FROM thinking_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesListForSession(sessionId: Long): List<ThinkingMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ThinkingSessionEntity): Long

    @Update
    suspend fun updateSession(session: ThinkingSessionEntity)

    @Delete
    suspend fun deleteSession(session: ThinkingSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ThinkingMessageEntity): Long

    @Query("SELECT COUNT(*) FROM thinking_sessions")
    fun getTotalSessionsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM thinking_sessions WHERE status = 'CONCLUDED'")
    fun getConcludedSessionsCount(): Flow<Int>
}
