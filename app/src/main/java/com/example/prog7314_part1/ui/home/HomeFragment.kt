package com.example.prog7314_part1.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.prog7314_part1.R
import com.example.prog7314_part1.data.local.AppDatabase
import com.example.prog7314_part1.data.local.entity.SessionStatus
import com.example.prog7314_part1.data.repository.ApiUserRepository
import com.example.prog7314_part1.data.repository.NetworkRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var userRepository: ApiUserRepository
    private lateinit var networkRepository: NetworkRepository
    private lateinit var database: AppDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        userRepository = ApiUserRepository(requireContext())
        networkRepository = NetworkRepository(requireContext())
        database = AppDatabase.getDatabase(requireContext())

        setupProfileNavigation(view)
        loadUserData(view)
        loadTodayActivity(view)

        return view
    }

    private fun setupProfileNavigation(view: View) {
        view.findViewById<View>(R.id.profileIconContainer)?.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
        }
    }

    private fun loadUserData(view: View) {
        lifecycleScope.launch {
            userRepository.getCurrentUser().collect { user ->
                if (user != null) {
                    // Debug: Log user goals
                    android.util.Log.d("HomeFragment", "📊 User Goals - Steps: ${user.dailyStepGoal}, Calories: ${user.dailyCalorieGoal}, Workouts: ${user.weeklyWorkoutGoal}")
                    
                    // Update greeting with user's name
                    view.findViewById<TextView>(R.id.userGreeting)?.text = user.displayName

                    // Load profile image
                    loadProfileImage(view, user.profileImageUrl)

                    // Show badge if profile is incomplete
                    updateProfileBadge(view, user)
                }
            }
        }
    }
    
    private fun loadTodayActivity(view: View) {
        lifecycleScope.launch {
            userRepository.getCurrentUser().collect { user ->
                if (user != null) {
                    // Get today's workout sessions
                    val todaySessions = getTodaySessions()
                    
                    // Calculate totals from today's workouts
                    val totalSteps = todaySessions.sumOf { it.steps }
                    val totalCalories = todaySessions.sumOf { it.caloriesBurned }
                    val totalDistance = todaySessions.sumOf { it.distanceKm }
                    val totalActiveMinutes = todaySessions.sumOf { it.durationSeconds / 60 }
                    
                    // Update main steps display
                    view.findViewById<TextView>(R.id.mainStepsValue)?.text = 
                        String.format("%,d", totalSteps)
                    
                    // Update supporting stats
                    view.findViewById<TextView>(R.id.caloriesValue)?.text = 
                        String.format("%,d", totalCalories)
                    view.findViewById<TextView>(R.id.activeMinutesValue)?.text = 
                        "$totalActiveMinutes"
                    view.findViewById<TextView>(R.id.distanceValue)?.text = 
                        String.format("%.1f km", totalDistance)
                    
                    // Update progress circles based on goals
                    updateProgressCircles(view, user, totalSteps, totalCalories)
                    
                    // Update weekly target
                    updateWeeklyTarget(view, user)
                    
                    // Update quick stats grid
                    updateQuickStats(view, todaySessions, totalSteps, totalCalories)
                }
            }
        }
    }
    
    private suspend fun getTodaySessions(): List<com.example.prog7314_part1.data.local.entity.WorkoutSession> {
        val currentUser = userRepository.getCurrentUserSuspend() ?: return emptyList()
        
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis
        
        return database.workoutSessionDao()
            .getSessionsInTimeRange(currentUser.userId, todayStart, System.currentTimeMillis())
            .first()
            .filter { it.status == SessionStatus.COMPLETED }
    }
    
    private fun updateProgressCircles(
        view: View, 
        user: com.example.prog7314_part1.data.local.entity.User,
        totalSteps: Int,
        totalCalories: Int
    ) {
        // Steps progress
        user.dailyStepGoal?.let { goal ->
            view.findViewById<TextView>(R.id.stepsProgressText)?.text = 
                String.format("%,d", totalSteps)
            view.findViewById<TextView>(R.id.stepsGoalText)?.text = 
                "/${String.format("%,d", goal)}"
        }
        
        // Calories progress
        user.dailyCalorieGoal?.let { goal ->
            view.findViewById<TextView>(R.id.caloriesProgressText)?.text = 
                String.format("%,d", totalCalories)
            view.findViewById<TextView>(R.id.caloriesGoalText)?.text = 
                "/${String.format("%,d", goal)}"
        }
    }
    
    private fun updateWeeklyTarget(view: View, user: com.example.prog7314_part1.data.local.entity.User) {
        lifecycleScope.launch {
            // Get this week's sessions
            val currentUser = userRepository.getCurrentUserSuspend() ?: return@launch
            
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val weekStart = calendar.timeInMillis
            
            val weekSessions = database.workoutSessionDao()
                .getSessionsInTimeRange(currentUser.userId, weekStart, System.currentTimeMillis())
                .first()
                .filter { it.status == com.example.prog7314_part1.data.local.entity.SessionStatus.COMPLETED }
            
            val weeklySteps = weekSessions.sumOf { it.steps }
            val weeklyGoal = (user.dailyStepGoal ?: 10000) * 7
            
            val percentage = if (weeklyGoal > 0) {
                ((weeklySteps.toFloat() / weeklyGoal) * 100).toInt().coerceIn(0, 100)
            } else 0
            
            view.findViewById<TextView>(R.id.weeklyTargetText)?.text = 
                "${String.format("%,d", weeklySteps)} of ${String.format("%,d", weeklyGoal)} steps"
            view.findViewById<TextView>(R.id.weeklyTargetPercentage)?.text = "$percentage%"
            view.findViewById<android.widget.ProgressBar>(R.id.weeklyTargetProgress)?.progress = percentage
        }
    }
    
    private fun updateQuickStats(
        view: View,
        sessions: List<com.example.prog7314_part1.data.local.entity.WorkoutSession>,
        totalSteps: Int,
        totalCalories: Int
    ) {
        // Optional: Update additional stats if views exist
        // These views are optional and may not be in all layouts
    }

    private fun loadProfileImage(view: View, photoUrl: String?) {
        val profileIcon = view.findViewById<ImageView>(R.id.profileIcon) ?: return

        // Try to load from photoUrl first (Google Sign-In profile picture)
        if (!photoUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(photoUrl)
                .transform(CircleCrop())
                .placeholder(R.drawable.icon_default_profile)
                .error(R.drawable.icon_default_profile)
                .into(profileIcon)
        } else {
            // Try to load from Firebase Auth (Google profile picture)
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            val googlePhotoUrl = firebaseUser?.photoUrl

            if (googlePhotoUrl != null) {
                Glide.with(this)
                    .load(googlePhotoUrl)
                    .transform(CircleCrop())
                    .placeholder(R.drawable.icon_default_profile)
                    .error(R.drawable.icon_default_profile)
                    .into(profileIcon)
            } else {
                // Use default profile icon
                Glide.with(this)
                    .load(R.drawable.icon_default_profile)
                    .transform(CircleCrop())
                    .into(profileIcon)
            }
        }
    }

    private fun updateProfileBadge(view: View, user: com.example.prog7314_part1.data.local.entity.User) {
        val badge = view.findViewById<View>(R.id.profileBadge) ?: return

        // Show badge if profile is incomplete (age or weight not set)
        val isProfileIncomplete = user.age == 0 || user.weightKg == 0.0

        badge.visibility = if (isProfileIncomplete) View.VISIBLE else View.GONE
    }
}
