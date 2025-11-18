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
    private val notificationApi: NotificationApiService = RetrofitClient.create()
    
    // ==================== User Operations ====================
    
    /**
     * Register new user via API
     */
    suspend fun registerUser(
        email: String,
        displayName: String,
        age: Int,
        weightKg: Double,
        heightCm: Double? = null,
        profileImageUrl: String? = null
    ): Result<UserDto> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("NetworkRepository", "📤 Preparing registration request for: $email")
            val request = RegisterRequest(
                email = email,
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

    suspend fun registerFcmToken(token: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = FcmTokenRequest(token)
            val response = notificationApi.registerFcmToken(request)

            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error(
                    Exception("Failed to register FCM token"),
                    response.errorBody()?.string() ?: "Unknown error"
                )
            }
        } catch (e: Exception) {
            Result.Error(e, e.message ?: "Network error")
        }
    }
    
    suspend fun sendNotification(
        title: String,
        body: String,
        data: Map<String, String>? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = SendNotificationRequest(
                title = title,
                body = body,
                data = data
            )
            val response = notificationApi.sendTestNotification(request)

            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error(
                    Exception("Failed to send notification"),
                    response.errorBody()?.string() ?: "Unknown error"
                )
            }
        } catch (e: Exception) {
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
                // Log what we're sending (truncate image data for logging)
                val imageSize = updates.profileImageUrl?.length ?: 0
                android.util.Log.d("NetworkRepository", "📤 Updating user profile: userId=$userId, hasImage=${updates.profileImageUrl != null}, imageSize=$imageSize chars")
                
                val response = userApi.updateUser(userId, updates)
                
                if (response.isSuccessful && response.body() != null) {
                    android.util.Log.d("NetworkRepository", "✅ User profile updated successfully")
                    Result.Success(response.body()!!.user)
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("NetworkRepository", "❌ Failed to update user: ${response.code()} - $errorBody")
                    Result.Error(
                        Exception("Failed to update user"),
                        errorBody ?: "Unknown error"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("NetworkRepository", "❌ Exception updating user profile: ${e.message}", e)
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
                
                val hashMapUpdates = HashMap(updates)
                val response = dailyActivityApi.updateDailyActivity(userId, date, hashMapUpdates)
                
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
     * Update daily activity with custom updates map (for increments from workouts)
     */
    suspend fun updateDailyActivityWithIncrements(
        userId: String,
        date: String,
        updates: Map<String, Any>
    ): Result<DailyActivityResponse> =
        withContext(Dispatchers.IO) {
            try {
                val hashMapUpdates = HashMap(updates)
                val response = dailyActivityApi.updateDailyActivity(userId, date, hashMapUpdates)
                
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

    // ==================== Custom Workout Operations ====================

    /**
     * Create custom workout via API
     */
    suspend fun createCustomWorkout(
        name: String,
        description: String,
        category: String,
        difficulty: String,
        durationMinutes: Int,
        estimatedCalories: Int,
        exerciseCount: Int,
        exercises: List<ExerciseDTO> = emptyList()
    ): Result<CustomWorkoutResponse> = withContext(Dispatchers.IO) {
        try {
            val request = CreateWorkoutRequest(
                name = name,
                description = description,
                category = category,
                difficulty = difficulty,
                durationMinutes = durationMinutes,
                estimatedCalories = estimatedCalories,
                exerciseCount = exerciseCount,
                isCustom = true,
                exercises = exercises
            )

            val response = RetrofitClient.create<CustomWorkoutApiService>().createCustomWorkout(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val workout = body["workout"] as? Map<String, Any>

                if (workout != null) {
                    // Parse exercises if present
                    val exercisesList = (workout["exercises"] as? List<*>)?.mapNotNull { exerciseMap ->
                        (exerciseMap as? Map<*, *>)?.let { map ->
                            ExerciseDTO(
                                name = map["name"] as? String ?: "",
                                description = map["description"] as? String ?: "",
                                muscleGroup = map["muscleGroup"] as? String ?: "",
                                orderIndex = (map["orderIndex"] as? Number)?.toInt() ?: 0,
                                sets = (map["sets"] as? Number)?.toInt(),
                                reps = (map["reps"] as? Number)?.toInt(),
                                durationSeconds = (map["durationSeconds"] as? Number)?.toInt(),
                                restSeconds = (map["restSeconds"] as? Number)?.toInt() ?: 60,
                                videoUrl = map["videoUrl"] as? String,
                                imageUrl = map["imageUrl"] as? String
                            )
                        }
                    } ?: emptyList()
                    
                    val workoutResponse = CustomWorkoutResponse(
                        id = workout["id"] as? String ?: "",
                        name = workout["name"] as? String ?: "",
                        description = workout["description"] as? String ?: "",
                        category = workout["category"] as? String ?: "",
                        difficulty = workout["difficulty"] as? String ?: "",
                        durationMinutes = (workout["durationMinutes"] as? Number)?.toInt() ?: 0,
                        estimatedCalories = (workout["estimatedCalories"] as? Number)?.toInt() ?: 0,
                        exerciseCount = (workout["exerciseCount"] as? Number)?.toInt() ?: 0,
                        isCustom = workout["isCustom"] as? Boolean ?: true,
                        createdBy = workout["createdBy"] as? String,
                        createdAt = (workout["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        thumbnailUrl = workout["thumbnailUrl"] as? String,
                        rating = (workout["rating"] as? Number)?.toDouble() ?: 0.0,
                        exercises = exercisesList
                    )
                    Result.Success(workoutResponse)
                } else {
                    Result.Error(Exception("Invalid response format"), "No workout data in response")
                }
            } else {
                Result.Error(
                    Exception("Failed to create custom workout"),
                    response.errorBody()?.string() ?: "Unknown error"
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("NetworkRepository", "❌ Error creating custom workout: ${e.message}", e)
            Result.Error(e, e.message ?: "Network error")
        }
    }

    // ==================== Workout Session Operations ====================

    /**
     * Create workout session via API
     */
    suspend fun createWorkoutSession(
        workoutName: String,
        workoutType: String,
        startTime: Long,
        endTime: Long?,
        durationSeconds: Int,
        caloriesBurned: Int,
        distanceKm: Double,
        steps: Int,
        avgHeartRate: Int,
        maxHeartRate: Int,
        avgPace: Double,
        notes: String?,
        status: String = "COMPLETED"
    ): Result<WorkoutSessionResponse> = withContext(Dispatchers.IO) {
        try {
            val request = CreateWorkoutSessionRequest(
                workoutName = workoutName,
                workoutType = workoutType,
                startTime = startTime,
                endTime = endTime,
                durationSeconds = durationSeconds,
                caloriesBurned = caloriesBurned,
                distanceKm = distanceKm,
                steps = steps,
                avgHeartRate = avgHeartRate,
                maxHeartRate = maxHeartRate,
                avgPace = avgPace,
                notes = notes,
                status = status
            )

            val response = workoutApi.createWorkoutSession(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val workout = body["workout"] as? Map<String, Any>
                
                if (workout != null) {
                    val sessionResponse = WorkoutSessionResponse(
                        id = workout["id"] as? String ?: "",
                        userId = workout["userId"] as? String ?: "",
                        workoutName = workout["workoutName"] as? String ?: "",
                        workoutType = workout["workoutType"] as? String ?: "OTHER",
                        startTime = (workout["startTime"] as? Number)?.toLong() ?: 0L,
                        endTime = (workout["endTime"] as? Number)?.toLong(),
                        durationSeconds = (workout["durationSeconds"] as? Number)?.toInt() ?: 0,
                        caloriesBurned = (workout["caloriesBurned"] as? Number)?.toInt() ?: 0,
                        distanceKm = (workout["distanceKm"] as? Number)?.toDouble() ?: 0.0,
                        steps = (workout["steps"] as? Number)?.toInt() ?: 0,
                        avgHeartRate = (workout["avgHeartRate"] as? Number)?.toInt() ?: 0,
                        maxHeartRate = (workout["maxHeartRate"] as? Number)?.toInt() ?: 0,
                        avgPace = (workout["avgPace"] as? Number)?.toDouble() ?: 0.0,
                        notes = workout["notes"] as? String,
                        status = workout["status"] as? String ?: "COMPLETED",
                        createdAt = (workout["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        isSynced = workout["isSynced"] as? Boolean ?: true
                    )
                    Result.Success(sessionResponse)
                } else {
                    Result.Error(Exception("Invalid response format"), "No workout data in response")
                }
            } else {
                Result.Error(
                    Exception("Failed to create workout session"),
                    response.errorBody()?.string() ?: "Unknown error"
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("NetworkRepository", "❌ Error creating workout session: ${e.message}", e)
            Result.Error(e, e.message ?: "Network error")
        }
    }

    /**
     * Get workout sessions for a user
     */
    suspend fun getWorkoutSessions(
        limit: Int = 50,
        status: String? = null
    ): Result<List<WorkoutSessionResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = workoutApi.getWorkoutSessions(limit, status)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val workouts = body.workouts.map { dto ->
                    WorkoutSessionResponse(
                        id = dto.id,
                        userId = dto.userId,
                        workoutName = dto.workoutName,
                        workoutType = dto.workoutType ?: "OTHER",
                        startTime = dto.startTime,
                        endTime = dto.endTime,
                        durationSeconds = dto.durationSeconds,
                        caloriesBurned = dto.caloriesBurned ?: 0,
                        distanceKm = dto.distanceKm ?: 0.0,
                        steps = dto.steps ?: 0,
                        avgHeartRate = dto.avgHeartRate ?: 0,
                        maxHeartRate = dto.maxHeartRate ?: 0,
                        avgPace = dto.avgPace ?: 0.0,
                        notes = dto.notes,
                        status = dto.status,
                        createdAt = dto.createdAt ?: System.currentTimeMillis(),
                        isSynced = true
                    )
                }
                Result.Success(workouts)
            } else {
                Result.Error(
                    Exception("Failed to fetch workout sessions"),
                    response.errorBody()?.string() ?: "Unknown error"
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("NetworkRepository", "❌ Error fetching workout sessions: ${e.message}", e)
            Result.Error(e, e.message ?: "Network error")
        }
    }

    /**
     * Delete workout session via API
     */
    suspend fun deleteWorkoutSession(workoutId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = workoutApi.deleteWorkoutSession(workoutId)

                if (response.isSuccessful) {
                    Result.Success(Unit)
                } else {
                    Result.Error(
                        Exception("Failed to delete workout session"),
                        response.errorBody()?.string() ?: "Unknown error"
                    )
                }
            } catch (e: Exception) {
                Result.Error(e, e.message ?: "Network error")
            }
        }

    /**
     * Sync workout sessions from Firebase to local DB
     */
    suspend fun syncWorkoutSessionsFromFirebase(userId: String): Result<List<com.example.prog7314_part1.data.local.entity.WorkoutSession>> =
        withContext(Dispatchers.IO) {
            try {
                when (val result = getWorkoutSessions(limit = 100)) {
                    is Result.Success -> {
                        val sessions = result.data.map { dto ->
                            com.example.prog7314_part1.data.local.entity.WorkoutSession(
                                sessionId = dto.id,
                                userId = dto.userId,
                                workoutId = null,
                                workoutName = dto.workoutName,
                                startTime = dto.startTime,
                                endTime = dto.endTime,
                                durationSeconds = dto.durationSeconds,
                                caloriesBurned = dto.caloriesBurned,
                                distanceKm = dto.distanceKm,
                                steps = dto.steps,              // ✅ From Firebase
                                avgHeartRate = dto.avgHeartRate, // ✅ From Firebase
                                maxHeartRate = dto.maxHeartRate, // ✅ From Firebase
                                avgPace = dto.avgPace,          // ✅ From Firebase
                                routeData = null,
                                status = com.example.prog7314_part1.data.local.entity.SessionStatus.valueOf(
                                    dto.status.uppercase()
                                ),
                                createdAt = dto.createdAt,
                                isSynced = true
                            )
                        }

                        // Save to local DB
                        database.workoutSessionDao().insertSessions(sessions)
                        android.util.Log.d("NetworkRepository", "✅ Synced ${sessions.size} workout sessions from Firebase")
                        Result.Success(sessions)
                    }
                    is Result.Error -> {
                        android.util.Log.w("NetworkRepository", "⚠️ Failed to sync workout sessions: ${result.message}")
                        result
                    }
                    else -> Result.Error(Exception("Unknown error"), "Failed to sync")
                }
            } catch (e: Exception) {
                android.util.Log.e("NetworkRepository", "❌ Error syncing workout sessions: ${e.message}", e)
                Result.Error(e, e.message ?: "Sync error")
            }
        }

    /**
     * Get workout statistics summary from Firebase
     * Returns total calories, steps, distance, etc. for a date range
     */
    suspend fun getWorkoutStats(
        startDate: Long? = null,
        endDate: Long? = null
    ): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val response = workoutApi.getWorkoutStats(startDate, endDate)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val summary = body["summary"] as? Map<String, Any>

                if (summary != null) {
                    android.util.Log.d("NetworkRepository", "✅ Fetched workout stats: ${summary}")
                    Result.Success(summary)
                } else {
                    Result.Error(Exception("Invalid stats format"), "No summary data")
                }
            } else {
                Result.Error(
                    Exception("Failed to fetch stats"),
                    response.errorBody()?.string() ?: "Unknown error"
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("NetworkRepository", "❌ Error fetching workout stats: ${e.message}", e)
            Result.Error(e, e.message ?: "Network error")
        }
    }

    /**
     * Get total calories and distance from workouts for today
     * Useful for Home/Progress pages
     */
    suspend fun getTodayWorkoutTotals(): Result<Triple<Int, Double, Int>> = withContext(Dispatchers.IO) {
        try {
            // Get today's start timestamp (midnight)
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            val todayStart = calendar.timeInMillis

            // Get today's end timestamp
            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
            val todayEnd = calendar.timeInMillis

            when (val result = getWorkoutStats(todayStart, todayEnd)) {
                is Result.Success -> {
                    val totalCalories = (result.data["totalCaloriesBurned"] as? Number)?.toInt() ?: 0
                    val totalDistance = (result.data["totalDistanceKm"] as? Number)?.toDouble() ?: 0.0
                    val totalWorkouts = (result.data["totalWorkouts"] as? Number)?.toInt() ?: 0
                    android.util.Log.d("NetworkRepository", "✅ Today's workout totals: $totalCalories cal, $totalDistance km, $totalWorkouts workouts")
                    Result.Success(Triple(totalCalories, totalDistance, totalWorkouts))
                }
                is Result.Error -> {
                    android.util.Log.w("NetworkRepository", "⚠️ Failed to get today's totals: ${result.message}")
                    Result.Error(result.exception, result.message)
                }
                else -> Result.Error(Exception("Unknown error"), "Failed to get totals")
            }
        } catch (e: Exception) {
            android.util.Log.e("NetworkRepository", "❌ Error getting today's totals: ${e.message}", e)
            Result.Error(e, e.message ?: "Error")
        }
    }
}

