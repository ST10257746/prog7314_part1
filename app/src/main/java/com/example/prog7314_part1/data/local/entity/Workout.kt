package com.example.prog7314_part1.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Workout entity for Room Database
 * Represents workout templates (pre-defined or custom)
 */
@Entity(tableName = "workouts")
data class Workout(
    @PrimaryKey
    val workoutId: String = UUID.randomUUID().toString(),
    
    val name: String,
    val description: String,
    val category: WorkoutCategory,
    val difficulty: WorkoutDifficulty,
    
    val durationMinutes: Int,
    val estimatedCalories: Int,
    val exerciseCount: Int,
    
    val rating: Double = 0.0,
    val thumbnailUrl: String? = null,
    
    val isCustom: Boolean = false,  // User-created vs. pre-defined
    val createdBy: String? = null,  // userId if custom
    
    val createdAt: Long = System.currentTimeMillis()
)

enum class WorkoutCategory {
    CARDIO,
    STRENGTH,
    YOGA,
    HIIT,
    FLEXIBILITY,
    FULL_BODY,
    UPPER_BODY,
    LOWER_BODY,
    CORE
}

enum class WorkoutDifficulty {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED
}
