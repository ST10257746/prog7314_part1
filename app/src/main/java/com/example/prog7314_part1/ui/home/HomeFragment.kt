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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var userRepository: ApiUserRepository
    private lateinit var networkRepository: NetworkRepository
    private lateinit var database: AppDatabase
    private var rootView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        rootView = view

        userRepository = ApiUserRepository(requireContext())
        networkRepository = NetworkRepository(requireContext())
        database = AppDatabase.getDatabase(requireContext())

        setupProfileNavigation(view)
        loadUserData(view)
        loadTodayActivity(view)
        loadWeeklyGoals(view)
        loadGoalsList(view)
        loadWeeklyChart(view)

        return view
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        rootView = null
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
            userRepository.getCurrentUser().collectLatest { user ->
                if (user != null) {
                    // Get today's workout sessions
                    val todaySessions = getTodaySessions(user.userId)
                    
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
    
    private suspend fun getTodaySessions(userId: String): List<com.example.prog7314_part1.data.local.entity.WorkoutSession> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis
        
        return database.workoutSessionDao()
            .getSessionsInTimeRange(userId, todayStart, System.currentTimeMillis())
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
            try {
                // Calculate week start (Monday of current week)
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                
                // Go back to Monday
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
                calendar.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
                val weekStart = calendar.timeInMillis
                
                val weekSessions = withContext(Dispatchers.IO) {
                    database.workoutSessionDao()
                        .getSessionsInTimeRange(user.userId, weekStart, System.currentTimeMillis())
                        .first()
                        .filter { it.status == com.example.prog7314_part1.data.local.entity.SessionStatus.COMPLETED }
                }
                
                val weeklySteps = weekSessions.sumOf { it.steps }
                val weeklyGoal = (user.dailyStepGoal ?: 10000) * 7
                
                val percentage = if (weeklyGoal > 0) {
                    ((weeklySteps.toFloat() / weeklyGoal) * 100).toInt().coerceIn(0, 100)
                } else 0
                
                withContext(Dispatchers.Main) {
                    view.findViewById<TextView>(R.id.weeklyTargetText)?.text = 
                        "${String.format("%,d", weeklySteps)} of ${String.format("%,d", weeklyGoal)} steps"
                    view.findViewById<TextView>(R.id.weeklyTargetPercentage)?.text = "$percentage%"
                    view.findViewById<android.widget.ProgressBar>(R.id.weeklyTargetProgress)?.progress = percentage
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "❌ Error updating weekly target: ${e.message}", e)
            }
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

    /**
     * Load weekly goals checkmarks (which days had workouts)
     */
    private fun loadWeeklyGoals(view: View) {
        lifecycleScope.launch {
            try {
                userRepository.getCurrentUser().collectLatest { currentUser ->
                    if (currentUser == null) {
                        android.util.Log.e("HomeFragment", "❌ No current user found")
                        return@collectLatest
                    }
                    
                    // Calculate week start (Monday of current week)
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    
                    // Go back to Monday (or first day of week)
                    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                    val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
                    calendar.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
                    val weekStart = calendar.timeInMillis
                    
                    android.util.Log.d("HomeFragment", "📅 Week start: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(weekStart))}")
                    
                    val weekSessions = withContext(Dispatchers.IO) {
                        database.workoutSessionDao()
                            .getSessionsInTimeRange(currentUser.userId, weekStart, System.currentTimeMillis())
                            .first()
                            .filter { it.status == SessionStatus.COMPLETED }
                    }
                    
                    android.util.Log.d("HomeFragment", "📊 Found ${weekSessions.size} completed sessions this week")
                    
                    // Group sessions by day of week
                    val sessionsByDay = mutableMapOf<Int, Boolean>() // Day of week -> has workout
                    for (i in Calendar.SUNDAY..Calendar.SATURDAY) {
                        sessionsByDay[i] = false
                    }
                    
                    weekSessions.forEach { session ->
                        calendar.timeInMillis = session.startTime
                        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                        sessionsByDay[dayOfWeek] = true
                        android.util.Log.d("HomeFragment", "✅ Session on ${calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())}")
                    }
                    
                    // Update checkmarks on main thread
                    withContext(Dispatchers.Main) {
                        updateDayCheckmark(view, R.id.mondayCheckmark, sessionsByDay[Calendar.MONDAY] == true)
                        updateDayCheckmark(view, R.id.tuesdayCheckmark, sessionsByDay[Calendar.TUESDAY] == true)
                        updateDayCheckmark(view, R.id.wednesdayCheckmark, sessionsByDay[Calendar.WEDNESDAY] == true)
                        updateDayCheckmark(view, R.id.thursdayCheckmark, sessionsByDay[Calendar.THURSDAY] == true)
                        updateDayCheckmark(view, R.id.fridayCheckmark, sessionsByDay[Calendar.FRIDAY] == true)
                        updateDayCheckmark(view, R.id.saturdayCheckmark, sessionsByDay[Calendar.SATURDAY] == true)
                        updateDayCheckmark(view, R.id.sundayCheckmark, sessionsByDay[Calendar.SUNDAY] == true)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "❌ Error loading weekly goals: ${e.message}", e)
            }
        }
    }
    
    private fun updateDayCheckmark(view: View, checkmarkId: Int, hasWorkout: Boolean) {
        val checkmark = view.findViewById<TextView>(checkmarkId) ?: return
        if (hasWorkout) {
            checkmark.text = "✓"
            checkmark.setTextColor(requireContext().getColor(R.color.Secondary))
            checkmark.setTypeface(null, android.graphics.Typeface.BOLD)
            checkmark.alpha = 1.0f
        } else {
            checkmark.text = "○"
            checkmark.setTextColor(requireContext().getColor(R.color.Text))
            checkmark.setTypeface(null, android.graphics.Typeface.NORMAL)
            checkmark.alpha = 0.5f
        }
    }

    /**
     * Load goals list for today - includes both daily Goal entities and fitness goals from User
     */
    private fun loadGoalsList(view: View) {
        lifecycleScope.launch {
            try {
                userRepository.getCurrentUser().collectLatest { currentUser ->
                    if (currentUser == null) {
                        android.util.Log.e("HomeFragment", "❌ No current user found for goals")
                        return@collectLatest
                    }
                    
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val today = dateFormat.format(Date())
                    
                    android.util.Log.d("HomeFragment", "📋 Loading goals for date: $today, userId: ${currentUser.userId}")
                    
                    // Get daily Goal entities
                    val dailyGoals = withContext(Dispatchers.IO) {
                        database.goalDao().getGoalsForDateSuspend(currentUser.userId, today)
                    }
                    
                    android.util.Log.d("HomeFragment", "📋 Found ${dailyGoals.size} daily goals for today")
                    
                    // Get today's activity to check progress
                    val todaySessions = getTodaySessions(currentUser.userId)
                    val todaySteps = todaySessions.sumOf { it.steps }
                    val todayCalories = todaySessions.sumOf { it.caloriesBurned }
                    
                    val goalsContainer = view.findViewById<android.widget.LinearLayout>(R.id.goalsListContainer)
                    val emptyState = view.findViewById<TextView>(R.id.goalsEmptyState)
                    
                    withContext(Dispatchers.Main) {
                        goalsContainer?.removeAllViews()
                        
                        // Create fitness goals from User's goals
                        val fitnessGoals = mutableListOf<com.example.prog7314_part1.data.local.entity.Goal>()
                        
                        // Step goal
                        currentUser.dailyStepGoal?.let { stepGoal ->
                            val isCompleted = todaySteps >= stepGoal
                            fitnessGoals.add(
                                com.example.prog7314_part1.data.local.entity.Goal(
                                    goalId = -1, // Temporary ID for fitness goals
                                    userId = currentUser.userId,
                                    date = today,
                                    title = getString(R.string.reach_steps_goal, String.format("%,d", stepGoal)),
                                    description = "${String.format("%,d", todaySteps)} / ${String.format("%,d", stepGoal)}",
                                    isCompleted = isCompleted,
                                    completedAt = if (isCompleted) System.currentTimeMillis() else null
                                )
                            )
                        }
                        
                        // Calorie goal
                        currentUser.dailyCalorieGoal?.let { calorieGoal ->
                            val isCompleted = todayCalories >= calorieGoal
                            fitnessGoals.add(
                                com.example.prog7314_part1.data.local.entity.Goal(
                                    goalId = -2,
                                    userId = currentUser.userId,
                                    date = today,
                                    title = getString(R.string.reach_calories_goal, String.format("%,d", calorieGoal)),
                                    description = "${String.format("%,d", todayCalories)} / ${String.format("%,d", calorieGoal)}",
                                    isCompleted = isCompleted,
                                    completedAt = if (isCompleted) System.currentTimeMillis() else null
                                )
                            )
                        }
                        
                        // Combine fitness goals with daily goals
                        val allGoals = fitnessGoals + dailyGoals
                        
                        if (allGoals.isEmpty()) {
                            emptyState?.visibility = View.VISIBLE
                            goalsContainer?.visibility = View.GONE
                        } else {
                            emptyState?.visibility = View.GONE
                            goalsContainer?.visibility = View.VISIBLE
                            
                            // Add each goal
                            allGoals.forEach { goal ->
                                val goalView = createGoalView(goal)
                                goalsContainer?.addView(goalView)
                            }
                        }
                        
                        // Setup add goal button (only once)
                        view.findViewById<android.widget.Button>(R.id.addGoalButton)?.setOnClickListener {
                            // TODO: Navigate to add goal screen or show dialog
                            android.util.Log.d("HomeFragment", "Add goal clicked")
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "❌ Error loading goals list: ${e.message}", e)
            }
        }
    }
    
    private fun createGoalView(goal: com.example.prog7314_part1.data.local.entity.Goal): View {
        val inflater = LayoutInflater.from(requireContext())
        val goalView = inflater.inflate(R.layout.item_home_goal, null)
        
        val checkbox = goalView.findViewById<android.widget.CheckBox>(R.id.goalCheckbox)
        val titleText = goalView.findViewById<TextView>(R.id.goalTitle)
        val statusText = goalView.findViewById<TextView>(R.id.goalStatus)
        
        titleText?.text = goal.title
        checkbox?.isChecked = goal.isCompleted
        
        if (goal.isCompleted) {
            statusText?.text = "✓"
            statusText?.setTextColor(requireContext().getColor(R.color.Secondary))
            statusText?.setTypeface(null, android.graphics.Typeface.BOLD)
            statusText?.alpha = 1.0f
        } else {
            statusText?.text = "○"
            statusText?.setTextColor(requireContext().getColor(R.color.Text))
            statusText?.setTypeface(null, android.graphics.Typeface.NORMAL)
            statusText?.alpha = 0.5f
        }
        
        checkbox?.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                val updatedGoal = goal.copy(isCompleted = isChecked, completedAt = if (isChecked) System.currentTimeMillis() else null)
                database.goalDao().updateGoal(updatedGoal)
                // Refresh the view if still attached
                rootView?.let { loadGoalsList(it) }
            }
        }
        
        return goalView
    }

    /**
     * Load weekly progress chart with actual step data
     */
    private fun loadWeeklyChart(view: View) {
        lifecycleScope.launch {
            try {
                userRepository.getCurrentUser().collectLatest { currentUser ->
                    if (currentUser == null) {
                        android.util.Log.e("HomeFragment", "❌ No current user found for chart")
                        return@collectLatest
                    }
                    
                    // Calculate week start (Monday of current week)
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    
                    // Go back to Monday
                    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                    val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
                    calendar.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
                    val weekStart = calendar.timeInMillis
                    
                    android.util.Log.d("HomeFragment", "📊 Loading chart data from week start: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(weekStart))}")
                    
                    val weekSessions = withContext(Dispatchers.IO) {
                        database.workoutSessionDao()
                            .getSessionsInTimeRange(currentUser.userId, weekStart, System.currentTimeMillis())
                            .first()
                            .filter { it.status == SessionStatus.COMPLETED }
                    }
                    
                    android.util.Log.d("HomeFragment", "📊 Found ${weekSessions.size} sessions for chart")
                    
                    // Calculate steps per day
                    val stepsByDay = mutableMapOf<Int, Int>() // Day of week -> total steps
                    for (i in Calendar.SUNDAY..Calendar.SATURDAY) {
                        stepsByDay[i] = 0
                    }
                    
                    weekSessions.forEach { session ->
                        calendar.timeInMillis = session.startTime
                        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                        stepsByDay[dayOfWeek] = (stepsByDay[dayOfWeek] ?: 0) + session.steps
                        android.util.Log.d("HomeFragment", "📈 Session: ${session.workoutName}, Steps: ${session.steps}, Day: ${calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())}")
                    }
                    
                    // Find max steps for scaling
                    val maxSteps = stepsByDay.values.maxOrNull() ?: 1
                    val chartHeight = 160 // dp equivalent
                    
                    android.util.Log.d("HomeFragment", "📊 Max steps: $maxSteps, Steps by day: $stepsByDay")
                    
                    val chartBarsContainer = view.findViewById<android.widget.LinearLayout>(R.id.weeklyChartBars)
                    
                    withContext(Dispatchers.Main) {
                        chartBarsContainer?.removeAllViews()
                        
                        // Create bars for each day (Sunday to Saturday)
                        val dayOrder = listOf(
                            Calendar.SUNDAY,
                            Calendar.MONDAY,
                            Calendar.TUESDAY,
                            Calendar.WEDNESDAY,
                            Calendar.THURSDAY,
                            Calendar.FRIDAY,
                            Calendar.SATURDAY
                        )
                        
                        dayOrder.forEach { dayOfWeek ->
                            val steps = stepsByDay[dayOfWeek] ?: 0
                            val barHeight = if (maxSteps > 0) {
                                ((steps.toFloat() / maxSteps) * chartHeight).toInt().coerceAtLeast(4)
                            } else {
                                4
                            }
                            
                            val barView = createChartBar(steps, barHeight)
                            chartBarsContainer?.addView(barView)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "❌ Error loading weekly chart: ${e.message}", e)
            }
        }
    }
    
    private fun createChartBar(steps: Int, heightDp: Int): View {
        val inflater = LayoutInflater.from(requireContext())
        val barView = inflater.inflate(R.layout.item_weekly_chart_bar, null)
        
        val stepsText = barView.findViewById<TextView>(R.id.barStepsText)
        val barViewElement = barView.findViewById<View>(R.id.barView)
        
        val stepsFormatted = if (steps >= 1000) {
            String.format("%.1fK", steps / 1000.0)
        } else {
            steps.toString()
        }
        
        stepsText?.text = stepsFormatted
        
        val heightPx = (heightDp * resources.displayMetrics.density).toInt()
        val layoutParams = barViewElement?.layoutParams
        layoutParams?.height = heightPx
        barViewElement?.layoutParams = layoutParams
        
        return barView
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
