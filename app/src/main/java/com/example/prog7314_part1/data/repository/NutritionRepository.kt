package com.example.prog7314_part1.data.repository

import android.content.Context
import com.example.prog7314_part1.data.local.AppDatabase
import com.example.prog7314_part1.data.local.dao.NutritionEntryDao
import com.example.prog7314_part1.data.local.entity.NutritionEntry
import com.example.prog7314_part1.data.local.entity.MealType
import com.example.prog7314_part1.data.model.Result
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

/**
 * NutritionRepository
 * Handles nutrition entry data operations with REST API sync
 */
class NutritionRepository(context: Context) {
    
    private val database = AppDatabase.getDatabase(context)
    private val nutritionDao: NutritionEntryDao = database.nutritionEntryDao()
    private val networkRepo = NetworkRepository(context)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    /**
     * Add a new nutrition entry (with API sync)
     */
    suspend fun addNutritionEntry(
        userId: String,
        mealType: MealType,
        foodName: String,
        description: String? = null,
        servingSize: String,
        calories: Int,
        proteinG: Double,
        carbsG: Double,
        fatsG: Double,
        fiberG: Double = 0.0,
        sugarG: Double = 0.0,
        sodiumMg: Double = 0.0,
        imageUrl: String? = null
    ): Result<NutritionEntry> {
        return try {
            val currentTime = System.currentTimeMillis()
            val date = dateFormat.format(Date(currentTime))
            val time = timeFormat.format(Date(currentTime))
            
            val entry = NutritionEntry(
                userId = userId,
                date = date,
                mealType = mealType,
                time = time,
                foodName = foodName,
                description = description,
                servingSize = servingSize,
                calories = calories,
                proteinG = proteinG,
                carbsG = carbsG,
                fatsG = fatsG,
                fiberG = fiberG,
                sugarG = sugarG,
                sodiumMg = sodiumMg,
                imageUrl = imageUrl,
                createdAt = currentTime
            )
            
            // Step 1: Save to local RoomDB (offline support)
            nutritionDao.insertEntry(entry)
            
            // Step 2: Sync to Firebase via REST API
            val nutritionRequest = com.example.prog7314_part1.data.network.model.CreateNutritionRequest(
                foodName = foodName,
                mealType = mealType.name,
                servingSize = servingSize,
                calories = calories,
                proteinG = proteinG,
                carbsG = carbsG,
                fatsG = fatsG,
                fiberG = fiberG,
                sugarG = sugarG,
                notes = description ?: "",
                timestamp = currentTime
            )
            
            when (val apiResult = networkRepo.createNutrition(nutritionRequest)) {
                is Result.Success -> {
                    android.util.Log.d("NutritionRepo", "✅ Nutrition entry synced to Firebase")
                }
                is Result.Error -> {
                    android.util.Log.w("NutritionRepo", "⚠️ API sync failed: ${apiResult.message}")
                    // Still return success because local save worked (offline mode)
                }
                else -> {}
            }
            
            Result.Success(entry)
        } catch (e: Exception) {
            Result.Error(e, "Failed to add nutrition entry")
        }
    }
    
    /**
     * Update an existing nutrition entry
     */
    suspend fun updateNutritionEntry(entry: NutritionEntry): Result<Unit> {
        return try {
            nutritionDao.updateEntry(entry)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to update nutrition entry")
        }
    }
    
    /**
     * Delete a nutrition entry (with API sync)
     */
    suspend fun deleteNutritionEntry(entry: NutritionEntry): Result<Unit> {
        return try {
            // Step 1: Delete from local RoomDB
            nutritionDao.deleteEntry(entry)
            
            // Step 2: Delete from Firebase via REST API
            // Note: We would need the Firebase document ID to delete from API
            // For now, we'll just delete locally and log the sync attempt
            android.util.Log.d("NutritionRepo", "✅ Nutrition entry deleted locally (API sync coming soon)")
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to delete nutrition entry")
        }
    }
    
    /**
     * Get all nutrition entries for a specific date
     */
    fun getEntriesForDate(userId: String, date: String): Flow<List<NutritionEntry>> {
        return nutritionDao.getEntriesForDate(userId, date)
    }
    
