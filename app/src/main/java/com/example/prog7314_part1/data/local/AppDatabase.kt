package com.example.prog7314_part1.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.prog7314_part1.data.local.dao.*
import com.example.prog7314_part1.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * FitTrackr Room Database
 * Local SQLite database for offline-first architecture
 */
@Database(
    entities = [
        User::class,
        DailyActivity::class,
        Workout::class,
        WorkoutSession::class,
        Exercise::class,
        NutritionEntry::class,
        Goal::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    // Abstract DAO methods
    abstract fun userDao(): UserDao
    abstract fun dailyActivityDao(): DailyActivityDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun nutritionEntryDao(): NutritionEntryDao
    abstract fun goalDao(): GoalDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        private const val DATABASE_NAME = "fittrackr_database"
        
        /**
         * Get database instance using singleton pattern
         * Thread-safe implementation
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration() // For development only
                    .allowMainThreadQueries() // For development only - remove in production
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Hardcode workouts directly into database on creation
                CoroutineScope(Dispatchers.IO).launch {
                    populateDatabase(INSTANCE!!)
                }
            }
            
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Ensure our hardcoded workouts exist (queries will filter to show only these)
                CoroutineScope(Dispatchers.IO).launch {
                    INSTANCE?.let { database ->
                        // Just populate our hardcoded workouts (queries filter out others)
                        populateDatabase(database)
                    }
                }
            }
        }
        
        private suspend fun populateDatabase(database: AppDatabase) {
            val workoutDao = database.workoutDao()
            
            println("🎯 Loading all 25 hardcoded workouts into database")
            
            val hardcodedWorkouts = listOf(
                // CARDIO Workouts (5 workouts)
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
            
            workoutDao.insertWorkouts(hardcodedWorkouts)
            println("✅ Successfully loaded ${hardcodedWorkouts.size} workouts: 5 Cardio, 5 Strength, 4 Yoga, 4 HIIT, 4 Flexibility, 3 Core")
        }
        /**
         * Clear database instance (useful for testing or logout)
         */
        fun clearInstance() {
            INSTANCE = null
        }
    }
}