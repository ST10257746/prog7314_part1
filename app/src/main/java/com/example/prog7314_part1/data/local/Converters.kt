package com.example.prog7314_part1.data.local

import androidx.room.TypeConverter
import com.example.prog7314_part1.data.local.entity.*

/**
 * Type Converters for Room Database
 * Handles conversion of enums to/from database-compatible types
 */
class Converters {
    
    // WorkoutCategory Converters
    @TypeConverter
    fun fromWorkoutCategory(value: WorkoutCategory): String = value.name
    
    @TypeConverter
    fun toWorkoutCategory(value: String): WorkoutCategory = WorkoutCategory.valueOf(value)
    
    // WorkoutDifficulty Converters
    @TypeConverter
    fun fromWorkoutDifficulty(value: WorkoutDifficulty): String = value.name
    
    @TypeConverter
    fun toWorkoutDifficulty(value: String): WorkoutDifficulty = WorkoutDifficulty.valueOf(value)
    
    // SessionStatus Converters
    @TypeConverter
    fun fromSessionStatus(value: SessionStatus): String = value.name
    
    @TypeConverter
    fun toSessionStatus(value: String): SessionStatus = SessionStatus.valueOf(value)
    
    // MealType Converters
    @TypeConverter
    fun fromMealType(value: MealType): String = value.name
    
    @TypeConverter
    fun toMealType(value: String): MealType = MealType.valueOf(value)
}