    /**
     * Sync nutrition entries from Firebase to local DB
     */
    suspend fun syncNutritionFromFirebase(userId: String, date: String? = null) {
        try {
            when (val result = networkRepo.getNutritionEntries(userId, date)) {
                is Result.Success -> {
                    // Convert DTOs to entities and save to local DB
                    val entries = result.data.map { dto ->
                        NutritionEntry(
                            userId = dto.userId,
                            date = dateFormat.format(Date(dto.timestamp)),
                            time = timeFormat.format(Date(dto.timestamp)),
                            mealType = MealType.valueOf(dto.mealType),
                            foodName = dto.foodName,
                            description = dto.notes,
                            servingSize = dto.servingSize,
                            calories = dto.calories,
                            proteinG = dto.proteinG,
                            carbsG = dto.carbsG,
                            fatsG = dto.fatsG,
                            fiberG = dto.fiberG ?: 0.0,
                            sugarG = dto.sugarG ?: 0.0,
                            sodiumMg = 0.0,
                            imageUrl = null,
                            createdAt = dto.createdAt ?: dto.timestamp,
                            isSynced = true
                        )
                    }
                    
                    // Clear existing entries for the date and insert new ones
                    if (date != null) {
                        nutritionDao.deleteEntriesForDate(userId, date)
                    }
                    entries.forEach { nutritionDao.insertEntry(it) }
                    
                    android.util.Log.d("NutritionRepo", "✅ Synced ${entries.size} nutrition entries from Firebase")
                }
                is Result.Error -> {
                    android.util.Log.w("NutritionRepo", "⚠️ Failed to sync from Firebase: ${result.message}")
                }
                else -> {}
            }
        } catch (e: Exception) {
            android.util.Log.e("NutritionRepo", "❌ Sync error: ${e.message}", e)
        }
    }
    
    /**
     * Get entries for today (triggers background sync)
     */
    fun getEntriesForToday(userId: String): Flow<List<NutritionEntry>> {
        val today = dateFormat.format(Date())
        return nutritionDao.getEntriesForDate(userId, today)
    }
    
    /**
     * Trigger manual sync (call from ViewModel)
     */
    suspend fun refreshTodayEntries(userId: String) {
        syncNutritionFromFirebase(userId, dateFormat.format(Date()))
    }
    
    /**
     * Get entries for a specific meal type on a specific date
     */
    fun getEntriesForMeal(userId: String, date: String, mealType: MealType): Flow<List<NutritionEntry>> {
        return nutritionDao.getEntriesForMeal(userId, date, mealType)
    }
    
    /**
     * Get total calories for a specific date
     */
    suspend fun getTotalCaloriesForDate(userId: String, date: String): Int {
        return nutritionDao.getTotalCaloriesForDate(userId, date) ?: 0
    }
    
    /**
     * Get total calories for today
     */
    suspend fun getTotalCaloriesForToday(userId: String): Int {
        val today = dateFormat.format(Date())
        return getTotalCaloriesForDate(userId, today)
    }
    
    /**
     * Get total protein for a specific date
     */
    suspend fun getTotalProteinForDate(userId: String, date: String): Double {
        return nutritionDao.getTotalProteinForDate(userId, date) ?: 0.0
    }
    
    /**
     * Get total carbs for a specific date
     */
    suspend fun getTotalCarbsForDate(userId: String, date: String): Double {
        return nutritionDao.getTotalCarbsForDate(userId, date) ?: 0.0
    }
    
    /**
     * Get total fats for a specific date
     */
    suspend fun getTotalFatsForDate(userId: String, date: String): Double {
        return nutritionDao.getTotalFatsForDate(userId, date) ?: 0.0
    }
    
    /**
     * Get nutrition summary for today
     */
    suspend fun getNutritionSummaryForToday(userId: String): NutritionSummary {
        val today = dateFormat.format(Date())
        return NutritionSummary(
            totalCalories = getTotalCaloriesForDate(userId, today),
            totalProtein = getTotalProteinForDate(userId, today),
            totalCarbs = getTotalCarbsForDate(userId, today),
            totalFats = getTotalFatsForDate(userId, today)
        )
    }
    
