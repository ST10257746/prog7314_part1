package com.example.prog7314_part1.data.repository

import com.example.prog7314_part1.data.local.dao.DailyActivityDao
import com.example.prog7314_part1.data.local.entity.DailyActivity
import com.example.prog7314_part1.data.model.Result
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository for managing daily activity data including water intake, steps, etc.
 */
class DailyActivityRepository(
    private val dailyActivityDao: DailyActivityDao
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    /**
     * Get today's date in yyyy-MM-dd format
     */
    private fun getTodayDate(): String {
        return dateFormat.format(Date())
    }
    
    /**
     * Get daily activity for today
     */
    fun getTodayActivity(userId: String): Flow<DailyActivity?> {
        return dailyActivityDao.getActivityByDateFlow(userId, getTodayDate())
    }
    
    /**
     * Get daily activity for a specific date
     */
    fun getActivityForDate(userId: String, date: String): Flow<DailyActivity?> {
        return dailyActivityDao.getActivityByDateFlow(userId, date)
    }
    
    /**
     * Get daily activity for a specific date (suspend function)
     */
    suspend fun getActivityForDateSuspend(userId: String, date: String): DailyActivity? {
        return dailyActivityDao.getActivityByDate(userId, date)
    }
    
    /**
     * Add water glasses to today's activity
     */
    suspend fun addWaterGlasses(userId: String, glasses: Int): Result<Unit> {
        return try {
            val today = getTodayDate()
            val existingActivity = dailyActivityDao.getActivityByDate(userId, today)
            
            if (existingActivity != null) {
                // Update existing activity
                val updatedActivity = existingActivity.copy(
                    waterGlasses = existingActivity.waterGlasses + glasses,
                    updatedAt = System.currentTimeMillis()
                )
                dailyActivityDao.updateActivity(updatedActivity)
            } else {
                // Create new activity
                val newActivity = DailyActivity(
                    userId = userId,
                    date = today,
                    waterGlasses = glasses,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                dailyActivityDao.insertActivity(newActivity)
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to update water intake: ${e.message}")
        }
    }
    
    /**
     * Set water glasses for today's activity
     */
    suspend fun setWaterGlasses(userId: String, glasses: Int): Result<Unit> {
        return try {
            val today = getTodayDate()
            val existingActivity = dailyActivityDao.getActivityByDate(userId, today)
            
            if (existingActivity != null) {
                // Update existing activity
                val updatedActivity = existingActivity.copy(
                    waterGlasses = glasses,
                    updatedAt = System.currentTimeMillis()
                )
                dailyActivityDao.updateActivity(updatedActivity)
            } else {
                // Create new activity
                val newActivity = DailyActivity(
                    userId = userId,
                    date = today,
                    waterGlasses = glasses,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                dailyActivityDao.insertActivity(newActivity)
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to update water intake: ${e.message}")
        }
    }
    
    /**
     * Get current water intake for today
     */
    suspend fun getTodayWaterIntake(userId: String): Int {
        val today = getTodayDate()
        return dailyActivityDao.getActivityByDate(userId, today)?.waterGlasses ?: 0
    }
    
    /**
     * Get activities for a date range
     */
    fun getActivitiesInRange(userId: String, startDate: String, endDate: String): Flow<List<DailyActivity>> {
        return dailyActivityDao.getActivitiesInRange(userId, startDate, endDate)
    }
    
    /**
     * Get recent activities
     */
    fun getRecentActivities(userId: String, limit: Int = 30): Flow<List<DailyActivity>> {
        return dailyActivityDao.getRecentActivities(userId, limit)
    }
    
    /**
     * Update daily activity with all metrics
     */
    suspend fun updateDailyActivity(
        userId: String,
        steps: Int? = null,
        distanceKm: Double? = null,
        activeMinutes: Int? = null,
        caloriesBurned: Int? = null,
        heartPoints: Int? = null,
        waterGlasses: Int? = null,
        sleepHours: Double? = null
    ): Result<Unit> {
        return try {
            val today = getTodayDate()
            val existingActivity = dailyActivityDao.getActivityByDate(userId, today)
            
            val updatedActivity = if (existingActivity != null) {
                existingActivity.copy(
                    steps = steps ?: existingActivity.steps,
                    distanceKm = distanceKm ?: existingActivity.distanceKm,
                    activeMinutes = activeMinutes ?: existingActivity.activeMinutes,
                    caloriesBurned = caloriesBurned ?: existingActivity.caloriesBurned,
                    heartPoints = heartPoints ?: existingActivity.heartPoints,
                    waterGlasses = waterGlasses ?: existingActivity.waterGlasses,
                    sleepHours = sleepHours ?: existingActivity.sleepHours,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                DailyActivity(
                    userId = userId,
                    date = today,
                    steps = steps ?: 0,
                    distanceKm = distanceKm ?: 0.0,
                    activeMinutes = activeMinutes ?: 0,
                    caloriesBurned = caloriesBurned ?: 0,
                    heartPoints = heartPoints ?: 0,
                    waterGlasses = waterGlasses ?: 0,
                    sleepHours = sleepHours ?: 0.0,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            }
            
            dailyActivityDao.insertActivity(updatedActivity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to update daily activity: ${e.message}")
        }
    }
}
