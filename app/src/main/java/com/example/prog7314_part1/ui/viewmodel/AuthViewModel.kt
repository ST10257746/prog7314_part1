package com.example.prog7314_part1.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.prog7314_part1.data.local.entity.User
import com.example.prog7314_part1.data.model.AuthState
import com.example.prog7314_part1.data.model.Result
import com.example.prog7314_part1.data.repository.ApiUserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * AuthViewModel
 * Manages authentication state and user operations
 * NOW USING REST API via ApiUserRepository
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = ApiUserRepository(application)
    
    // Authentication state
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    // Current user
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Error message
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // Success message
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    init {
        checkAuthState()
        observeCurrentUser()
    }
    
    /**
     * Check current authentication state
     */
    private fun checkAuthState() {
        viewModelScope.launch {
            repository.observeAuthState().collect { state ->
                _authState.value = state
            }
        }
    }
    
    /**
     * Observe current user changes
     */
    private fun observeCurrentUser() {
        viewModelScope.launch {
            repository.getCurrentUserFlow().collect { user ->
                _currentUser.value = user
            }
        }
    }
    
    /**
     * Register new user
     */
    fun register(
        email: String,
        password: String,
        confirmPassword: String,
        displayName: String,
        age: Int,
        weightKg: Double
    ) {
        // Validation
        val validationError = validateRegistration(email, password, confirmPassword, displayName, age, weightKg)
        if (validationError != null) {
            _errorMessage.value = validationError
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            when (val result = repository.register(email, password, displayName, age, weightKg)) {
                is Result.Success -> {
                    _currentUser.value = result.data
                    _successMessage.value = "Registration successful!"
                    _isLoading.value = false
                }
                is Result.Error -> {
                    _errorMessage.value = result.message ?: "Registration failed"
                    _isLoading.value = false
                }
                Result.Loading -> {
                    _isLoading.value = true
                }
            }
        }
    }
    
    /**
     * Login user
     */
    fun login(email: String, password: String) {
        // Validation
        val validationError = validateLogin(email, password)
        if (validationError != null) {
            _errorMessage.value = validationError
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            when (val result = repository.login(email, password)) {
                is Result.Success -> {
                    _currentUser.value = result.data
                    _successMessage.value = "Login successful!"
                    _isLoading.value = false
                }
                is Result.Error -> {
                    _errorMessage.value = result.message ?: "Login failed"
                    _isLoading.value = false
                }
                Result.Loading -> {
                    _isLoading.value = true
                }
            }
        }
    }
    
    /**
     * Logout user
     */
    fun logout() {
        viewModelScope.launch {
            _isLoading.value = true
            
            when (val result = repository.logout()) {
                is Result.Success -> {
                    _currentUser.value = null
                    _successMessage.value = "Logged out successfully"
                    _isLoading.value = false
                }
                is Result.Error -> {
                    _errorMessage.value = result.message ?: "Logout failed"
                    _isLoading.value = false
                }
                Result.Loading -> {
                    _isLoading.value = true
                }
            }
        }
    }
    
    /**
     * Update user profile
     */
    fun updateUser(user: User) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            when (val result = repository.updateUser(user)) {
                is Result.Success -> {
                    _currentUser.value = result.data
                    _successMessage.value = "Profile updated successfully!"
                    _isLoading.value = false
                }
                is Result.Error -> {
                    _errorMessage.value = result.message ?: "Update failed"
                    _isLoading.value = false
                }
                Result.Loading -> {
                    _isLoading.value = true
                }
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
    
    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _successMessage.value = null
    }
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean = repository.isLoggedIn()
    
    // ========== Validation Methods ==========
    
    private fun validateRegistration(
        email: String,
        password: String,
        confirmPassword: String,
        displayName: String,
        age: Int,
        weightKg: Double
    ): String? {
        if (displayName.isBlank()) {
            return "Please enter your name"
        }
        
        if (email.isBlank()) {
            return "Please enter your email"
        }
        
        if (!isValidEmail(email)) {
            return "Please enter a valid email address"
        }
        
        if (password.isBlank()) {
            return "Please enter a password"
        }
        
        if (password.length < 6) {
            return "Password must be at least 6 characters"
        }
        
        if (password != confirmPassword) {
            return "Passwords do not match"
        }
        
        if (age < 13 || age > 100) {
            return "Please enter a valid age (13-100)"
        }
        
        if (weightKg < 30 || weightKg > 200) {
            return "Please enter a valid weight (30-200 kg)"
        }
        
        return null
    }
    
    private fun validateLogin(email: String, password: String): String? {
        if (email.isBlank()) {
            return "Please enter your email"
        }
        
        if (!isValidEmail(email)) {
            return "Please enter a valid email address"
        }
        
        if (password.isBlank()) {
            return "Please enter your password"
        }
        
        return null
    }
    
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
