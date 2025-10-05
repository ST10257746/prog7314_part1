package com.example.prog7314_part1.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.prog7314_part1.data.local.dao.*
import com.example.prog7314_part1.data.local.entity.*

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
    version = 1,
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
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * Clear database instance (useful for testing or logout)
         */
        fun clearInstance() {
            INSTANCE = null
        }
    }
}