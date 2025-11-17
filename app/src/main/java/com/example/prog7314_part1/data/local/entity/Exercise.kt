package com.example.prog7314_part1.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Exercise entity for Room Database
 * Individual exercises within workouts
 */
@Entity(
    tableName = "exercises",
    foreignKeys = [ForeignKey(
        entity = Workout::class,
        parentColumns = ["workoutId"],
        childColumns = ["workoutId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("workoutId")]
)
data class Exercise(
    @PrimaryKey
    val exerciseId: String = UUID.randomUUID().toString(),
    
    val workoutId: String,
    val orderIndex: Int,  // Position in workout
    
    val name: String,
    val description: String,
    val muscleGroup: String,  // "Upper Body", "Core", "Legs", etc.
    
    // Sets and Reps
    val sets: Int? = null,
    val reps: Int? = null,
    val durationSeconds: Int? = null,  // For time-based exercises
    val restSeconds: Int = 60,
    
    val videoUrl: String? = null,
    val imageUrl: String? = null,
    
    val createdAt: Long = System.currentTimeMillis()
)
