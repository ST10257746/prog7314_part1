package com.example.prog7314_part1.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User entity for Room Database
 * Stores local cache of user profile data
 */
@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val userId: String,  // Firebase UID
    
    // Basic Info
    val email: String,
    val displayName: String,
    val age: Int,
    val weightKg: Double,
    val heightCm: Double? = null,  // Optional for BMI calculation
    val profileImageUrl: String? = null,
    
    // Daily Goals (nullable until user sets them)
    val dailyStepGoal: Int? = null,
    val dailyCalorieGoal: Int? = null,
    val dailyWaterGoal: Int? = null,  // glasses
    
    // Weekly Goals
    val weeklyWorkoutGoal: Int? = null,
    
    // Nutrition Goals
    val proteinGoalG: Int? = null,
    val carbsGoalG: Int? = null,
    val fatsGoalG: Int? = null,
    
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long = 0
)
