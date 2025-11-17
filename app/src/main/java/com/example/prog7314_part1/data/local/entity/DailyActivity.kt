package com.example.prog7314_part1.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * DailyActivity entity for Room Database
 * Tracks daily fitness metrics per user
 */
@Entity(
    tableName = "daily_activities",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["userId"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userId"), Index("date")]
)
data class DailyActivity(
    @PrimaryKey(autoGenerate = true)
    val activityId: Long = 0,
    
    val userId: String,
    val date: String,  // "yyyy-MM-dd" format
    
    // Activity Metrics
    val steps: Int = 0,
    val distanceKm: Double = 0.0,
    val activeMinutes: Int = 0,
    val caloriesBurned: Int = 0,
    val heartPoints: Int = 0,
    
    // Hydration
    val waterGlasses: Int = 0,
    
    // Sleep (optional)
    val sleepHours: Double = 0.0,
    
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