    /**
     * Get nutrition summary for a specific date
     */
    suspend fun getNutritionSummaryForDate(userId: String, date: String): NutritionSummary {
        return NutritionSummary(
            totalCalories = getTotalCaloriesForDate(userId, date),
            totalProtein = getTotalProteinForDate(userId, date),
            totalCarbs = getTotalCarbsForDate(userId, date),
            totalFats = getTotalFatsForDate(userId, date)
        )
    }
    
    /**
     * Add sample nutrition entries for demo purposes
     */
    suspend fun addSampleEntries(userId: String): Result<Unit> {
        return try {
            val today = dateFormat.format(Date())
            
            val sampleEntries = listOf(
                NutritionEntry(
                    userId = userId,
                    date = today,
                    mealType = MealType.BREAKFAST,
                    time = "08:30",
                    foodName = "Greek Yogurt Parfait",
                    description = "Greek yogurt, granola, berries, honey",
                    servingSize = "1 cup",
                    calories = 320,
                    proteinG = 25.0,
                    carbsG = 45.0,
                    fatsG = 12.0,
                    fiberG = 5.0,
                    sugarG = 20.0
                ),
                NutritionEntry(
                    userId = userId,
                    date = today,
                    mealType = MealType.LUNCH,
                    time = "12:45",
                    foodName = "Grilled Chicken Salad",
                    description = "Mixed greens, grilled chicken, avocado, tomatoes",
                    servingSize = "1 large bowl",
                    calories = 480,
                    proteinG = 35.0,
                    carbsG = 25.0,
                    fatsG = 22.0,
                    fiberG = 8.0,
                    sugarG = 12.0
                ),
                NutritionEntry(
                    userId = userId,
                    date = today,
                    mealType = MealType.DINNER,
                    time = "19:15",
                    foodName = "Salmon with Quinoa",
                    description = "Baked salmon, quinoa, steamed broccoli, olive oil",
                    servingSize = "1 serving",
                    calories = 620,
                    proteinG = 42.0,
                    carbsG = 35.0,
                    fatsG = 28.0,
                    fiberG = 6.0,
                    sugarG = 8.0
                ),
                NutritionEntry(
                    userId = userId,
                    date = today,
                    mealType = MealType.SNACK,
                    time = "15:30",
                    foodName = "Mixed Nuts",
                    description = "Almonds, walnuts, cashews",
                    servingSize = "1 oz",
                    calories = 160,
                    proteinG = 6.0,
                    carbsG = 8.0,
                    fatsG = 14.0,
                    fiberG = 3.0,
                    sugarG = 2.0
                )
            )
            
            nutritionDao.insertEntries(sampleEntries)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to add sample entries")
        }
    }
}

/**
 * Data class for nutrition summary
 */
data class NutritionSummary(
    val totalCalories: Int,
    val totalProtein: Double,
    val totalCarbs: Double,
    val totalFats: Double
) {
    /**
     * Calculate protein calories (4 kcal per gram)
     */
    val proteinCalories: Int get() = (totalProtein * 4).toInt()
    
    /**
     * Calculate carbs calories (4 kcal per gram)
     */
    val carbsCalories: Int get() = (totalCarbs * 4).toInt()
    
    /**
     * Calculate fats calories (9 kcal per gram)
     */
    val fatsCalories: Int get() = (totalFats * 9).toInt()
    
    /**
     * Calculate protein percentage
     */
    val proteinPercentage: Float get() = if (totalCalories > 0) (proteinCalories * 100f / totalCalories) else 0f
    
    /**
     * Calculate carbs percentage
     */
    val carbsPercentage: Float get() = if (totalCalories > 0) (carbsCalories * 100f / totalCalories) else 0f
    
    /**
     * Calculate fats percentage
     */
    val fatsPercentage: Float get() = if (totalCalories > 0) (fatsCalories * 100f / totalCalories) else 0f
}
