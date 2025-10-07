package com.example.prog7314_part1.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prog7314_part1.data.local.dao.WorkoutSessionDao
import com.example.prog7314_part1.data.local.entity.WorkoutSession
import com.example.prog7314_part1.data.local.entity.SessionStatus
import com.example.prog7314_part1.data.repository.ApiUserRepository
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

data class SessionState(
    val selectedWorkoutType: WorkoutType? = null,
    val isSessionActive: Boolean = false,
    val isSessionPaused: Boolean = false,
    val elapsedTimeSeconds: Long = 0,
    val timerText: String = "00:00:00",
    val sessionId: String? = null,
    val startTime: Long? = null
)

class SessionViewModel(
    private val workoutSessionDao: WorkoutSessionDao,
    private val userRepository: ApiUserRepository
) : ViewModel() {

    private val _sessionState = MutableStateFlow(SessionState())
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private var timerJob: Job? = null

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
                caloriesBurned = 0, // Set to 0 for now as requested
                distanceKm = 0.0, // Set to 0 for now as requested
                avgHeartRate = 0, // Set to 0 for now as requested
                maxHeartRate = 0, // Set to 0 for now as requested
                avgPace = 0.0, // Set to 0 for now as requested
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
    }
}
