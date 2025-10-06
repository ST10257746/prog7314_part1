package com.example.prog7314_part1.data.repository

import com.example.prog7314_part1.data.local.dao.ExerciseDao
import com.example.prog7314_part1.data.local.dao.WorkoutDao
import com.example.prog7314_part1.data.local.entity.Exercise
import com.example.prog7314_part1.data.local.entity.Workout
import com.example.prog7314_part1.data.local.entity.WorkoutCategory
import com.example.prog7314_part1.data.local.entity.WorkoutDifficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
/**
 * Repository for managing workout data
 */
class WorkoutRepository(
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao? = null
) {
    
    // Hardcoded workouts that will ALWAYS be returned (bypassing database)
    private val hardcodedWorkouts = listOf(
        // CARDIO Workouts (5 workouts)
        Workout(
            name = "Cardio Burn",
            description = "High energy cardio session to burn calories fast",
            category = WorkoutCategory.CARDIO,
            difficulty = WorkoutDifficulty.INTERMEDIATE,
            durationMinutes = 35,
            estimatedCalories = 400,
            exerciseCount = 6,
            rating = 4.5,
            isCustom = false
        ),
        Workout(
            name = "Fat Burning Cardio",
            description = "Intense cardio workout for maximum fat burn",
            category = WorkoutCategory.CARDIO,
            difficulty = WorkoutDifficulty.ADVANCED,
            durationMinutes = 45,
            estimatedCalories = 520,
            exerciseCount = 8,
            rating = 4.7,
            isCustom = false
        ),
        Workout(
            name = "Beginner Cardio",
            description = "Easy cardio workout for beginners",
            category = WorkoutCategory.CARDIO,
            difficulty = WorkoutDifficulty.BEGINNER,
            durationMinutes = 20,
            estimatedCalories = 180,
            exerciseCount = 5,
            rating = 4.3,
            isCustom = false
        ),
        Workout(
            name = "Dance Cardio",
            description = "Fun dance moves that get your heart pumping",
            category = WorkoutCategory.CARDIO,
            difficulty = WorkoutDifficulty.INTERMEDIATE,
            durationMinutes = 30,
            estimatedCalories = 320,
            exerciseCount = 7,
            rating = 4.6,
            isCustom = false
        ),
        Workout(
            name = "Running Intervals",
            description = "Interval running for endurance and speed",
            category = WorkoutCategory.CARDIO,
            difficulty = WorkoutDifficulty.ADVANCED,
            durationMinutes = 40,
            estimatedCalories = 480,
            exerciseCount = 6,
            rating = 4.4,
            isCustom = false
        ),
        
        // STRENGTH Workouts (5 workouts)
        Workout(
            name = "Strength Builder",
            description = "Build muscle and increase strength",
            category = WorkoutCategory.STRENGTH,
            difficulty = WorkoutDifficulty.INTERMEDIATE,
            durationMinutes = 50,
            estimatedCalories = 350,
            exerciseCount = 10,
            rating = 4.8,
            isCustom = false
        ),
        Workout(
            name = "Power Lifting",
            description = "Advanced strength training with heavy weights",
            category = WorkoutCategory.STRENGTH,
            difficulty = WorkoutDifficulty.ADVANCED,
            durationMinutes = 60,
            estimatedCalories = 420,
            exerciseCount = 8,
            rating = 4.9,
            isCustom = false
        ),
        Workout(
            name = "Beginner Strength",
            description = "Perfect introduction to strength training",
            category = WorkoutCategory.STRENGTH,
            difficulty = WorkoutDifficulty.BEGINNER,
            durationMinutes = 30,
            estimatedCalories = 220,
            exerciseCount = 8,
            rating = 4.6,
            isCustom = false
        ),
        Workout(
            name = "Upper Body Blast",
            description = "Target your arms, chest, and shoulders",
            category = WorkoutCategory.STRENGTH,
            difficulty = WorkoutDifficulty.INTERMEDIATE,
            durationMinutes = 40,
            estimatedCalories = 300,
            exerciseCount = 9,
            rating = 4.7,
            isCustom = false
        ),
        Workout(
            name = "Lower Body Power",
            description = "Build strong legs and glutes",
            category = WorkoutCategory.STRENGTH,
            difficulty = WorkoutDifficulty.ADVANCED,
            durationMinutes = 45,
            estimatedCalories = 380,
            exerciseCount = 10,
            rating = 4.8,
            isCustom = false
        ),
        
        // YOGA Workouts (4 workouts)
        Workout(
            name = "Morning Yoga Flow",
            description = "Start your day with mindfulness and flexibility",
            category = WorkoutCategory.YOGA,
            difficulty = WorkoutDifficulty.BEGINNER,
            durationMinutes = 25,
            estimatedCalories = 120,
            exerciseCount = 12,
            rating = 4.9,
            isCustom = false
        ),
        Workout(
            name = "Power Yoga",
            description = "Dynamic yoga for strength and flexibility",
            category = WorkoutCategory.YOGA,
            difficulty = WorkoutDifficulty.INTERMEDIATE,
            durationMinutes = 45,
            estimatedCalories = 200,
            exerciseCount = 15,
            rating = 4.7,
            isCustom = false
        ),
        Workout(
            name = "Relaxing Yoga",
            description = "Gentle yoga for stress relief and relaxation",
            category = WorkoutCategory.YOGA,
            difficulty = WorkoutDifficulty.BEGINNER,
            durationMinutes = 30,
            estimatedCalories = 100,
            exerciseCount = 10,
            rating = 4.8,
            isCustom = false
        ),
        Workout(
            name = "Hot Yoga Challenge",
            description = "Intense yoga practice in heated environment",
            category = WorkoutCategory.YOGA,
            difficulty = WorkoutDifficulty.ADVANCED,
            durationMinutes = 60,
            estimatedCalories = 350,
            exerciseCount = 18,
            rating = 4.5,
            isCustom = false
        ),
        
        // HIIT Workouts (4 workouts)
        Workout(
            name = "20-Min HIIT Blast",
            description = "High-intensity interval training for maximum results",
            category = WorkoutCategory.HIIT,
            difficulty = WorkoutDifficulty.INTERMEDIATE,
            durationMinutes = 20,
            estimatedCalories = 280,
            exerciseCount = 8,
            rating = 4.8,
            isCustom = false
        ),
        Workout(
            name = "HIIT Tabata",
            description = "4-minute intense Tabata workout",
            category = WorkoutCategory.HIIT,
            difficulty = WorkoutDifficulty.ADVANCED,
            durationMinutes = 15,
            estimatedCalories = 200,
            exerciseCount = 6,
            rating = 4.6,
            isCustom = false
        ),
        Workout(
            name = "Beginner HIIT",
            description = "Introduction to high-intensity interval training",
            category = WorkoutCategory.HIIT,
            difficulty = WorkoutDifficulty.BEGINNER,
            durationMinutes = 25,
            estimatedCalories = 250,
            exerciseCount = 7,
            rating = 4.5,
            isCustom = false
        ),
        Workout(
            name = "Full Body HIIT",
            description = "Complete body workout with high intensity intervals",
            category = WorkoutCategory.HIIT,
            difficulty = WorkoutDifficulty.INTERMEDIATE,
            durationMinutes = 30,
            estimatedCalories = 350,
            exerciseCount = 10,
            rating = 4.7,
            isCustom = false
        ),
        
        // FLEXIBILITY Workouts (4 workouts)
        Workout(
            name = "Flexibility Focus",
            description = "Improve your flexibility and range of motion",
            category = WorkoutCategory.FLEXIBILITY,
            difficulty = WorkoutDifficulty.BEGINNER,
            durationMinutes = 40,
            estimatedCalories = 150,
            exerciseCount = 15,
            rating = 4.4,
            isCustom = false
        ),
        Workout(
            name = "Deep Stretch",
            description = "Advanced stretching for improved mobility",
            category = WorkoutCategory.FLEXIBILITY,
            difficulty = WorkoutDifficulty.INTERMEDIATE,
            durationMinutes = 35,
            estimatedCalories = 120,
            exerciseCount = 12,
            rating = 4.6,
            isCustom = false
        ),
        Workout(
            name = "Quick Stretch",
            description = "Fast stretching routine for busy schedules",
            category = WorkoutCategory.FLEXIBILITY,
            difficulty = WorkoutDifficulty.BEGINNER,
            durationMinutes = 15,
            estimatedCalories = 60,
            exerciseCount = 8,
            rating = 4.2,
            isCustom = false
        ),
        Workout(
            name = "Post-Workout Recovery",
            description = "Essential stretches for muscle recovery",
            category = WorkoutCategory.FLEXIBILITY,
            difficulty = WorkoutDifficulty.INTERMEDIATE,
            durationMinutes = 25,
            estimatedCalories = 90,
            exerciseCount = 10,
            rating = 4.5,
            isCustom = false
        ),
        
        // CORE Workouts (3 workouts)
        Workout(
            name = "Core Crusher",
            description = "Intense core workout for strong abs",
            category = WorkoutCategory.CORE,
            difficulty = WorkoutDifficulty.INTERMEDIATE,
            durationMinutes = 25,
            estimatedCalories = 180,
            exerciseCount = 8,
            rating = 4.6,
            isCustom = false
        ),
        Workout(
            name = "Beginner Core",
            description = "Build your core foundation safely",
            category = WorkoutCategory.CORE,
            difficulty = WorkoutDifficulty.BEGINNER,
            durationMinutes = 20,
            estimatedCalories = 120,
            exerciseCount = 6,
            rating = 4.4,
            isCustom = false
        ),
        Workout(
            name = "Advanced Ab Shredder",
            description = "Elite level core training for maximum definition",
            category = WorkoutCategory.CORE,
            difficulty = WorkoutDifficulty.ADVANCED,
            durationMinutes = 35,
            estimatedCalories = 250,
            exerciseCount = 12,
            rating = 4.8,
            isCustom = false
        )
    )
    
    // Return only hardcoded workouts (bypass database completely)
    fun getAllWorkouts(): Flow<List<Workout>> = flow {
        emit(hardcodedWorkouts.sortedByDescending { it.rating })
    }
    
    fun getWorkoutsByCategory(category: WorkoutCategory): Flow<List<Workout>> = flow {
        emit(hardcodedWorkouts.filter { it.category == category }.sortedByDescending { it.rating })
    }
    
    fun getWorkoutsByDifficulty(difficulty: WorkoutDifficulty): Flow<List<Workout>> = flow {
        emit(hardcodedWorkouts.filter { it.difficulty == difficulty }.sortedByDescending { it.rating })
    }
    
    fun getPreDefinedWorkouts(): Flow<List<Workout>> = flow {
        emit(hardcodedWorkouts.sortedByDescending { it.rating })
    }
    
    fun getCustomWorkouts(userId: String): Flow<List<Workout>> = 
        workoutDao.getCustomWorkouts(userId)
    
    fun searchWorkouts(query: String): Flow<List<Workout>> = flow {
        val filteredWorkouts = hardcodedWorkouts.filter { workout ->
            workout.name.contains(query, ignoreCase = true) ||
            workout.description.contains(query, ignoreCase = true) ||
            workout.category.name.contains(query, ignoreCase = true)
        }.sortedByDescending { it.rating }
        emit(filteredWorkouts)
    }
    
    suspend fun getWorkoutById(workoutId: String): Workout? = 
        workoutDao.getWorkoutById(workoutId)
    
    suspend fun insertWorkout(workout: Workout) = workoutDao.insertWorkout(workout)
    
    suspend fun insertWorkouts(workouts: List<Workout>) = workoutDao.insertWorkouts(workouts)
    
    suspend fun updateWorkout(workout: Workout) = workoutDao.updateWorkout(workout)
    
    suspend fun deleteWorkout(workout: Workout) = workoutDao.deleteWorkout(workout)
    
    // Exercise operations
    fun getExercisesForWorkout(workoutId: String): Flow<List<Exercise>> = 
        exerciseDao?.getExercisesForWorkout(workoutId) ?: throw IllegalStateException("ExerciseDao not initialized")
    
    suspend fun getExercisesForWorkoutSuspend(workoutId: String): List<Exercise> = 
        exerciseDao?.getExercisesForWorkoutSuspend(workoutId) ?: throw IllegalStateException("ExerciseDao not initialized")
    
    suspend fun insertExercise(exercise: Exercise) = 
        exerciseDao?.insertExercise(exercise) ?: throw IllegalStateException("ExerciseDao not initialized")
    
    suspend fun insertExercises(exercises: List<Exercise>) = 
        exerciseDao?.insertExercises(exercises) ?: throw IllegalStateException("ExerciseDao not initialized")
    
    suspend fun updateExercise(exercise: Exercise) = 
        exerciseDao?.updateExercise(exercise) ?: throw IllegalStateException("ExerciseDao not initialized")
    
    suspend fun deleteExercise(exercise: Exercise) = 
        exerciseDao?.deleteExercise(exercise) ?: throw IllegalStateException("ExerciseDao not initialized")
    
    /**
     * Forces refresh of predefined workouts - clears all existing and reseeds
     */
    suspend fun refreshPredefinedWorkouts() {
        // Clear all predefined workouts efficiently
        workoutDao.deleteAllPredefinedWorkouts()
        
        // Debug: Log that we're refreshing workouts
        println("🔄 Refreshing workout database with properly categorized workouts...")
        
        val predefinedWorkouts = listOf(
            // CARDIO Workouts
            Workout(
                name = "Cardio Burn",
                description = "High-energy cardio session to burn calories fast",
                category = WorkoutCategory.CARDIO,
                difficulty = WorkoutDifficulty.INTERMEDIATE,
                durationMinutes = 35,
                estimatedCalories = 400,
                exerciseCount = 6,
                rating = 4.5,
                isCustom = false
            ),
            Workout(
                name = "Fat Burning Cardio",
                description = "Intense cardio workout for maximum fat burn",
                category = WorkoutCategory.CARDIO,
                difficulty = WorkoutDifficulty.ADVANCED,
                durationMinutes = 45,
                estimatedCalories = 520,
                exerciseCount = 8,
                rating = 4.7,
                isCustom = false
            ),
            Workout(
                name = "Beginner Cardio",
                description = "Easy cardio workout for beginners",
                category = WorkoutCategory.CARDIO,
                difficulty = WorkoutDifficulty.BEGINNER,
                durationMinutes = 20,
                estimatedCalories = 180,
                exerciseCount = 5,
                rating = 4.3,
                isCustom = false
            ),
            
            // STRENGTH Workouts
            Workout(
                name = "Strength Builder",
                description = "Build muscle and increase strength",
                category = WorkoutCategory.STRENGTH,
                difficulty = WorkoutDifficulty.INTERMEDIATE,
                durationMinutes = 50,
                estimatedCalories = 350,
                exerciseCount = 10,
                rating = 4.8,
                isCustom = false
            ),
            Workout(
                name = "Power Lifting",
                description = "Advanced strength training with heavy weights",
                category = WorkoutCategory.STRENGTH,
                difficulty = WorkoutDifficulty.ADVANCED,
                durationMinutes = 60,
                estimatedCalories = 420,
                exerciseCount = 8,
                rating = 4.9,
                isCustom = false
            ),
            Workout(
                name = "Beginner Strength",
                description = "Perfect introduction to strength training",
                category = WorkoutCategory.STRENGTH,
                difficulty = WorkoutDifficulty.BEGINNER,
                durationMinutes = 30,
                estimatedCalories = 220,
                exerciseCount = 8,
                rating = 4.6,
                isCustom = false
            ),
            
            // YOGA Workouts
            Workout(
                name = "Morning Yoga Flow",
                description = "Start your day with mindfulness and flexibility",
                category = WorkoutCategory.YOGA,
                difficulty = WorkoutDifficulty.BEGINNER,
                durationMinutes = 25,
                estimatedCalories = 120,
                exerciseCount = 12,
                rating = 4.9,
                isCustom = false
            ),
            Workout(
                name = "Power Yoga",
                description = "Dynamic yoga for strength and flexibility",
                category = WorkoutCategory.YOGA,
                difficulty = WorkoutDifficulty.INTERMEDIATE,
                durationMinutes = 45,
                estimatedCalories = 200,
                exerciseCount = 15,
                rating = 4.7,
                isCustom = false
            ),
            Workout(
                name = "Relaxing Yoga",
                description = "Gentle yoga for stress relief and relaxation",
                category = WorkoutCategory.YOGA,
                difficulty = WorkoutDifficulty.BEGINNER,
                durationMinutes = 30,
                estimatedCalories = 100,
                exerciseCount = 10,
                rating = 4.8,
                isCustom = false
            ),
            
            // HIIT Workouts
            Workout(
                name = "20-Min HIIT Blast",
                description = "High-intensity interval training for maximum results",
                category = WorkoutCategory.HIIT,
                difficulty = WorkoutDifficulty.INTERMEDIATE,
                durationMinutes = 20,
                estimatedCalories = 280,
                exerciseCount = 8,
                rating = 4.8,
                isCustom = false
            ),
            Workout(
                name = "HIIT Tabata",
                description = "4-minute intense Tabata workout",
                category = WorkoutCategory.HIIT,
                difficulty = WorkoutDifficulty.ADVANCED,
                durationMinutes = 15,
                estimatedCalories = 200,
                exerciseCount = 6,
                rating = 4.6,
                isCustom = false
            ),
            Workout(
                name = "Beginner HIIT",
                description = "Introduction to high-intensity interval training",
                category = WorkoutCategory.HIIT,
                difficulty = WorkoutDifficulty.BEGINNER,
                durationMinutes = 25,
                estimatedCalories = 250,
                exerciseCount = 7,
                rating = 4.5,
                isCustom = false
            ),
            
            // FLEXIBILITY Workouts
            Workout(
                name = "Flexibility Focus",
                description = "Improve your flexibility and range of motion",
                category = WorkoutCategory.FLEXIBILITY,
                difficulty = WorkoutDifficulty.BEGINNER,
                durationMinutes = 40,
                estimatedCalories = 150,
                exerciseCount = 15,
                rating = 4.4,
                isCustom = false
            ),
            Workout(
                name = "Deep Stretch",
                description = "Advanced stretching for improved mobility",
                category = WorkoutCategory.FLEXIBILITY,
                difficulty = WorkoutDifficulty.INTERMEDIATE,
                durationMinutes = 35,
                estimatedCalories = 120,
                exerciseCount = 12,
                rating = 4.6,
                isCustom = false
            ),
            Workout(
                name = "Quick Stretch",
                description = "Fast stretching routine for busy schedules",
                category = WorkoutCategory.FLEXIBILITY,
                difficulty = WorkoutDifficulty.BEGINNER,
                durationMinutes = 15,
                estimatedCalories = 60,
                exerciseCount = 8,
                rating = 4.2,
                isCustom = false
            )
        )
        insertWorkouts(predefinedWorkouts)
        println("✅ Successfully loaded ${predefinedWorkouts.size} properly categorized workouts!")
    }
    
    /**
     * Seeds the database with predefined workouts if empty (for initial setup)
     */
    suspend fun seedPredefinedWorkoutsIfEmpty() {
        val existingWorkouts = workoutDao.getPreDefinedWorkouts().first()
        if (existingWorkouts.isEmpty()) {
            refreshPredefinedWorkouts()
        }
    }
}
