package com.example.prog7314_part1.data.network

import com.example.prog7314_part1.data.network.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * User API Service
 */
interface UserApiService {
    
    @POST("api/users/register")
    suspend fun register(@Body request: RegisterRequest): Response<UserResponse>
    
    @POST("api/users/login")
    suspend fun login(): Response<UserResponse>
    
    @GET("api/users/{userId}")
    suspend fun getUser(@Path("userId") userId: String): Response<UserResponse>
    
    @PUT("api/users/{userId}")
    suspend fun updateUser(
        @Path("userId") userId: String,
        @Body updates: UpdateUserRequest
    ): Response<UserResponse>
    
    @DELETE("api/users/{userId}")
    suspend fun deleteUser(@Path("userId") userId: String): Response<ApiResponse<Unit>>
}

/**
 * Workout API Service
 */
interface WorkoutApiService {
    
    @GET("api/workouts")
    suspend fun getWorkouts(
        @Query("limit") limit: Int = 50,
        @Query("status") status: String? = null
    ): Response<WorkoutsResponse>
    
    @GET("api/workouts/{workoutId}")
    suspend fun getWorkout(@Path("workoutId") workoutId: String): Response<WorkoutResponse>
    
    @POST("api/workouts")
    suspend fun createWorkout(@Body workout: Map<String, Any>): Response<WorkoutResponse>
    
    @PUT("api/workouts/{workoutId}")
    suspend fun updateWorkout(
        @Path("workoutId") workoutId: String,
        @Body updates: Map<String, Any>
    ): Response<WorkoutResponse>
    
    @DELETE("api/workouts/{workoutId}")
    suspend fun deleteWorkout(@Path("workoutId") workoutId: String): Response<ApiResponse<Unit>>
    
    @GET("api/workouts/stats/summary")
    suspend fun getWorkoutStats(
        @Query("startDate") startDate: Long? = null,
        @Query("endDate") endDate: Long? = null
    ): Response<Map<String, Any>>
}

/**
 * Goals API Service
 */
interface GoalsApiService {
    
    @GET("api/goals/{userId}")
    suspend fun getGoals(@Path("userId") userId: String): Response<GoalsResponse>
    
    @GET("api/goals/user/{userId}/active")
    suspend fun getActiveGoals(@Path("userId") userId: String): Response<GoalsResponse>
    
    @POST("api/goals")
    suspend fun createGoal(@Body goal: Map<String, Any>): Response<GoalResponse>
    
    @PUT("api/goals/{goalId}")
    suspend fun updateGoal(
        @Path("goalId") goalId: String,
        @Body updates: Map<String, Any>
    ): Response<GoalResponse>
    
    @PUT("api/goals/{goalId}/progress")
    suspend fun updateGoalProgress(
        @Path("goalId") goalId: String,
        @Body progress: Map<String, Any>
    ): Response<GoalResponse>
    
    @DELETE("api/goals/{goalId}")
    suspend fun deleteGoal(@Path("goalId") goalId: String): Response<ApiResponse<Unit>>
}

/**
 * Progress API Service
 */
interface ProgressApiService {
    
    @GET("api/progress/{userId}")
    suspend fun getProgress(
        @Path("userId") userId: String,
        @Query("startDate") startDate: Long? = null,
        @Query("endDate") endDate: Long? = null,
        @Query("limit") limit: Int = 30
    ): Response<ProgressListResponse>
    
    @GET("api/progress/{userId}/latest")
    suspend fun getLatestProgress(@Path("userId") userId: String): Response<ProgressResponse>
    
    @GET("api/progress/{userId}/summary")
    suspend fun getProgressSummary(@Path("userId") userId: String): Response<Map<String, Any>>
    
    @POST("api/progress")
    suspend fun createProgress(@Body progress: Map<String, Any>): Response<ProgressResponse>
    
    @PUT("api/progress/{progressId}")
    suspend fun updateProgress(
        @Path("progressId") progressId: String,
        @Body updates: Map<String, Any>
    ): Response<ProgressResponse>
    
    @DELETE("api/progress/{progressId}")
    suspend fun deleteProgress(@Path("progressId") progressId: String): Response<ApiResponse<Unit>>
}

/**
 * Nutrition API Service
 */
interface NutritionApiService {
    
    @GET("api/nutrition/{userId}")
    suspend fun getNutrition(
        @Path("userId") userId: String,
        @Query("date") date: String? = null,
        @Query("mealType") mealType: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<NutritionListResponse>
    
    @GET("api/nutrition/{userId}/daily/{date}")
    suspend fun getDailyNutrition(
        @Path("userId") userId: String,
        @Path("date") date: String
    ): Response<Map<String, Any>>
    
    @POST("api/nutrition")
    suspend fun createNutrition(@Body nutrition: Map<String, Any>): Response<NutritionResponse>
    
    @PUT("api/nutrition/{nutritionId}")
    suspend fun updateNutrition(
        @Path("nutritionId") nutritionId: String,
        @Body updates: Map<String, Any>
    ): Response<NutritionResponse>
    
    @DELETE("api/nutrition/{nutritionId}")
    suspend fun deleteNutrition(@Path("nutritionId") nutritionId: String): Response<ApiResponse<Unit>>
}

