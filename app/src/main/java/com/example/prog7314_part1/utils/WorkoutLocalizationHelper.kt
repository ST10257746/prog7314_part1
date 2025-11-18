package com.example.prog7314_part1.utils

import android.content.Context
import com.example.prog7314_part1.R
import com.example.prog7314_part1.data.local.entity.Workout
import com.example.prog7314_part1.data.local.entity.WorkoutCategory

/**
 * Helper class to get localized workout names and descriptions
 */
object WorkoutLocalizationHelper {
    
    /**
     * Get localized workout name based on workout ID
     * Falls back to original name if not found
     */
    fun getLocalizedWorkoutName(context: Context, workout: Workout): String {
        val resourceName = getWorkoutNameResourceId(workout.workoutId)
        return try {
            val resourceId = context.resources.getIdentifier(resourceName, "string", context.packageName)
            if (resourceId != 0) {
                context.getString(resourceId)
            } else {
                workout.name // Fallback to original name
            }
        } catch (e: Exception) {
            workout.name // Fallback to original name
        }
    }
    
    /**
     * Get localized workout description based on workout ID
     * Falls back to original description if not found
     */
    fun getLocalizedWorkoutDescription(context: Context, workout: Workout): String {
        val resourceName = getWorkoutDescriptionResourceId(workout.workoutId)
        return try {
            val resourceId = context.resources.getIdentifier(resourceName, "string", context.packageName)
            if (resourceId != 0) {
                context.getString(resourceId)
            } else {
                workout.description // Fallback to original description
            }
        } catch (e: Exception) {
            workout.description // Fallback to original description
        }
    }
    
    /**
     * Get localized category description
     */
    fun getLocalizedCategoryDescription(context: Context, category: WorkoutCategory?): String {
        val resourceName = when (category) {
            WorkoutCategory.CARDIO -> "category_desc_cardio"
            WorkoutCategory.STRENGTH -> "category_desc_strength"
            WorkoutCategory.YOGA -> "category_desc_yoga"
            WorkoutCategory.HIIT -> "category_desc_hiit"
            WorkoutCategory.FLEXIBILITY -> "category_desc_flexibility"
            WorkoutCategory.FULL_BODY -> "category_desc_full_body"
            WorkoutCategory.UPPER_BODY -> "category_desc_upper_body"
            WorkoutCategory.LOWER_BODY -> "category_desc_lower_body"
            WorkoutCategory.CORE -> "category_desc_core"
            null -> "category_desc_all"
        }
        
        return try {
            val resourceId = context.resources.getIdentifier(resourceName, "string", context.packageName)
            if (resourceId != 0) {
                context.getString(resourceId)
            } else {
                getDefaultCategoryDescription(category)
            }
        } catch (e: Exception) {
            getDefaultCategoryDescription(category)
        }
    }
    
    /**
     * Map workout ID to string resource name for workout name
     */
    private fun getWorkoutNameResourceId(workoutId: String): String {
        return when (workoutId) {
            "1" -> "workout_cardio_burn"
            "2" -> "workout_fat_burning_cardio"
            "3" -> "workout_beginner_cardio"
            "4" -> "workout_dance_cardio"
            "5" -> "workout_running_intervals"
            "6" -> "workout_strength_builder"
            "7" -> "workout_power_lifting"
            "8" -> "workout_beginner_strength"
            "9" -> "workout_upper_body_blast"
            "10" -> "workout_lower_body_power"
            "11" -> "workout_morning_yoga_flow"
            "12" -> "workout_power_yoga"
            "13" -> "workout_relaxing_yoga"
            "14" -> "workout_hot_yoga_challenge"
            "15" -> "workout_20min_hiit_blast"
            "16" -> "workout_hiit_tabata"
            "17" -> "workout_beginner_hiit"
            "18" -> "workout_full_body_hiit"
            "19" -> "workout_flexibility_focus"
            "20" -> "workout_deep_stretch"
            "21" -> "workout_quick_stretch"
            "22" -> "workout_post_workout_recovery"
            "23" -> "workout_core_crusher"
            "24" -> "workout_beginner_core"
            "25" -> "workout_advanced_ab_shredder"
            else -> "" // Custom workouts or unknown IDs
        }
    }
    
    /**
     * Map workout ID to string resource name for workout description
     */
    private fun getWorkoutDescriptionResourceId(workoutId: String): String {
        return when (workoutId) {
            "1" -> "workout_desc_cardio_burn"
            "2" -> "workout_desc_fat_burning_cardio"
            "3" -> "workout_desc_beginner_cardio"
            "4" -> "workout_desc_dance_cardio"
            "5" -> "workout_desc_running_intervals"
            "6" -> "workout_desc_strength_builder"
            "7" -> "workout_desc_power_lifting"
            "8" -> "workout_desc_beginner_strength"
            "9" -> "workout_desc_upper_body_blast"
            "10" -> "workout_desc_lower_body_power"
            "11" -> "workout_desc_morning_yoga_flow"
            "12" -> "workout_desc_power_yoga"
            "13" -> "workout_desc_relaxing_yoga"
            "14" -> "workout_desc_hot_yoga_challenge"
            "15" -> "workout_desc_20min_hiit_blast"
            "16" -> "workout_desc_hiit_tabata"
            "17" -> "workout_desc_beginner_hiit"
            "18" -> "workout_desc_full_body_hiit"
            "19" -> "workout_desc_flexibility_focus"
            "20" -> "workout_desc_deep_stretch"
            "21" -> "workout_desc_quick_stretch"
            "22" -> "workout_desc_post_workout_recovery"
            "23" -> "workout_desc_core_crusher"
            "24" -> "workout_desc_beginner_core"
            "25" -> "workout_desc_advanced_ab_shredder"
            else -> "" // Custom workouts or unknown IDs
        }
    }
    
    /**
     * Default category descriptions (fallback)
     */
    private fun getDefaultCategoryDescription(category: WorkoutCategory?): String {
        return when (category) {
            WorkoutCategory.CARDIO -> "Burn calories and improve endurance"
            WorkoutCategory.STRENGTH -> "Build muscle and power"
            WorkoutCategory.YOGA -> "Improve flexibility and mindfulness"
            WorkoutCategory.HIIT -> "High-intensity interval training"
            WorkoutCategory.FLEXIBILITY -> "Improve mobility and range of motion"
            WorkoutCategory.FULL_BODY -> "Complete body conditioning"
            WorkoutCategory.UPPER_BODY -> "Focus on arms, chest, and back"
            WorkoutCategory.LOWER_BODY -> "Strengthen legs and glutes"
            WorkoutCategory.CORE -> "Build core strength and stability"
            null -> "All available workouts"
        }
    }
}

