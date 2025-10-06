package com.example.prog7314_part1.data.local.dao

import androidx.room.*
import com.example.prog7314_part1.data.local.entity.Goal
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Goal entity
 */
@Dao
interface GoalDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: List<Goal>)
    
    @Update
    suspend fun updateGoal(goal: Goal)
    
    @Delete
    suspend fun deleteGoal(goal: Goal)
    
    @Query("SELECT * FROM goals WHERE goalId = :goalId")
    suspend fun getGoalById(goalId: Long): Goal?
    
    @Query("SELECT * FROM goals WHERE userId = :userId AND date = :date ORDER BY createdAt ASC")
    fun getGoalsForDate(userId: String, date: String): Flow<List<Goal>>
    
    @Query("SELECT * FROM goals WHERE userId = :userId AND date = :date ORDER BY createdAt ASC")
    suspend fun getGoalsForDateSuspend(userId: String, date: String): List<Goal>
    
    @Query("SELECT * FROM goals WHERE userId = :userId AND date = :date AND isCompleted = 0 ORDER BY createdAt ASC")
    fun getIncompleteGoalsForDate(userId: String, date: String): Flow<List<Goal>>
    
    @Query("SELECT * FROM goals WHERE userId = :userId AND date = :date AND isCompleted = 1 ORDER BY completedAt DESC")
    fun getCompletedGoalsForDate(userId: String, date: String): Flow<List<Goal>>
    
    @Query("SELECT * FROM goals WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC, createdAt ASC")
    fun getGoalsInRange(userId: String, startDate: String, endDate: String): Flow<List<Goal>>
    
    @Query("SELECT COUNT(*) FROM goals WHERE userId = :userId AND date = :date AND isCompleted = 1")
    suspend fun getCompletedGoalCountForDate(userId: String, date: String): Int
    
    @Query("SELECT COUNT(*) FROM goals WHERE userId = :userId AND date = :date")
    suspend fun getTotalGoalCountForDate(userId: String, date: String): Int
    
    @Query("SELECT * FROM goals WHERE userId = :userId AND isSynced = 0")
    suspend fun getUnsyncedGoals(userId: String): List<Goal>
    
    @Query("UPDATE goals SET isSynced = 1 WHERE goalId = :goalId")
    suspend fun markAsSynced(goalId: Long)
    
    @Query("DELETE FROM goals WHERE userId = :userId AND date = :date")
    suspend fun deleteGoalsForDate(userId: String, date: String)
    
    @Query("DELETE FROM goals WHERE userId = :userId")
    suspend fun deleteAllGoalsForUser(userId: String)
}
