package com.example.prog7314_part1.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prog7314_part1.data.local.dao.ExerciseDao
import com.example.prog7314_part1.data.local.dao.WorkoutDao
import com.example.prog7314_part1.data.local.dao.WorkoutSessionDao
import com.example.prog7314_part1.data.local.entity.Exercise
import com.example.prog7314_part1.data.local.entity.WorkoutSession
import com.example.prog7314_part1.data.local.entity.SessionStatus
import com.example.prog7314_part1.data.local.entity.Workout
import com.example.prog7314_part1.data.local.entity.WorkoutCategory
import com.example.prog7314_part1.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.*

data class WorkoutType(
    val name: String,
    val category: WorkoutCategory,
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

data class WorkoutWithExercises(
    val workoutId: String,
    val name: String,
    val description: String,
    val workoutType: WorkoutType? = null,
    val exercises: List<Exercise>
)

class SessionViewModel(
    private val workoutSessionDao: WorkoutSessionDao,
    private val userRepository: UserRepository,
    private val exerciseDao: ExerciseDao,
    private val workoutDao: WorkoutDao
) : ViewModel() {

    private val _sessionState = MutableStateFlow(SessionState())
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _selectedWorkout = MutableStateFlow<WorkoutWithExercises?>(null)
    val selectedWorkout: StateFlow<WorkoutWithExercises?> get() = _selectedWorkout

    private var timerJob: Job? = null
    private var metricsJob: Job? = null

    companion object {
        val availableWorkoutTypes = listOf(
            WorkoutType("Running", WorkoutCategory.CARDIO, "Cardio exercise"),
            WorkoutType("Cycling", WorkoutCategory.CARDIO, "Cardio exercise"),
            WorkoutType("Walking", WorkoutCategory.CARDIO, "Light cardio"),
            WorkoutType("Strength", WorkoutCategory.STRENGTH, "Weight training"),
            WorkoutType("Yoga", WorkoutCategory.FLEXIBILITY, "Mind-body practice"),
            WorkoutType("HIIT", WorkoutCategory.CARDIO, "High-intensity interval training")
        )

        fun getWorkoutTypeForWorkout(workout: WorkoutWithExercises): WorkoutType? {
            return availableWorkoutTypes.find { it.name == workout.name }
        }
    }

    fun selectWorkoutType(workoutType: WorkoutType) {
        _sessionState.value = _sessionState.value.copy(selectedWorkoutType = workoutType)
    }

    fun loadWorkoutWithExercises(workoutId: String) {
        viewModelScope.launch {
            val workout = workoutDao.getWorkoutById(workoutId)
            if (workout != null) {
                val exercisesList = exerciseDao.getExercisesForWorkoutOnce(workoutId)
                _selectedWorkout.value = WorkoutWithExercises(
                    workoutId = workout.workoutId,
                    name = workout.name,
                    description = workout.description,
                    workoutType = getWorkoutTypeForWorkout(workout), // returns correct WorkoutType
                    exercises = exercisesList
                )
            }
        }
    }

    private suspend fun ExerciseDao.getExercisesForWorkoutOnce(workoutId: String): List<Exercise> {
        return this.getExercisesForWorkout(workoutId).firstOrNull() ?: emptyList()
    }

    fun getWorkoutTypeForWorkout(workout: Workout): WorkoutType? {
        // Workout.category is an enum; availableWorkoutTypes[].category must also be WorkoutCategory
        return availableWorkoutTypes.find { it.category == workout.category }
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

    private fun startMetricsSimulation() {
        metricsJob = viewModelScope.launch {
            while (_sessionState.value.watchMetrics.isConnected) {
                delay(3000)
                if (_sessionState.value.isSessionActive && !_sessionState.value.isSessionPaused) {
                    updateMetrics()
                }
            }
        }
    }

    private fun updateMetrics() {
        val currentMetrics = _sessionState.value.watchMetrics
        val workoutType = _sessionState.value.selectedWorkoutType?.name ?: "General"

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
        return maxOf(60, minOf(200, baseHeartRate + (-10..10).random()))
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
        return (caloriesPerSecond * 3).toInt()
    }

    private fun calculateStepIncrement(workoutType: String): Int {
        val stepsPerSecond = when (workoutType) {
            "Running" -> 2.5
            "Cycling" -> 0.0
            "Walking" -> 1.8
            "Strength" -> 0.2
            "Yoga" -> 0.1
            "HIIT" -> 1.5
            else -> 1.0
        }
        return (stepsPerSecond * 3).toInt()
    }

    private fun calculateDistance(steps: Int): Double {
        return (steps * 0.7) / 1000.0
    }

    private suspend fun saveSessionToDatabase(state: SessionState) {
        try {
            val currentUser = userRepository.getCurrentUserSuspend() ?: return
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
                maxHeartRate = state.watchMetrics.heartRate,
                avgPace = if (state.watchMetrics.distanceKm > 0)
                    (state.elapsedTimeSeconds / 60.0) / state.watchMetrics.distanceKm
                else 0.0,
                routeData = null,
                createdAt = System.currentTimeMillis(),
                isSynced = false
            )
            workoutSessionDao.insertSession(session)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        metricsJob?.cancel()
    }
}
