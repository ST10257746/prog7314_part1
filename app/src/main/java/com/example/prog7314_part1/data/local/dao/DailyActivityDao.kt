package com.example.prog7314_part1.data.local.dao

import androidx.room.*
import com.example.prog7314_part1.data.local.entity.DailyActivity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for DailyActivity entity
 */
@Dao
interface DailyActivityDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: DailyActivity)
    
    @Update
    suspend fun updateActivity(activity: DailyActivity)
    
    @Delete
    suspend fun deleteActivity(activity: DailyActivity)
    
    @Query("SELECT * FROM daily_activities WHERE userId = :userId AND date = :date")
    suspend fun getActivityByDate(userId: String, date: String): DailyActivity?
    
    @Query("SELECT * FROM daily_activities WHERE userId = :userId AND date = :date")
    fun getActivityByDateFlow(userId: String, date: String): Flow<DailyActivity?>
    
    @Query("SELECT * FROM daily_activities WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getActivitiesInRange(userId: String, startDate: String, endDate: String): Flow<List<DailyActivity>>
    
    @Query("SELECT * FROM daily_activities WHERE userId = :userId ORDER BY date DESC LIMIT :limit")
    fun getRecentActivities(userId: String, limit: Int = 30): Flow<List<DailyActivity>>
    
    @Query("SELECT * FROM daily_activities WHERE userId = :userId AND isSynced = 0")
    suspend fun getUnsyncedActivities(userId: String): List<DailyActivity>
    
    @Query("UPDATE daily_activities SET isSynced = 1 WHERE activityId = :activityId")
    suspend fun markAsSynced(activityId: Long)
    
    @Query("DELETE FROM daily_activities WHERE userId = :userId")
    suspend fun deleteAllActivitiesForUser(userId: String)
}
