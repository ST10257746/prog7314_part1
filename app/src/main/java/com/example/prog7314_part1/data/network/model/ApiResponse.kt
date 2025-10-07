package com.example.prog7314_part1.data.network.model

import com.google.gson.annotations.SerializedName

/**
 * Generic API Response wrapper
 */
data class ApiResponse<T>(
    @SerializedName("message")
    val message: String? = null,
    
    @SerializedName("data")
    val data: T? = null,
    
    @SerializedName("error")
    val error: String? = null
)

/**
 * User API request models
 */
data class RegisterRequest(
    @SerializedName("email")
    val email: String,
    
    @SerializedName("password")
    val password: String,
    
    @SerializedName("displayName")
    val displayName: String,
    
    @SerializedName("age")
    val age: Int,
    
    @SerializedName("weightKg")
    val weightKg: Double
)

data class UpdateUserRequest(
    @SerializedName("displayName")
    val displayName: String? = null,
    
    @SerializedName("age")
    val age: Int? = null,
    
    @SerializedName("weightKg")
    val weightKg: Double? = null,
    
    @SerializedName("heightCm")
    val heightCm: Double? = null,
    
    @SerializedName("profileImageUrl")
    val profileImageUrl: String? = null,
    
    @SerializedName("dailyStepGoal")
    val dailyStepGoal: Int? = null,
    
    @SerializedName("dailyCalorieGoal")
    val dailyCalorieGoal: Int? = null,
    
    @SerializedName("dailyWaterGoal")
    val dailyWaterGoal: Int? = null,
    
    @SerializedName("weeklyWorkoutGoal")
    val weeklyWorkoutGoal: Int? = null,
    
    @SerializedName("proteinGoalG")
    val proteinGoalG: Int? = null,
    
    @SerializedName("carbsGoalG")
    val carbsGoalG: Int? = null,
    
    @SerializedName("fatsGoalG")
    val fatsGoalG: Int? = null
)

/**
 * User API responses
 */
data class UserResponse(
    @SerializedName("message")
    val message: String,
    
    @SerializedName("user")
    val user: UserDto
)

data class UserDto(
    @SerializedName("userId")
    val userId: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("displayName")
    val displayName: String,
    
    @SerializedName("age")
    val age: Int,
    
    @SerializedName("weightKg")
    val weightKg: Double,
    
    @SerializedName("heightCm")
    val heightCm: Double? = null,
    
    @SerializedName("profileImageUrl")
    val profileImageUrl: String? = null,
    
    @SerializedName("dailyStepGoal")
    val dailyStepGoal: Int? = null,
    
    @SerializedName("dailyCalorieGoal")
    val dailyCalorieGoal: Int? = null,
    
    @SerializedName("dailyWaterGoal")
    val dailyWaterGoal: Int? = null,
    
    @SerializedName("weeklyWorkoutGoal")
    val weeklyWorkoutGoal: Int? = null,
    
    @SerializedName("proteinGoalG")
    val proteinGoalG: Int? = null,
    
    @SerializedName("carbsGoalG")
    val carbsGoalG: Int? = null,
    
    @SerializedName("fatsGoalG")
    val fatsGoalG: Int? = null,
    
    @SerializedName("createdAt")
    val createdAt: Long,
    
    @SerializedName("updatedAt")
    val updatedAt: Long
)

/**
 * Workout API responses
 */
data class WorkoutsResponse(
    @SerializedName("count")
    val count: Int,
    
    @SerializedName("workouts")
    val workouts: List<WorkoutDto>
)

data class WorkoutResponse(
    @SerializedName("message")
    val message: String,
    
    @SerializedName("workout")
    val workout: WorkoutDto
)

data class WorkoutDto(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("userId")
    val userId: String,
    
    @SerializedName("workoutName")
    val workoutName: String,
    
    @SerializedName("workoutType")
    val workoutType: String? = null,
    
    @SerializedName("startTime")
    val startTime: Long,
    
    @SerializedName("endTime")
    val endTime: Long? = null,
    
    @SerializedName("durationSeconds")
    val durationSeconds: Int,
    
    @SerializedName("caloriesBurned")
    val caloriesBurned: Int,
    
    @SerializedName("distanceKm")
    val distanceKm: Double,
    
    @SerializedName("notes")
    val notes: String? = null,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("createdAt")
    val createdAt: Long,
    
    @SerializedName("isSynced")
    val isSynced: Boolean = true
)

/**
 * Goals API responses
 */
data class GoalsResponse(
    @SerializedName("count")
    val count: Int,
    
    @SerializedName("goals")
    val goals: List<GoalDto>
)

data class GoalResponse(
    @SerializedName("message")
    val message: String,
    
    @SerializedName("goal")
    val goal: GoalDto
)

data class GoalDto(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("userId")
    val userId: String,
    
    @SerializedName("goalType")
    val goalType: String,
    
    @SerializedName("targetValue")
    val targetValue: Double,
    
    @SerializedName("currentValue")
    val currentValue: Double,
    
    @SerializedName("startDate")
    val startDate: Long,
    
    @SerializedName("targetDate")
    val targetDate: Long? = null,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("isActive")
    val isActive: Boolean,
    
    @SerializedName("isCompleted")
    val isCompleted: Boolean,
    
    @SerializedName("createdAt")
    val createdAt: Long,
    
    @SerializedName("updatedAt")
    val updatedAt: Long
)

/**
 * Progress API responses
 */
data class ProgressListResponse(
    @SerializedName("count")
    val count: Int,
    
    @SerializedName("progress")
    val progress: List<ProgressDto>
)

data class ProgressResponse(
    @SerializedName("message")
    val message: String,
    
    @SerializedName("progress")
    val progress: ProgressDto
)

data class ProgressDto(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("userId")
    val userId: String,
    
    @SerializedName("date")
    val date: Long,
    
    @SerializedName("weightKg")
    val weightKg: Double? = null,
    
    @SerializedName("bmi")
    val bmi: Double? = null,
    
    @SerializedName("bodyFatPercentage")
    val bodyFatPercentage: Double? = null,
    
    @SerializedName("muscleMassKg")
    val muscleMassKg: Double? = null,
    
    @SerializedName("notes")
    val notes: String,
    
    @SerializedName("createdAt")
    val createdAt: Long
)

/**
 * Nutrition API responses
 */
data class NutritionListResponse(
    @SerializedName("count")
    val count: Int,
    
    @SerializedName("nutrition")
    val nutrition: List<NutritionDto>
)

data class NutritionResponse(
    @SerializedName("message")
    val message: String,
    
    @SerializedName("nutrition")
    val nutrition: NutritionDto
)

data class NutritionDto(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("userId")
    val userId: String,
    
    @SerializedName("foodName")
    val foodName: String,
    
    @SerializedName("mealType")
    val mealType: String,
    
    @SerializedName("servingSize")
    val servingSize: String,
    
    @SerializedName("calories")
    val calories: Int,
    
    @SerializedName("proteinG")
    val proteinG: Double,
    
    @SerializedName("carbsG")
    val carbsG: Double,
    
    @SerializedName("fatsG")
    val fatsG: Double,
    
    @SerializedName("fiberG")
    val fiberG: Double,
    
    @SerializedName("sugarG")
    val sugarG: Double,
    
    @SerializedName("notes")
    val notes: String,
    
    @SerializedName("timestamp")
    val timestamp: Long,
    
    @SerializedName("createdAt")
    val createdAt: Long
)

