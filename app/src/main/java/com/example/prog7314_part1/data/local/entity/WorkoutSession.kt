package com.example.prog7314_part1.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * WorkoutSession entity for Room Database
 * Tracks individual workout session instances
 */
@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Workout::class,
            parentColumns = ["workoutId"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("userId"), Index("workoutId"), Index("startTime")]
)
data class WorkoutSession(
    @PrimaryKey
    val sessionId: String = UUID.randomUUID().toString(),
    
    val userId: String,
    val workoutId: String? = null,  // Null if free-form session
    val workoutName: String,
    
    // Session Details
    val startTime: Long,
    val endTime: Long? = null,
    val durationSeconds: Int = 0,
    
    // Metrics
    val caloriesBurned: Int = 0,
    val distanceKm: Double = 0.0,
    val avgHeartRate: Int = 0,
    val maxHeartRate: Int = 0,
    val avgPace: Double = 0.0,  // min/km
    
    // GPS Data (if applicable)
    val routeData: String? = null,  // JSON string of lat/lng points
    
    // Status
    val status: SessionStatus = SessionStatus.PLANNED,
    
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

enum class SessionStatus {
    PLANNED,
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    CANCELLED
}
