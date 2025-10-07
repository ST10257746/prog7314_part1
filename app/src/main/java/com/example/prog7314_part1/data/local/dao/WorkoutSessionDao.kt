package com.example.prog7314_part1.data.local.dao

import androidx.room.*
import com.example.prog7314_part1.data.local.entity.WorkoutSession
import com.example.prog7314_part1.data.local.entity.SessionStatus
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for WorkoutSession entity
 */
@Dao
interface WorkoutSessionDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSession)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<WorkoutSession>)
    
    @Update
    suspend fun updateSession(session: WorkoutSession)
    
    @Delete
    suspend fun deleteSession(session: WorkoutSession)
    
    @Query("SELECT * FROM workout_sessions WHERE sessionId = :sessionId")
    suspend fun getSessionById(sessionId: String): WorkoutSession?
    
    @Query("SELECT * FROM workout_sessions WHERE sessionId = :sessionId")
    fun getSessionByIdFlow(sessionId: String): Flow<WorkoutSession?>
    
    @Query("SELECT * FROM workout_sessions WHERE userId = :userId ORDER BY startTime DESC")
    fun getAllSessions(userId: String): Flow<List<WorkoutSession>>
    
    @Query("SELECT * FROM workout_sessions WHERE userId = :userId ORDER BY startTime DESC LIMIT :limit")
    fun getRecentSessions(userId: String, limit: Int = 10): Flow<List<WorkoutSession>>
    
    @Query("SELECT * FROM workout_sessions WHERE userId = :userId AND status = :status ORDER BY startTime DESC")
    suspend fun getSessionsByStatus(userId: String, status: SessionStatus): List<WorkoutSession>
    
    @Query("SELECT * FROM workout_sessions WHERE userId = :userId AND status = :status ORDER BY startTime DESC")
    fun getSessionsByStatusFlow(userId: String, status: SessionStatus): Flow<List<WorkoutSession>>
    
    @Query("SELECT * FROM workout_sessions WHERE userId = :userId AND startTime BETWEEN :startTime AND :endTime ORDER BY startTime DESC")
    fun getSessionsInTimeRange(userId: String, startTime: Long, endTime: Long): Flow<List<WorkoutSession>>
    
    @Query("SELECT * FROM workout_sessions WHERE userId = :userId AND isSynced = 0")
    suspend fun getUnsyncedSessions(userId: String): List<WorkoutSession>
    
    @Query("UPDATE workout_sessions SET isSynced = 1 WHERE sessionId = :sessionId")
    suspend fun markAsSynced(sessionId: String)
    
    @Query("SELECT COUNT(*) FROM workout_sessions WHERE userId = :userId AND status = 'COMPLETED'")
    suspend fun getCompletedSessionCount(userId: String): Int
    
    @Query("SELECT SUM(caloriesBurned) FROM workout_sessions WHERE userId = :userId AND status = 'COMPLETED'")
    suspend fun getTotalCaloriesBurned(userId: String): Int?
    
    @Query("DELETE FROM workout_sessions WHERE userId = :userId")
    suspend fun deleteAllSessionsForUser(userId: String)
}
