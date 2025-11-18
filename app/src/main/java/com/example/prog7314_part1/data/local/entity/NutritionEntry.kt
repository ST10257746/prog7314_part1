package com.example.prog7314_part1.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * NutritionEntry entity for Room Database
 * Tracks meals and nutrition intake
 */
@Entity(
    tableName = "nutrition_entries",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["userId"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userId"), Index("date"), Index("mealType")]
)
data class NutritionEntry(
    @PrimaryKey(autoGenerate = true)
    val entryId: Long = 0,
    
    val userId: String,
    val date: String,  // "yyyy-MM-dd"
    val mealType: MealType,
    val time: String,  // "HH:mm"
    
    // Food Details
    val foodName: String,
    val description: String? = null,
    val servingSize: String,
    
    // Macros
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatsG: Double,
    
    // Optional Nutrients
    val fiberG: Double = 0.0,
    val sugarG: Double = 0.0,
    val sodiumMg: Double = 0.0,
    
    val imageUrl: String? = null,
    
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val firebaseId: String? = null  // Firebase document ID for API deletion
)

enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK
}
