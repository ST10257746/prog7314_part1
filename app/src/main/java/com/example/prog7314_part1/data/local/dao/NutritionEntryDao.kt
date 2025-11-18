package com.example.prog7314_part1.data.local.dao

import androidx.room.*
import com.example.prog7314_part1.data.local.entity.NutritionEntry
import com.example.prog7314_part1.data.local.entity.MealType
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for NutritionEntry entity
 */
@Dao
interface NutritionEntryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: NutritionEntry): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<NutritionEntry>)
    
    @Update
    suspend fun updateEntry(entry: NutritionEntry)
    
    @Delete
    suspend fun deleteEntry(entry: NutritionEntry)
    
    @Query("SELECT * FROM nutrition_entries WHERE entryId = :entryId")
    suspend fun getEntryById(entryId: Long): NutritionEntry?
    
    @Query("SELECT * FROM nutrition_entries WHERE userId = :userId AND date = :date ORDER BY time ASC")
    fun getEntriesForDate(userId: String, date: String): Flow<List<NutritionEntry>>
    
    @Query("SELECT * FROM nutrition_entries WHERE userId = :userId AND date = :date AND mealType = :mealType ORDER BY time ASC")
    fun getEntriesForMeal(userId: String, date: String, mealType: MealType): Flow<List<NutritionEntry>>
    
    @Query("SELECT * FROM nutrition_entries WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC, time ASC")
    fun getEntriesInRange(userId: String, startDate: String, endDate: String): Flow<List<NutritionEntry>>
    
    @Query("SELECT SUM(calories) FROM nutrition_entries WHERE userId = :userId AND date = :date")
    suspend fun getTotalCaloriesForDate(userId: String, date: String): Int?
    
    @Query("SELECT SUM(calories) FROM nutrition_entries WHERE userId = :userId AND date = :date")
    fun getTotalCaloriesForDateFlow(userId: String, date: String): Flow<Int?>
    
    @Query("SELECT SUM(proteinG) FROM nutrition_entries WHERE userId = :userId AND date = :date")
    suspend fun getTotalProteinForDate(userId: String, date: String): Double?
    
    @Query("SELECT SUM(carbsG) FROM nutrition_entries WHERE userId = :userId AND date = :date")
    suspend fun getTotalCarbsForDate(userId: String, date: String): Double?
    
    @Query("SELECT SUM(fatsG) FROM nutrition_entries WHERE userId = :userId AND date = :date")
    suspend fun getTotalFatsForDate(userId: String, date: String): Double?
    
    @Query("SELECT * FROM nutrition_entries WHERE userId = :userId AND isSynced = 0")
    suspend fun getUnsyncedEntries(userId: String): List<NutritionEntry>
    
    @Query("UPDATE nutrition_entries SET isSynced = 1 WHERE entryId = :entryId")
    suspend fun markAsSynced(entryId: Long)
    
    @Query("DELETE FROM nutrition_entries WHERE userId = :userId AND date = :date")
    suspend fun deleteEntriesForDate(userId: String, date: String)
    
    @Query("DELETE FROM nutrition_entries WHERE userId = :userId")
    suspend fun deleteAllEntriesForUser(userId: String)
}
