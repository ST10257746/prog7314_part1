package com.example.prog7314_part1.data.repository

import android.content.Context
import com.example.prog7314_part1.data.local.AppDatabase
import com.example.prog7314_part1.data.local.entity.WorkoutSession
import com.example.prog7314_part1.data.model.Result
import com.example.prog7314_part1.data.network.*
import com.example.prog7314_part1.data.network.model.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * NetworkRepository
 * Handles all REST API calls and manages sync between local DB and remote server
 */
class NetworkRepository(private val context: Context) {
    
    private val database = AppDatabase.getDatabase(context)
    private val auth = FirebaseAuth.getInstance()
    
    // API Services
    private val userApi: UserApiService = RetrofitClient.create()
    private val workoutApi: WorkoutApiService = RetrofitClient.create()
    private val goalsApi: GoalsApiService = RetrofitClient.create()
    private val progressApi: ProgressApiService = RetrofitClient.create()
    private val nutritionApi: NutritionApiService = RetrofitClient.create()
    private val dailyActivityApi: DailyActivityApiService = RetrofitClient.create()
    
    // ==================== User Operations ====================
    
    /**
     * Register new user via API
     */
    suspend fun registerUser(
        email: String,
        password: String,
        displayName: String,
        age: Int,
        weightKg: Double
    ): Result<UserDto> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("NetworkRepository", "📤 Preparing registration request for: $email")
            val request = RegisterRequest(
                email = email,
                password = password,
                displayName = displayName,
                age = age,
                weightKg = weightKg
            )
            android.util.Log.d("NetworkRepository", "📡 Calling API /api/users/register...")
            
            val response = userApi.register(request)
            
            android.util.Log.d("NetworkRepository", "📥 Response received - Code: ${response.code()}, Success: ${response.isSuccessful}")
            
            if (response.isSuccessful && response.body() != null) {
                android.util.Log.d("NetworkRepository", "✅ Registration successful!")
                Result.Success(response.body()!!.user)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                android.util.Log.e("NetworkRepository", "❌ Registration failed - Code: ${response.code()}, Error: $errorBody")
                Result.Error(
                    Exception("Registration failed"),
                    errorBody
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("NetworkRepository", "💥 Exception during registration: ${e.message}", e)
            Result.Error(e, e.message ?: "Network error")
        }
    }
    
