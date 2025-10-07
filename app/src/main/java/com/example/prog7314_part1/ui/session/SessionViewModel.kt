package com.example.prog7314_part1.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prog7314_part1.data.local.dao.WorkoutSessionDao
import com.example.prog7314_part1.data.local.entity.WorkoutSession
import com.example.prog7314_part1.data.local.entity.SessionStatus
import com.example.prog7314_part1.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

data class WorkoutType(
    val name: String,
    val category: String,
    val description: String
)

data class WatchMetrics(
    val heartRate: Int = 0,
    val calories: Int = 0,
    val steps: Int = 0,
    val distanceKm: Double = 0.0,
    val activeTimeMinutes: Int = 0,
    val isConnected: Boolean = false,
    val lastUpdateTime: Long = System.currentTimeMillis()
)

data class SessionState(
    val selectedWorkoutType: WorkoutType? = null,
    val isSessionActive: Boolean = false,
    val isSessionPaused: Boolean = false,
    val elapsedTimeSeconds: Long = 0,
    val timerText: String = "00:00:00",
    val sessionId: String? = null,
    val startTime: Long? = null,
    val watchMetrics: WatchMetrics = WatchMetrics()
)

class SessionViewModel(
    private val workoutSessionDao: WorkoutSessionDao,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _sessionState = MutableStateFlow(SessionState())
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private var timerJob: Job? = null
    private var metricsJob: Job? = null

    companion object {
        val availableWorkoutTypes = listOf(
            WorkoutType("Running", "Outdoor", "Cardio exercise"),
            WorkoutType("Cycling", "Outdoor", "Cardio exercise"),
            WorkoutType("Walking", "Outdoor", "Light cardio"),
            WorkoutType("Strength", "Gym", "Weight training"),
            WorkoutType("Yoga", "Flexibility", "Mind-body practice"),
            WorkoutType("HIIT", "Cardio", "High-intensity interval training")
        )
    }

    fun selectWorkoutType(workoutType: WorkoutType) {
        _sessionState.value = _sessionState.value.copy(selectedWorkoutType = workoutType)
    }

    fun connectWatch() {
        val currentMetrics = _sessionState.value.watchMetrics
        _sessionState.value = _sessionState.value.copy(
            watchMetrics = currentMetrics.copy(isConnected = true)
        )
        startMetricsSimulation()
    }

    fun disconnectWatch() {
        metricsJob?.cancel()
        val currentMetrics = _sessionState.value.watchMetrics
        _sessionState.value = _sessionState.value.copy(
            watchMetrics = currentMetrics.copy(isConnected = false)
        )
    }

    fun startSession() {
        val currentState = _sessionState.value
        if (currentState.selectedWorkoutType == null) return

        val sessionId = UUID.randomUUID().toString()
        val startTime = System.currentTimeMillis()
        
        _sessionState.value = currentState.copy(
            isSessionActive = true,
            isSessionPaused = false,
            sessionId = sessionId,
            startTime = startTime,
            elapsedTimeSeconds = 0,
            timerText = "00:00:00"
        )

        startTimer()
    }

    fun pauseSession() {
        if (_sessionState.value.isSessionActive) {
            _sessionState.value = _sessionState.value.copy(isSessionPaused = true)
            timerJob?.cancel()
        }
    }

    fun resumeSession() {
        if (_sessionState.value.isSessionPaused) {
            _sessionState.value = _sessionState.value.copy(isSessionPaused = false)
            startTimer()
        }
    }

    fun stopSession() {
        val currentState = _sessionState.value
        if (!currentState.isSessionActive || currentState.sessionId == null) return

        timerJob?.cancel()
        metricsJob?.cancel()
        
        // Save session to database
        viewModelScope.launch {
            saveSessionToDatabase(currentState)
        }

        _sessionState.value = SessionState()
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (_sessionState.value.isSessionActive && !_sessionState.value.isSessionPaused) {
                delay(1000)
                val currentTime = _sessionState.value.elapsedTimeSeconds + 1
                val timerText = formatTime(currentTime)
                
                _sessionState.value = _sessionState.value.copy(
                    elapsedTimeSeconds = currentTime,
                    timerText = timerText
                )
            }
        }
    }

    private fun formatTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }

    private fun startMetricsSimulation() {
        metricsJob = viewModelScope.launch {
            while (_sessionState.value.watchMetrics.isConnected) {
                delay(3000) // Update metrics every 3 seconds
                
                if (_sessionState.value.isSessionActive && !_sessionState.value.isSessionPaused) {
                    updateMetrics()
                }
            }
        }
    }

    private fun updateMetrics() {
        val currentMetrics = _sessionState.value.watchMetrics
        val workoutType = _sessionState.value.selectedWorkoutType?.name ?: "General"
        
        // Simulate realistic metrics based on workout type and session duration
        val newHeartRate = simulateHeartRate(workoutType, currentMetrics.heartRate)
        val newCalories = currentMetrics.calories + calculateCalorieIncrement(workoutType)
        val newSteps = currentMetrics.steps + calculateStepIncrement(workoutType)
        val newDistance = calculateDistance(newSteps)
        val newActiveTime = (_sessionState.value.elapsedTimeSeconds / 60).toInt()
        
        _sessionState.value = _sessionState.value.copy(
            watchMetrics = currentMetrics.copy(
                heartRate = newHeartRate,
                calories = newCalories,
                steps = newSteps,
                distanceKm = newDistance,
                activeTimeMinutes = newActiveTime,
                lastUpdateTime = System.currentTimeMillis()
            )
        )
    }

    private fun simulateHeartRate(workoutType: String, currentHeartRate: Int): Int {
        val baseHeartRate = when (workoutType) {
            "Running" -> 150
            "Cycling" -> 140
            "Walking" -> 120
            "Strength" -> 130
            "Yoga" -> 90
            "HIIT" -> 160
            else -> 120
        }
        
        // Add some variation to make it realistic
        val variation = (-10..10).random()
        return maxOf(60, minOf(200, baseHeartRate + variation))
    }

    private fun calculateCalorieIncrement(workoutType: String): Int {
        val caloriesPerSecond = when (workoutType) {
            "Running" -> 0.2
            "Cycling" -> 0.15
            "Walking" -> 0.08
            "Strength" -> 0.1
            "Yoga" -> 0.05
            "HIIT" -> 0.25
            else -> 0.1
        }
        return (caloriesPerSecond * 3).toInt() // 3-second intervals
    }

    private fun calculateStepIncrement(workoutType: String): Int {
        val stepsPerSecond = when (workoutType) {
            "Running" -> 2.5
            "Cycling" -> 0.0 // Cycling doesn't count steps
            "Walking" -> 1.8
            "Strength" -> 0.2
            "Yoga" -> 0.1
            "HIIT" -> 1.5
            else -> 1.0
        }
        return (stepsPerSecond * 3).toInt()
    }

    private fun calculateDistance(steps: Int): Double {
        // Average step length is approximately 0.7 meters
        return (steps * 0.7) / 1000.0 // Convert to kilometers
    }

    private suspend fun saveSessionToDatabase(state: SessionState) {
        try {
            // Get current user from Room database
            val currentUser = userRepository.getCurrentUserSuspend()
            if (currentUser == null) {
                // Handle case where user is not found in Room database
                return
            }
            
            val session = WorkoutSession(
                sessionId = state.sessionId!!,
                userId = currentUser.userId,
                workoutName = state.selectedWorkoutType!!.name,
                startTime = state.startTime!!,
                endTime = System.currentTimeMillis(),
                durationSeconds = state.elapsedTimeSeconds.toInt(),
                status = SessionStatus.COMPLETED,
                caloriesBurned = state.watchMetrics.calories,
                distanceKm = state.watchMetrics.distanceKm,
                avgHeartRate = state.watchMetrics.heartRate,
                maxHeartRate = state.watchMetrics.heartRate, // For now, same as avg
                avgPace = if (state.watchMetrics.distanceKm > 0) {
                    (state.elapsedTimeSeconds / 60.0) / state.watchMetrics.distanceKm
                } else 0.0,
                routeData = null, // Set to null for now as requested
                createdAt = System.currentTimeMillis(),
                isSynced = false // Leave as false for now as requested
            )

            workoutSessionDao.insertSession(session)
        } catch (e: Exception) {
            // Handle error - could emit error state
            e.printStackTrace()
        }
    }


    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        metricsJob?.cancel()
    }
}
