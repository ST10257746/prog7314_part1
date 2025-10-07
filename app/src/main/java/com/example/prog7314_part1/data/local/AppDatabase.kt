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
                // Seed predefined workouts only once when DB is created
                CoroutineScope(Dispatchers.IO).launch {
                    INSTANCE?.let { database ->
                        populateDatabaseIfEmpty(database)
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // No seeding needed on every app open
            }
        }

        /**
         * Populate database only if predefined workouts are not already present
         */
        private suspend fun populateDatabaseIfEmpty(database: AppDatabase) {
            val workoutDao = database.workoutDao()
            val existingPredefined = workoutDao.getPreDefinedWorkoutsSuspend()
            if (existingPredefined.isEmpty()) {
                println("🎯 Loading predefined workouts into database")
                val hardcodedWorkouts = getHardcodedWorkouts()
                workoutDao.insertWorkouts(hardcodedWorkouts)
                println("✅ Successfully loaded ${hardcodedWorkouts.size} workouts")
            } else {
                println("ℹ️ Predefined workouts already exist, skipping population")
            }
        }

        /**
         * Returns all hardcoded predefined workouts
         */
        private fun getHardcodedWorkouts(): List<Workout> = listOf(
            // CARDIO Workouts
            Workout("1", "Cardio Burn", "High-energy cardio session to burn calories fast", WorkoutCategory.CARDIO, WorkoutDifficulty.INTERMEDIATE, 35, 400, 6, 4.5,"", false),
            Workout("2", "Fat Burning Cardio", "Intense cardio workout for maximum fat burn", WorkoutCategory.CARDIO, WorkoutDifficulty.ADVANCED, 45, 520, 8, 4.7, "",false),
            Workout("3", "Beginner Cardio", "Easy cardio workout for beginners", WorkoutCategory.CARDIO, WorkoutDifficulty.BEGINNER, 20, 180, 5, 4.3, "",false),
            Workout("4", "Dance Cardio", "Fun dance moves that get your heart pumping", WorkoutCategory.CARDIO, WorkoutDifficulty.INTERMEDIATE, 30, 320, 7, 4.6,"", false),
            Workout("5", "Running Intervals", "Interval running for endurance and speed", WorkoutCategory.CARDIO, WorkoutDifficulty.ADVANCED, 40, 480, 6, 4.4,"", false),

            // STRENGTH Workouts
            Workout("6","Strength Builder", "Build muscle and increase strength", WorkoutCategory.STRENGTH, WorkoutDifficulty.INTERMEDIATE, 50, 350, 10, 4.8, "",false),
            Workout("7","Power Lifting", "Advanced strength training with heavy weights", WorkoutCategory.STRENGTH, WorkoutDifficulty.ADVANCED, 60, 420, 8, 4.9, "",false),
            Workout("8","Beginner Strength", "Perfect introduction to strength training", WorkoutCategory.STRENGTH, WorkoutDifficulty.BEGINNER, 30, 220, 8, 4.6, "",false),
            Workout("9","Upper Body Blast", "Target your arms, chest, and shoulders", WorkoutCategory.STRENGTH, WorkoutDifficulty.INTERMEDIATE, 40, 300, 9, 4.7, "",false),
            Workout("10","Lower Body Power", "Build strong legs and glutes", WorkoutCategory.STRENGTH, WorkoutDifficulty.ADVANCED, 45, 380, 10, 4.8, "",false),

            // YOGA Workouts
            Workout("11","Morning Yoga Flow", "Start your day with mindfulness and flexibility", WorkoutCategory.YOGA, WorkoutDifficulty.BEGINNER, 25, 120, 12, 4.9, "",false),
            Workout("12","Power Yoga", "Dynamic yoga for strength and flexibility", WorkoutCategory.YOGA, WorkoutDifficulty.INTERMEDIATE, 45, 200, 15, 4.7, "",false),
            Workout("13","Relaxing Yoga", "Gentle yoga for stress relief and relaxation", WorkoutCategory.YOGA, WorkoutDifficulty.BEGINNER, 30, 100, 10, 4.8, "",false),
            Workout("14","Hot Yoga Challenge", "Intense yoga practice in heated environment", WorkoutCategory.YOGA, WorkoutDifficulty.ADVANCED, 60, 350, 18, 4.5, "",false),

            // HIIT Workouts
            Workout("15","20-Min HIIT Blast", "High-intensity interval training for maximum results", WorkoutCategory.HIIT, WorkoutDifficulty.INTERMEDIATE, 20, 280, 8, 4.8, "",false),
            Workout("16","HIIT Tabata", "4-minute intense Tabata workout", WorkoutCategory.HIIT, WorkoutDifficulty.ADVANCED, 15, 200, 6, 4.6, "",false),
            Workout("17","Beginner HIIT", "Introduction to high-intensity interval training", WorkoutCategory.HIIT, WorkoutDifficulty.BEGINNER, 25, 250, 7, 4.5, "",false),
            Workout("18","Full Body HIIT", "Complete body workout with high intensity intervals", WorkoutCategory.HIIT, WorkoutDifficulty.INTERMEDIATE, 30, 350, 10, 4.7, "",false),

            // FLEXIBILITY Workouts
            Workout("19","Flexibility Focus", "Improve your flexibility and range of motion", WorkoutCategory.FLEXIBILITY, WorkoutDifficulty.BEGINNER, 40, 150, 15, 4.4, "",false),
            Workout("20","Deep Stretch", "Advanced stretching for improved mobility", WorkoutCategory.FLEXIBILITY, WorkoutDifficulty.INTERMEDIATE, 35, 120, 12, 4.6, "",false),
            Workout("21","Quick Stretch", "Fast stretching routine for busy schedules", WorkoutCategory.FLEXIBILITY, WorkoutDifficulty.BEGINNER, 15, 60, 8, 4.2, "",false),
            Workout("22","Post-Workout Recovery", "Essential stretches for muscle recovery", WorkoutCategory.FLEXIBILITY, WorkoutDifficulty.INTERMEDIATE, 25, 90, 10, 4.5, "",false),

            // CORE Workouts
            Workout("23","Core Crusher", "Intense core workout for strong abs", WorkoutCategory.CORE, WorkoutDifficulty.INTERMEDIATE, 25, 180, 8, 4.6, "",false),
            Workout("24","Beginner Core", "Build your core foundation safely", WorkoutCategory.CORE, WorkoutDifficulty.BEGINNER, 20, 120, 6, 4.4, "",false),
            Workout("25","Advanced Ab Shredder", "Elite level core training for maximum definition", WorkoutCategory.CORE, WorkoutDifficulty.ADVANCED, 35, 250, 12, 4.8, "",false)
        )

        /**
         * Clear database instance (useful for testing or logout)
         */
        fun clearInstance() {
            INSTANCE = null
        }
    }
}