    /**
     * Login user via API (fetch user data)
     */
    suspend fun loginUser(): Result<UserDto> = withContext(Dispatchers.IO) {
        try {
            val response = userApi.login()
            
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.user)
            } else {
                Result.Error(
                    Exception("Login failed"),
                    response.errorBody()?.string() ?: "Unknown error"
                )
            }
        } catch (e: Exception) {
            Result.Error(e, e.message ?: "Network error")
        }
    }
    
    /**
     * Get user profile from API
     */
    suspend fun getUserProfile(userId: String): Result<UserDto> = withContext(Dispatchers.IO) {
        try {
            val response = userApi.getUser(userId)
            
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.user)
            } else {
                Result.Error(
                    Exception("Failed to fetch user"),
                    response.errorBody()?.string() ?: "Unknown error"
                )
            }
        } catch (e: Exception) {
            Result.Error(e, e.message ?: "Network error")
        }
    }
    
    /**
     * Update user profile via API
     */
    suspend fun updateUserProfile(userId: String, updates: UpdateUserRequest): Result<UserDto> = 
        withContext(Dispatchers.IO) {
            try {
                val response = userApi.updateUser(userId, updates)
                
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!.user)
                } else {
                    Result.Error(
                        Exception("Failed to update user"),
                        response.errorBody()?.string() ?: "Unknown error"
                    )
                }
            } catch (e: Exception) {
                Result.Error(e, e.message ?: "Network error")
            }
        }
    
    // ==================== Workout Operations ====================
    
    /**
     * Sync local workouts to API
     */
    suspend fun syncWorkouts(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
            
            // Get unsynced workouts from local DB
            val unsyncedWorkouts = database.workoutSessionDao().getUnsyncedSessions(userId)
            
            // Upload each unsynced workout
            unsyncedWorkouts.forEach { workout ->
                val workoutMap = mapOf(
                    "workoutName" to workout.workoutName,
                    "startTime" to workout.startTime,
                    "endTime" to (workout.endTime ?: 0),
                    "durationSeconds" to workout.durationSeconds,
                    "caloriesBurned" to workout.caloriesBurned,
                    "distanceKm" to workout.distanceKm,
                    "status" to workout.status.name
                )
                
                val response = workoutApi.createWorkout(workoutMap)
                
                if (response.isSuccessful) {
                    // Mark as synced in local DB
                    database.workoutSessionDao().updateSession(
                        workout.copy(isSynced = true)
                    )
                }
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, e.message ?: "Sync failed")
        }
    }
    
    /**
     * Fetch workouts from API
     */
    suspend fun fetchWorkouts(limit: Int = 50): Result<List<WorkoutDto>> = 
        withContext(Dispatchers.IO) {
            try {
                val response = workoutApi.getWorkouts(limit)
                
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!.workouts)
                } else {
                    Result.Error(
                        Exception("Failed to fetch workouts"),
                        response.errorBody()?.string() ?: "Unknown error"
                    )
                }
            } catch (e: Exception) {
                Result.Error(e, e.message ?: "Network error")
            }
        }
    
    /**
     * Create workout via API
     */
    suspend fun createWorkout(workout: Map<String, Any>): Result<WorkoutDto> = 
        withContext(Dispatchers.IO) {
            try {
                val response = workoutApi.createWorkout(workout)
                
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!.workout)
                } else {
                    Result.Error(
                        Exception("Failed to create workout"),
                        response.errorBody()?.string() ?: "Unknown error"
                    )
                }
            } catch (e: Exception) {
                Result.Error(e, e.message ?: "Network error")
            }
        }
    
    /**
     * Update workout via API
     */
    suspend fun updateWorkout(workoutId: String, updates: Map<String, Any>): Result<WorkoutDto> = 
        withContext(Dispatchers.IO) {
            try {
                val response = workoutApi.updateWorkout(workoutId, updates)
                
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!.workout)
                } else {
                    Result.Error(
                        Exception("Failed to update workout"),
                        response.errorBody()?.string() ?: "Unknown error"
                    )
                }
            } catch (e: Exception) {
                Result.Error(e, e.message ?: "Network error")
            }
        }
    
    /**
     * Delete workout via API
     */
    suspend fun deleteWorkout(workoutId: String): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                val response = workoutApi.deleteWorkout(workoutId)
                
                if (response.isSuccessful) {
                    Result.Success(Unit)
                } else {
                    Result.Error(
                        Exception("Failed to delete workout"),
                        response.errorBody()?.string() ?: "Unknown error"
                    )
                }
            } catch (e: Exception) {
                Result.Error(e, e.message ?: "Network error")
            }
        }
    
    // ==================== Goals Operations ====================
    
    /**
     * Fetch goals from API
     */
    suspend fun fetchGoals(userId: String): Result<List<GoalDto>> = 
        withContext(Dispatchers.IO) {
            try {
                val response = goalsApi.getGoals(userId)
                
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!.goals)
                } else {
                    Result.Error(
                        Exception("Failed to fetch goals"),
                        response.errorBody()?.string() ?: "Unknown error"
                    )
                }
            } catch (e: Exception) {
                Result.Error(e, e.message ?: "Network error")
            }
        }
    
    /**
     * Create goal via API
     */
    suspend fun createGoal(goal: Map<String, Any>): Result<GoalDto> = 
        withContext(Dispatchers.IO) {
            try {
                val response = goalsApi.createGoal(goal)
                
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!.goal)
                } else {
                    Result.Error(
                        Exception("Failed to create goal"),
                        response.errorBody()?.string() ?: "Unknown error"
                    )
                }
            } catch (e: Exception) {
                Result.Error(e, e.message ?: "Network error")
            }
        }
    
    /**
     * Update goal progress via API
     */
    suspend fun updateGoalProgress(goalId: String, currentValue: Double): Result<GoalDto> = 
        withContext(Dispatchers.IO) {
            try {
                val updates = mapOf("currentValue" to currentValue)
                val response = goalsApi.updateGoalProgress(goalId, updates)
                
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!.goal)
                } else {
                    Result.Error(
                        Exception("Failed to update goal progress"),
                        response.errorBody()?.string() ?: "Unknown error"
                    )
                }
            } catch (e: Exception) {
                Result.Error(e, e.message ?: "Network error")
            }
        }
    
    // ==================== Progress Operations ====================
    
    /**
     * Fetch progress entries from API
     */
    suspend fun fetchProgress(userId: String, limit: Int = 30): Result<List<ProgressDto>> = 
        withContext(Dispatchers.IO) {
            try {
                val response = progressApi.getProgress(userId, limit = limit)
                
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!.progress)
                } else {
                    Result.Error(
                        Exception("Failed to fetch progress"),
                        response.errorBody()?.string() ?: "Unknown error"
                    )
                }
            } catch (e: Exception) {
                Result.Error(e, e.message ?: "Network error")
            }
        }
    
    /**
     * Create progress entry via API
     */
    suspend fun createProgress(progress: Map<String, Any>): Result<ProgressDto> = 
        withContext(Dispatchers.IO) {
            try {
                val response = progressApi.createProgress(progress)
                
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!.progress)
                } else {
                    Result.Error(
                        Exception("Failed to create progress entry"),
                        response.errorBody()?.string() ?: "Unknown error"
                    )
                }
            } catch (e: Exception) {
                Result.Error(e, e.message ?: "Network error")
            }
        }
    
    // ==================== Nutrition Operations ====================
    
    /**
     * Fetch nutrition entries from API
     */
    suspend fun fetchNutrition(
        userId: String,
        date: String? = null,
        limit: Int = 50
    ): Result<List<NutritionDto>> = withContext(Dispatchers.IO) {
        try {
            val response = nutritionApi.getNutrition(userId, date, limit = limit)
            
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.nutrition)
            } else {
                Result.Error(
                    Exception("Failed to fetch nutrition"),
                    response.errorBody()?.string() ?: "Unknown error"
                )
            }
        } catch (e: Exception) {
            Result.Error(e, e.message ?: "Network error")
        }
    }
    
    /**
     * Fetch nutrition entries from API
     */
    suspend fun getNutritionEntries(userId: String, date: String? = null): Result<List<NutritionDto>> =
        withContext(Dispatchers.IO) {
            try {
                val response = nutritionApi.getNutrition(userId, date)
                
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!.nutrition)
                } else {
                    Result.Error(
                        Exception("Failed to fetch nutrition entries"),
                        response.errorBody()?.string() ?: "Unknown error"
                    )
                }
            } catch (e: Exception) {
                Result.Error(e, e.message ?: "Network error")
            }
        }
    
    /**
     * Create nutrition entry via API
     */
    suspend fun createNutrition(nutrition: CreateNutritionRequest): Result<NutritionDto> = 
        withContext(Dispatchers.IO) {
            try {
                val response = nutritionApi.createNutrition(nutrition)
                
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!.nutrition)
                } else {
                    Result.Error(
                        Exception("Failed to create nutrition entry"),
                        response.errorBody()?.string() ?: "Unknown error"
                    )
                }
            } catch (e: Exception) {
                Result.Error(e, e.message ?: "Network error")
            }
        }
    
    // ==================== Daily Activity Operations ====================
    
    /**
     * Get daily activity from API
     */
    suspend fun getDailyActivity(userId: String, date: String): Result<DailyActivityResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = dailyActivityApi.getDailyActivity(userId, date)
                
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else {
                    Result.Error(
                        Exception("Failed to fetch daily activity"),
                        response.errorBody()?.string() ?: "Unknown error"
                    )
                }
            } catch (e: Exception) {
                Result.Error(e, e.message ?: "Network error")
            }
        }
    
    /**
     * Update daily activity via API
     */
    suspend fun updateDailyActivity(
        userId: String,
        date: String,
        steps: Int? = null,
        waterGlasses: Int? = null,
        caloriesBurned: Int? = null,
        activeMinutes: Int? = null,
        distance: Double? = null
    ): Result<DailyActivityResponse> =
        withContext(Dispatchers.IO) {
            try {
                val updates = mutableMapOf<String, Any>()
                steps?.let { updates["steps"] = it }
                waterGlasses?.let { updates["waterGlasses"] = it }
                caloriesBurned?.let { updates["caloriesBurned"] = it }
                activeMinutes?.let { updates["activeMinutes"] = it }
                distance?.let { updates["distance"] = it }
                
                val response = dailyActivityApi.updateDailyActivity(userId, date, updates)
                
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else {
                    Result.Error(
                        Exception("Failed to update daily activity"),
                        response.errorBody()?.string() ?: "Unknown error"
                    )
                }
            } catch (e: Exception) {
                Result.Error(e, e.message ?: "Network error")
            }
        }
    
    /**
     * Update water intake via API
     */
    suspend fun updateWaterIntake(userId: String, date: String, amount: Int): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val request = mapOf("amount" to amount)
                val response = dailyActivityApi.updateWaterIntake(userId, date, request)
                
                if (response.isSuccessful && response.body() != null) {
                    val waterGlasses = response.body()!!["waterGlasses"] as? Double ?: 0.0
                    Result.Success(waterGlasses.toInt())
                } else {
                    Result.Error(
                        Exception("Failed to update water intake"),
                        response.errorBody()?.string() ?: "Unknown error"
                    )
                }
            } catch (e: Exception) {
                Result.Error(e, e.message ?: "Network error")
            }
        }
}

