package com.example.prog7314_part1.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.prog7314_part1.data.local.AppDatabase
import com.example.prog7314_part1.data.repository.NetworkRepository
import com.google.firebase.auth.FirebaseAuth

/**
 * SyncWorker
 * 
 * WorkManager worker that automatically syncs unsynced data from Room database to the REST API
 * when network connection is available. This ensures offline data is synchronized once the
 * device regains internet connectivity.
 * 
 * References:
 * - Android WorkManager: https://developer.android.com/topic/libraries/architecture/workmanager
 * - Offline-first architecture: https://developer.android.com/topic/architecture/data-layer/offline-first
 * 
 * @author FitTrackr Development Team
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val database = AppDatabase.getDatabase(context)
    private val networkRepository = NetworkRepository(context)
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "SyncWorker"
        const val WORK_NAME = "sync_worker"
    }

    /**
     * Main sync operation
     * Syncs all unsynced entities (workout sessions, nutrition entries, daily activities, goals)
     */
    override suspend fun doWork(): Result {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.d(TAG, "⏸️ No user logged in, skipping sync")
                return Result.success()
            }

            val userId = currentUser.uid
            Log.d(TAG, "🔄 Starting sync for user: $userId")

            var syncCount = 0

            // Sync workout sessions
            try {
                val unsyncedSessions = database.workoutSessionDao().getUnsyncedSessions(userId)
                if (unsyncedSessions.isNotEmpty()) {
                    Log.d(TAG, "📤 Syncing ${unsyncedSessions.size} workout sessions...")
                    syncCount += syncWorkoutSessions(unsyncedSessions)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error syncing workout sessions: ${e.message}", e)
            }

            // Sync nutrition entries
            try {
                val unsyncedNutrition = database.nutritionEntryDao().getUnsyncedEntries(userId)
                if (unsyncedNutrition.isNotEmpty()) {
                    Log.d(TAG, "📤 Syncing ${unsyncedNutrition.size} nutrition entries...")
                    syncCount += syncNutritionEntries(unsyncedNutrition, userId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error syncing nutrition entries: ${e.message}", e)
            }

            // Sync daily activities
            try {
                val unsyncedActivities = database.dailyActivityDao().getUnsyncedActivities(userId)
                if (unsyncedActivities.isNotEmpty()) {
                    Log.d(TAG, "📤 Syncing ${unsyncedActivities.size} daily activities...")
                    syncCount += syncDailyActivities(unsyncedActivities, userId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error syncing daily activities: ${e.message}", e)
            }

            // Sync goals
            try {
                val unsyncedGoals = database.goalDao().getUnsyncedGoals(userId)
                if (unsyncedGoals.isNotEmpty()) {
                    Log.d(TAG, "📤 Syncing ${unsyncedGoals.size} goals...")
                    syncCount += syncGoals(unsyncedGoals, userId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error syncing goals: ${e.message}", e)
            }

            // Sync user profile (if updated since last sync)
            try {
                val unsyncedUser = database.userDao().getUnsyncedUser(userId)
                if (unsyncedUser != null) {
                    Log.d(TAG, "📤 Syncing user profile...")
                    if (syncUserProfile(unsyncedUser, userId)) {
                        syncCount++
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error syncing user profile: ${e.message}", e)
            }

            // Sync custom workouts
            try {
                val unsyncedWorkouts = database.workoutDao().getUnsyncedCustomWorkouts(userId)
                if (unsyncedWorkouts.isNotEmpty()) {
                    Log.d(TAG, "📤 Syncing ${unsyncedWorkouts.size} custom workouts...")
                    syncCount += syncCustomWorkouts(unsyncedWorkouts, userId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error syncing custom workouts: ${e.message}", e)
            }

            if (syncCount > 0) {
                Log.d(TAG, "✅ Sync completed successfully. Synced $syncCount items")
            } else {
                Log.d(TAG, "✅ No unsynced data found")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Sync failed: ${e.message}", e)
            // Retry on failure
            Result.retry()
        }
    }

    /**
     * Sync unsynced workout sessions to API
     */
    private suspend fun syncWorkoutSessions(sessions: List<com.example.prog7314_part1.data.local.entity.WorkoutSession>): Int {
        var syncedCount = 0
        for (session in sessions) {
            try {
                val result = networkRepository.createWorkoutSession(
                    workoutName = session.workoutName,
                    workoutType = "CARDIO", // Default type
                    startTime = session.startTime,
                    endTime = session.endTime ?: session.startTime + (session.durationSeconds * 1000L),
                    durationSeconds = session.durationSeconds,
                    caloriesBurned = session.caloriesBurned,
                    distanceKm = session.distanceKm,
                    steps = session.steps,
                    avgHeartRate = session.avgHeartRate,
                    maxHeartRate = session.maxHeartRate,
                    avgPace = session.avgPace,
                    notes = null,
                    status = session.status.name
                )

                when (result) {
                    is com.example.prog7314_part1.data.model.Result.Success -> {
                        database.workoutSessionDao().markAsSynced(session.sessionId)
                        syncedCount++
                        Log.d(TAG, "✅ Synced workout session: ${session.sessionId}")
                    }
                    is com.example.prog7314_part1.data.model.Result.Error -> {
                        Log.w(TAG, "⚠️ Failed to sync workout session ${session.sessionId}: ${result.message}")
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception syncing workout session ${session.sessionId}: ${e.message}", e)
            }
        }
        return syncedCount
    }

    /**
     * Sync unsynced nutrition entries to API
     */
    private suspend fun syncNutritionEntries(
        entries: List<com.example.prog7314_part1.data.local.entity.NutritionEntry>,
        userId: String
    ): Int {
        var syncedCount = 0
        for (entry in entries) {
            try {
                val request = com.example.prog7314_part1.data.network.model.CreateNutritionRequest(
                    foodName = entry.foodName,
                    mealType = entry.mealType.name,
                    servingSize = entry.servingSize,
                    calories = entry.calories,
                    proteinG = entry.proteinG,
                    carbsG = entry.carbsG,
                    fatsG = entry.fatsG,
                    fiberG = entry.fiberG,
                    sugarG = entry.sugarG,
                    notes = entry.description ?: "",
                    timestamp = entry.createdAt
                )

                val result = networkRepository.createNutrition(request)

                when (result) {
                    is com.example.prog7314_part1.data.model.Result.Success -> {
                        database.nutritionEntryDao().markAsSynced(entry.entryId)
                        syncedCount++
                        Log.d(TAG, "✅ Synced nutrition entry: ${entry.entryId}")
                    }
                    is com.example.prog7314_part1.data.model.Result.Error -> {
                        Log.w(TAG, "⚠️ Failed to sync nutrition entry ${entry.entryId}: ${result.message}")
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception syncing nutrition entry ${entry.entryId}: ${e.message}", e)
            }
        }
        return syncedCount
    }

    /**
     * Sync unsynced daily activities to API
     * Uses updateDailyActivity which creates the activity if it doesn't exist
     */
    private suspend fun syncDailyActivities(
        activities: List<com.example.prog7314_part1.data.local.entity.DailyActivity>,
        userId: String
    ): Int {
        var syncedCount = 0
        for (activity in activities) {
            try {
                val result = networkRepository.updateDailyActivity(
                    userId = userId,
                    date = activity.date,
                    steps = activity.steps,
                    caloriesBurned = activity.caloriesBurned,
                    distance = activity.distanceKm,
                    activeMinutes = activity.activeMinutes
                )

                when (result) {
                    is com.example.prog7314_part1.data.model.Result.Success -> {
                        database.dailyActivityDao().markAsSynced(activity.activityId)
                        syncedCount++
                        Log.d(TAG, "✅ Synced daily activity: ${activity.activityId}")
                    }
                    is com.example.prog7314_part1.data.model.Result.Error -> {
                        Log.w(TAG, "⚠️ Failed to sync daily activity ${activity.activityId}: ${result.message}")
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception syncing daily activity ${activity.activityId}: ${e.message}", e)
            }
        }
        return syncedCount
    }

    /**
     * Sync unsynced goals to API
     */
    private suspend fun syncGoals(
        goals: List<com.example.prog7314_part1.data.local.entity.Goal>,
        userId: String
    ): Int {
        var syncedCount = 0
        for (goal in goals) {
            try {
                val goalMap = mutableMapOf<String, Any>()
                goalMap["userId"] = userId
                goalMap["title"] = goal.title
                goalMap["description"] = goal.description ?: ""
                goalMap["date"] = goal.date
                goalMap["isCompleted"] = goal.isCompleted
                goal.completedAt?.let { goalMap["completedAt"] = it }
                goalMap["createdAt"] = goal.createdAt

                val result = networkRepository.createGoal(goalMap)

                when (result) {
                    is com.example.prog7314_part1.data.model.Result.Success -> {
                        database.goalDao().markAsSynced(goal.goalId)
                        syncedCount++
                        Log.d(TAG, "✅ Synced goal: ${goal.goalId}")
                    }
                    is com.example.prog7314_part1.data.model.Result.Error -> {
                        Log.w(TAG, "⚠️ Failed to sync goal ${goal.goalId}: ${result.message}")
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception syncing goal ${goal.goalId}: ${e.message}", e)
            }
        }
        return syncedCount
    }

    /**
     * Sync unsynced user profile to API
     */
    private suspend fun syncUserProfile(
        user: com.example.prog7314_part1.data.local.entity.User,
        userId: String
    ): Boolean {
        return try {
            val updates = com.example.prog7314_part1.data.network.model.UpdateUserRequest(
                displayName = user.displayName,
                age = user.age,
                weightKg = user.weightKg,
                heightCm = user.heightCm,
                dailyStepGoal = user.dailyStepGoal,
                dailyCalorieGoal = user.dailyCalorieGoal,
                dailyWaterGoal = user.dailyWaterGoal,
                weeklyWorkoutGoal = user.weeklyWorkoutGoal,
                proteinGoalG = user.proteinGoalG,
                carbsGoalG = user.carbsGoalG,
                fatsGoalG = user.fatsGoalG,
                profileImageUrl = user.profileImageUrl
            )

            val result = networkRepository.updateUserProfile(userId, updates)

            when (result) {
                is com.example.prog7314_part1.data.model.Result.Success -> {
                    database.userDao().markUserAsSynced(userId)
                    Log.d(TAG, "✅ Synced user profile")
                    true
                }
                is com.example.prog7314_part1.data.model.Result.Error -> {
                    Log.w(TAG, "⚠️ Failed to sync user profile: ${result.message}")
                    false
                }
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception syncing user profile: ${e.message}", e)
            false
        }
    }

    /**
     * Sync unsynced custom workouts to API
     */
    private suspend fun syncCustomWorkouts(
        workouts: List<com.example.prog7314_part1.data.local.entity.Workout>,
        userId: String
    ): Int {
        var syncedCount = 0
        for (workout in workouts) {
            try {
                // Get exercises for this workout
                val exercises = database.exerciseDao().getExercisesForWorkoutSuspend(workout.workoutId)
                val exerciseDTOs = exercises.map { exercise ->
                    com.example.prog7314_part1.data.network.model.ExerciseDTO(
                        name = exercise.name,
                        description = exercise.description,
                        muscleGroup = exercise.muscleGroup,
                        orderIndex = exercise.orderIndex,
                        sets = exercise.sets,
                        reps = exercise.reps,
                        durationSeconds = exercise.durationSeconds,
                        restSeconds = exercise.restSeconds,
                        videoUrl = exercise.videoUrl,
                        imageUrl = exercise.imageUrl
                    )
                }

                val result = networkRepository.createCustomWorkout(
                    name = workout.name,
                    description = workout.description,
                    category = workout.category.name,
                    difficulty = workout.difficulty.name,
                    durationMinutes = workout.durationMinutes,
                    estimatedCalories = workout.estimatedCalories,
                    exerciseCount = workout.exerciseCount,
                    exercises = exerciseDTOs
                )

                when (result) {
                    is com.example.prog7314_part1.data.model.Result.Success -> {
                        database.workoutDao().markAsSynced(workout.workoutId)
                        syncedCount++
                        Log.d(TAG, "✅ Synced custom workout: ${workout.workoutId}")
                    }
                    is com.example.prog7314_part1.data.model.Result.Error -> {
                        Log.w(TAG, "⚠️ Failed to sync custom workout ${workout.workoutId}: ${result.message}")
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception syncing custom workout ${workout.workoutId}: ${e.message}", e)
            }
        }
        return syncedCount
    }
}

