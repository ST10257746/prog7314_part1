package com.example.prog7314_part1.ui.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.prog7314_part1.data.repository.DailyActivityRepository
import com.example.prog7314_part1.data.repository.NutritionRepository
import com.example.prog7314_part1.data.repository.UserRepository

/**
 * ViewModelFactory for NutritionViewModel
 */
class NutritionViewModelFactory(
    private val nutritionRepository: NutritionRepository,
    private val userRepository: UserRepository,
    private val dailyActivityRepository: DailyActivityRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NutritionViewModel::class.java)) {
            return NutritionViewModel(nutritionRepository, userRepository, dailyActivityRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
