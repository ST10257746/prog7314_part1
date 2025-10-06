package com.example.prog7314_part1.data.local.dao

import androidx.room.*
import com.example.prog7314_part1.data.local.entity.Workout
import com.example.prog7314_part1.data.local.entity.WorkoutCategory
import com.example.prog7314_part1.data.local.entity.WorkoutDifficulty
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Workout entity
 */
@Dao
interface WorkoutDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: Workout)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkouts(workouts: List<Workout>)
    
    @Update
    suspend fun updateWorkout(workout: Workout)
    
    @Delete
    suspend fun deleteWorkout(workout: Workout)
    
    @Query("SELECT * FROM workouts WHERE workoutId = :workoutId")
    suspend fun getWorkoutById(workoutId: String): Workout?
    
    @Query("SELECT * FROM workouts WHERE workoutId = :workoutId")
    fun getWorkoutByIdFlow(workoutId: String): Flow<Workout?>
    
    @Query("SELECT * FROM workouts WHERE name IN ('Cardio Burn', 'Fat Burning Cardio', 'Beginner Cardio', 'Strength Builder', 'Power Lifting', 'Beginner Strength', 'Morning Yoga Flow', 'Power Yoga', 'Relaxing Yoga') ORDER BY rating DESC")
    fun getAllWorkouts(): Flow<List<Workout>>
    
    @Query("SELECT * FROM workouts WHERE category = :category AND name IN ('Cardio Burn', 'Fat Burning Cardio', 'Beginner Cardio', 'Strength Builder', 'Power Lifting', 'Beginner Strength', 'Morning Yoga Flow', 'Power Yoga', 'Relaxing Yoga') ORDER BY rating DESC")
    fun getWorkoutsByCategory(category: WorkoutCategory): Flow<List<Workout>>
    
    @Query("SELECT * FROM workouts WHERE difficulty = :difficulty AND name IN ('Cardio Burn', 'Fat Burning Cardio', 'Beginner Cardio', 'Strength Builder', 'Power Lifting', 'Beginner Strength', 'Morning Yoga Flow', 'Power Yoga', 'Relaxing Yoga') ORDER BY rating DESC")
    fun getWorkoutsByDifficulty(difficulty: WorkoutDifficulty): Flow<List<Workout>>
    
    @Query("SELECT * FROM workouts WHERE isCustom = 1 AND createdBy = :userId ORDER BY createdAt DESC")
    fun getCustomWorkouts(userId: String): Flow<List<Workout>>
    
    @Query("SELECT * FROM workouts WHERE name IN ('Cardio Burn', 'Fat Burning Cardio', 'Beginner Cardio', 'Strength Builder', 'Power Lifting', 'Beginner Strength', 'Morning Yoga Flow', 'Power Yoga', 'Relaxing Yoga') ORDER BY rating DESC")
    fun getPreDefinedWorkouts(): Flow<List<Workout>>
    
    @Query("SELECT * FROM workouts WHERE name IN ('Cardio Burn', 'Fat Burning Cardio', 'Beginner Cardio', 'Strength Builder', 'Power Lifting', 'Beginner Strength', 'Morning Yoga Flow', 'Power Yoga', 'Relaxing Yoga') AND (LOWER(name) LIKE '%' || LOWER(:searchQuery) || '%' OR LOWER(description) LIKE '%' || LOWER(:searchQuery) || '%' OR LOWER(category) LIKE '%' || LOWER(:searchQuery) || '%') ORDER BY rating DESC")
    fun searchWorkouts(searchQuery: String): Flow<List<Workout>>
    
    @Query("DELETE FROM workouts WHERE workoutId = :workoutId")
    suspend fun deleteWorkoutById(workoutId: String)
    
    @Query("DELETE FROM workouts WHERE isCustom = 0")
    suspend fun deleteAllPredefinedWorkouts()
    
    @Query("DELETE FROM workouts")
    suspend fun deleteAllWorkouts()
}
