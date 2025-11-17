package com.example.prog7314_part1.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.prog7314_part1.data.local.entity.Workout
import com.example.prog7314_part1.data.local.entity.WorkoutCategory
import com.example.prog7314_part1.data.repository.WorkoutRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WorkoutViewModel(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val userId: String = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Filters
    private val _selectedCategory = MutableStateFlow<WorkoutCategory?>(null)
    val selectedCategory: StateFlow<WorkoutCategory?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Workouts list (filtered + search)
    private val _workouts = MutableStateFlow<List<Workout>>(emptyList())
    val workouts: StateFlow<List<Workout>> = _workouts.asStateFlow()

    // User-created custom workouts
    private val _userWorkouts = MutableStateFlow<List<Workout>>(emptyList())
    val userWorkouts: StateFlow<List<Workout>> = _userWorkouts.asStateFlow()

    init {
        // Observe user-created workouts
        viewModelScope.launch {
            if (userId.isNotEmpty()) {
                workoutRepository.getUserCreatedWorkouts(userId).collect { list ->
                    _userWorkouts.value = list
                }
            }
        }

        // Observe filters and search query
        viewModelScope.launch {
            combine(_selectedCategory, _searchQuery) { category, query ->
                category to query
            }.flatMapLatest { (category, query) ->
                _isLoading.value = true
                val flow = when {
                    query.isNotBlank() -> workoutRepository.searchWorkouts(userId, query)
                    category != null -> workoutRepository.getWorkoutsByCategory(userId, category)
                    else -> workoutRepository.getAllWorkouts(userId)
                }
                flow
            }.collect { list ->
                _workouts.value = list
                _isLoading.value = false
            }
        }
    }

    /** Update selected category filter */
    fun setSelectedCategory(category: WorkoutCategory?) {
        _selectedCategory.value = category
        if (category != null) _searchQuery.value = ""
    }

    /** Update search query */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isNotBlank()) _selectedCategory.value = null
    }

    /** Clear all filters */
    fun clearFilters() {
        _selectedCategory.value = null
        _searchQuery.value = ""
    }

    /** Public method to get custom workouts for the current user */
    fun getCustomWorkoutsByUser(userId: String): Flow<List<Workout>> {
        return workoutRepository.getUserCreatedWorkouts(userId)
    }
}

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
