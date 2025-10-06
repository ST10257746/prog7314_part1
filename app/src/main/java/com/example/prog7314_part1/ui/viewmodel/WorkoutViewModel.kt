package com.example.prog7314_part1.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prog7314_part1.data.local.entity.Workout
import com.example.prog7314_part1.data.local.entity.WorkoutCategory
import com.example.prog7314_part1.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider

/**
 * ViewModel for managing workout library state and filtering
 */
class WorkoutViewModel(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {
    
    // Filter state
    private val _selectedCategory = MutableStateFlow<WorkoutCategory?>(null)
    val selectedCategory: StateFlow<WorkoutCategory?> = _selectedCategory.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Filtered workouts based on current filters
    private val _workouts = MutableStateFlow<List<Workout>>(emptyList())
    val workouts: StateFlow<List<Workout>> = _workouts.asStateFlow()
    
    init {
        // Workouts are now hardcoded directly in the database callback
        // No need to seed here as database handles it automatically
        
        // Observe filter changes and update workouts
        viewModelScope.launch {
            combine(_selectedCategory, _searchQuery) { category, query ->
                Pair(category, query)
            }.collect { (category, query) ->
                val flow = when {
                    query.isNotBlank() -> workoutRepository.searchWorkouts(query)
                    category != null -> workoutRepository.getWorkoutsByCategory(category)
                    else -> workoutRepository.getAllWorkouts()
                }
                flow.collect { workoutList ->
                    _workouts.value = workoutList
                }
            }
        }
    }
    
    /**
     * Set the selected category filter
     */
    fun setSelectedCategory(category: WorkoutCategory?) {
        _selectedCategory.value = category
        // Clear search when category changes
        if (category != null) {
            _searchQuery.value = ""
        }
    }
    
    /**
     * Set the search query
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        // Clear category when searching
        if (query.isNotBlank()) {
            _selectedCategory.value = null
        }
    }
    
    /**
     * Clear all filters
     */
    fun clearFilters() {
        _selectedCategory.value = null
        _searchQuery.value = ""
    }
    
    /**
     * Get workout by ID
     */
    suspend fun getWorkoutById(workoutId: String): Workout? {
        return workoutRepository.getWorkoutById(workoutId)
    }
}

/**
 * Factory for creating WorkoutViewModel instances
 */
class WorkoutViewModelFactory(
    private val workoutRepository: WorkoutRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutViewModel::class.java)) {
            return WorkoutViewModel(workoutRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
