package com.example.prog7314_part1.data.repository

import com.example.prog7314_part1.data.local.dao.ExerciseDao
import com.example.prog7314_part1.data.local.dao.WorkoutDao
import com.example.prog7314_part1.data.local.entity.Exercise
import com.example.prog7314_part1.data.local.entity.Workout
import com.example.prog7314_part1.data.local.entity.WorkoutCategory
import com.example.prog7314_part1.data.local.entity.WorkoutDifficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Repository for managing workout data from Room database
 */
class WorkoutRepository(
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao? = null
) {

    /** Get all workouts (predefined + custom for user) */
    fun getAllWorkouts(userId: String): Flow<List<Workout>> = combine(
        workoutDao.getCustomWorkouts(userId),
        workoutDao.getPreDefinedWorkouts()
    ) { custom, predefined ->
        (predefined + custom).sortedByDescending { it.rating }
    }

    /** Get workouts filtered by category (predefined + custom) */
    fun getWorkoutsByCategory(userId: String, category: WorkoutCategory): Flow<List<Workout>> =
        combine(workoutDao.getCustomWorkouts(userId), workoutDao.getPreDefinedWorkouts()) { custom, predefined ->
            (predefined + custom).filter { it.category == category }.sortedByDescending { it.rating }
        }

    fun getUserCreatedWorkouts(userId: String): Flow<List<Workout>> {
        return workoutDao.getCustomWorkouts(userId) // Make a DAO query for user's workouts
    }

    /** Get workouts filtered by difficulty (predefined + custom) */
    fun getWorkoutsByDifficulty(userId: String, difficulty: WorkoutDifficulty): Flow<List<Workout>> =
        combine(workoutDao.getCustomWorkouts(userId), workoutDao.getPreDefinedWorkouts()) { custom, predefined ->
            (predefined + custom).filter { it.difficulty == difficulty }.sortedByDescending { it.rating }
        }

    /** Search workouts by query string (name, description, category) */
    fun searchWorkouts(userId: String, query: String): Flow<List<Workout>> =
        combine(workoutDao.getCustomWorkouts(userId), workoutDao.getPreDefinedWorkouts()) { custom, predefined ->
            val allWorkouts = predefined + custom
            allWorkouts.filter { workout ->
                workout.name.contains(query, ignoreCase = true) ||
                        workout.description.contains(query, ignoreCase = true) ||
                        workout.category.name.contains(query, ignoreCase = true)
            }.sortedByDescending { it.rating }
        }

    /** Get a single workout by its ID */
    suspend fun getWorkoutById(workoutId: String): Workout? = workoutDao.getWorkoutById(workoutId)

    /** Insert a new workout */
    suspend fun insertWorkout(workout: Workout) = workoutDao.insertWorkout(workout)

    /** Insert multiple workouts */
    suspend fun insertWorkouts(workouts: List<Workout>) = workoutDao.insertWorkouts(workouts)

    /** Update a workout */
    suspend fun updateWorkout(workout: Workout) = workoutDao.updateWorkout(workout)

    /** Delete a workout */
    suspend fun deleteWorkout(workout: Workout) = workoutDao.deleteWorkout(workout)

    // --- Exercise operations ---
    fun getExercisesForWorkout(workoutId: String): Flow<List<Exercise>> =
        exerciseDao?.getExercisesForWorkout(workoutId)
            ?: throw IllegalStateException("ExerciseDao not initialized")

    suspend fun getExercisesForWorkoutSuspend(workoutId: String): List<Exercise> =
        exerciseDao?.getExercisesForWorkoutSuspend(workoutId)
            ?: throw IllegalStateException("ExerciseDao not initialized")

    suspend fun insertExercise(exercise: Exercise) =
        exerciseDao?.insertExercise(exercise) ?: throw IllegalStateException("ExerciseDao not initialized")

    suspend fun insertExercises(exercises: List<Exercise>) =
        exerciseDao?.insertExercises(exercises) ?: throw IllegalStateException("ExerciseDao not initialized")

    suspend fun updateExercise(exercise: Exercise) =
        exerciseDao?.updateExercise(exercise) ?: throw IllegalStateException("ExerciseDao not initialized")

    suspend fun deleteExercise(exercise: Exercise) =
        exerciseDao?.deleteExercise(exercise) ?: throw IllegalStateException("ExerciseDao not initialized")
}
