package com.example.prog7314_part1.ui.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prog7314_part1.data.local.entity.MealType
import com.example.prog7314_part1.data.local.entity.NutritionEntry
import com.example.prog7314_part1.data.local.entity.User
import com.example.prog7314_part1.data.model.Result
import com.example.prog7314_part1.data.repository.ApiUserRepository
import com.example.prog7314_part1.data.repository.DailyActivityRepository
import com.example.prog7314_part1.data.repository.NutritionRepository
import com.example.prog7314_part1.data.repository.NutritionSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class NutritionState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val todayEntries: List<NutritionEntry> = emptyList(),
    val selectedDateEntries: List<NutritionEntry> = emptyList(),
    val nutritionSummary: NutritionSummary = NutritionSummary(0, 0.0, 0.0, 0.0),
    val waterGlasses: Int = 0, // Current water intake - start from 0
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * NutritionViewModel
 * Manages nutrition tracking state and operations
 */
class NutritionViewModel(
    private val nutritionRepository: NutritionRepository,
    private val userRepository: ApiUserRepository,
    private val dailyActivityRepository: DailyActivityRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(NutritionState())
    val state: StateFlow<NutritionState> = _state.asStateFlow()
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    init {
        loadData()
    }
    
    /**
     * Load nutrition data for today
     */
    fun loadData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            
            try {
                // Get current user
                val user = userRepository.getCurrentUserSuspend()
                if (user == null) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = "Please log in to view nutrition data"
                    )
                    return@launch
                }
                
                // Sync nutrition entries from Firebase
                nutritionRepository.refreshTodayEntries(user.userId)
                
                // Combine user data with nutrition entries and daily activity for today
                combine(
                    userRepository.getCurrentUser(),
                    nutritionRepository.getEntriesForToday(user.userId),
                    dailyActivityRepository.getTodayActivity(user.userId)
                ) { currentUser, entries, dailyActivity ->
                    Triple(currentUser, entries, dailyActivity)
                }.collect { (currentUser, entries, dailyActivity) ->
                    if (currentUser != null) {
                        // Get nutrition summary
                        val summary = nutritionRepository.getNutritionSummaryForToday(currentUser.userId)
                        
                        _state.value = _state.value.copy(
                            isLoading = false,
                            user = currentUser,
                            todayEntries = entries,
                            nutritionSummary = summary,
                            waterGlasses = dailyActivity?.waterGlasses ?: 0,
                            errorMessage = null
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load nutrition data: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Add a new nutrition entry
     */
    fun addNutritionEntry(
        mealType: MealType,
        foodName: String,
        description: String? = null,
        servingSize: String,
        calories: Int,
        proteinG: Double,
        carbsG: Double,
        fatsG: Double
    ) {
        viewModelScope.launch {
            val user = _state.value.user ?: return@launch
            
            _state.value = _state.value.copy(isLoading = true)
            
            when (val result = nutritionRepository.addNutritionEntry(
                userId = user.userId,
                mealType = mealType,
                foodName = foodName,
                description = description,
                servingSize = servingSize,
                calories = calories,
                proteinG = proteinG,
                carbsG = carbsG,
                fatsG = fatsG
            )) {
                is Result.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        successMessage = "Meal logged successfully!"
                    )
                    // Data will be automatically updated through the Flow
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = result.message ?: "Failed to add nutrition entry"
                    )
                }
                else -> {}
            }
        }
    }
    
    /**
     * Delete a nutrition entry
     */
    fun deleteNutritionEntry(entry: NutritionEntry) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            when (val result = nutritionRepository.deleteNutritionEntry(entry)) {
                is Result.Success -> {
                    // The Flow should automatically update, but ensure we refresh the data
                    // Remove the entry from current state immediately for instant UI feedback
                    val updatedEntries = _state.value.todayEntries.filter { it.entryId != entry.entryId }
                    _state.value = _state.value.copy(
                        isLoading = false,
                        todayEntries = updatedEntries,
                        successMessage = "Entry deleted successfully!"
                    )
                    // Recalculate summary with updated entries
                    val user = userRepository.getCurrentUserSuspend()
                    if (user != null) {
                        val summary = nutritionRepository.getNutritionSummaryForToday(user.userId)
                        _state.value = _state.value.copy(nutritionSummary = summary)
                    }
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = result.message ?: "Failed to delete entry"
                    )
                }
                else -> {}
            }
        }
    }
    
    /**
     * Add a glass of water (syncs to Firebase)
     */
    fun addWaterGlass() {
        viewModelScope.launch {
            val user = _state.value.user ?: return@launch
            val currentGlasses = _state.value.waterGlasses
            val maxGlasses = user.dailyWaterGoal ?: 8
            
            if (currentGlasses < maxGlasses) {
                // Use addWaterGlasses which syncs to Firebase
                when (val result = dailyActivityRepository.addWaterGlasses(user.userId, 1)) {
                    is Result.Success -> {
                        _state.value = _state.value.copy(
                            waterGlasses = currentGlasses + 1,
                            successMessage = "Water glass added! Keep hydrated!"
                        )
                    }
                    is Result.Error -> {
                        _state.value = _state.value.copy(
                            errorMessage = result.message ?: "Failed to save water intake"
                        )
                    }
                    else -> {}
                }
            } else {
                _state.value = _state.value.copy(
                    successMessage = "Great! You've reached your daily water goal!"
                )
            }
        }
    }
    
    /**
     * Remove a glass of water (syncs to Firebase)
     */
    fun removeWaterGlass() {
        viewModelScope.launch {
            val user = _state.value.user ?: return@launch
            val currentGlasses = _state.value.waterGlasses
            
            if (currentGlasses > 0) {
                // Use addWaterGlasses with -1 which syncs to Firebase
                when (val result = dailyActivityRepository.addWaterGlasses(user.userId, -1)) {
                    is Result.Success -> {
                        _state.value = _state.value.copy(waterGlasses = currentGlasses - 1)
                    }
                    is Result.Error -> {
                        _state.value = _state.value.copy(
                            errorMessage = result.message ?: "Failed to save water intake"
                        )
                    }
                    else -> {}
                }
            }
        }
    }
    
    /**
     * Add sample nutrition entries for demo
     */
    fun addSampleEntries() {
        viewModelScope.launch {
            val user = _state.value.user ?: return@launch
            
            _state.value = _state.value.copy(isLoading = true)
            
            when (val result = nutritionRepository.addSampleEntries(user.userId)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        successMessage = "Sample meals added!"
                    )
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = result.message ?: "Failed to add sample entries"
                    )
                }
                else -> {}
            }
        }
    }
    
    /**
     * Get entries for a specific meal type
     */
    fun getEntriesForMeal(mealType: MealType): List<NutritionEntry> {
        return _state.value.todayEntries.filter { it.mealType == mealType }
    }
    
    /**
     * Calculate calories for a specific meal type
     */
    fun getCaloriesForMeal(mealType: MealType): Int {
        return getEntriesForMeal(mealType).sumOf { it.calories }
    }
    
    /**
     * Calculate macros for a specific meal type
     */
    fun getMacrosForMeal(mealType: MealType): Triple<Double, Double, Double> {
        val entries = getEntriesForMeal(mealType)
        val protein = entries.sumOf { it.proteinG }
        val carbs = entries.sumOf { it.carbsG }
        val fats = entries.sumOf { it.fatsG }
        return Triple(protein, carbs, fats)
    }
    
    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _state.value = _state.value.copy(successMessage = null)
    }
    
    /**
     * Clear error message
     */
    fun clearErrorMessage() {
        _state.value = _state.value.copy(errorMessage = null)
    }
    
    /**
     * Load nutrition data for a specific date
     */
    fun loadDataForDate(date: String) {
        viewModelScope.launch {
            val user = _state.value.user ?: return@launch
            
            _state.value = _state.value.copy(isLoading = true)
            
            try {
                // Get entries for the specific date
                nutritionRepository.getEntriesForDate(user.userId, date).collect { entries ->
                    // Get summary for the specific date
                    val summary = nutritionRepository.getNutritionSummaryForDate(user.userId, date)
                    
                    _state.value = _state.value.copy(
                        isLoading = false,
                        selectedDateEntries = entries,
                        nutritionSummary = summary
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load data for selected date"
                )
            }
        }
    }
}
