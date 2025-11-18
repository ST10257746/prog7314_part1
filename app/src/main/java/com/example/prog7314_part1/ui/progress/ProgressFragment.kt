package com.example.prog7314_part1.ui.progress

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prog7314_part1.R
import com.example.prog7314_part1.data.local.AppDatabase
import com.example.prog7314_part1.data.local.entity.MealType
import com.example.prog7314_part1.data.local.entity.NutritionEntry
import com.example.prog7314_part1.data.local.entity.SessionStatus
import com.example.prog7314_part1.data.local.entity.WorkoutSession
import com.example.prog7314_part1.data.repository.ApiUserRepository
import com.example.prog7314_part1.data.repository.NetworkRepository
import com.example.prog7314_part1.data.model.Result
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ProgressFragment : Fragment() {
    
    private lateinit var userRepository: ApiUserRepository
    private lateinit var networkRepository: NetworkRepository
    private lateinit var database: AppDatabase
    private lateinit var sharedPreferences: SharedPreferences
    private var rootView: View? = null
    
    companion object {
        private const val PREFS_NAME = "test_data_prefs"
        private const val KEY_TEST_DATA_ACTIVE = "test_data_active"
        private const val KEY_TEST_SESSION_IDS = "test_session_ids"
        private const val KEY_TEST_ENTRY_IDS = "test_entry_ids"
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_progress, container, false)
        rootView = view
        
        userRepository = ApiUserRepository(requireContext())
        networkRepository = NetworkRepository(requireContext())
        database = AppDatabase.getDatabase(requireContext())
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        
        setupTestModeButton(view)
        loadProgressData(view)
        
        return view
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        rootView = null
    }
    
    private fun setupTestModeButton(view: View) {
        val testButton = view.findViewById<Button>(R.id.testModeButton) ?: return
        
        // Update button text based on current state
        updateTestButtonText(testButton)
        
        testButton.setOnClickListener {
            lifecycleScope.launch {
                val isTestDataActive = sharedPreferences.getBoolean(KEY_TEST_DATA_ACTIVE, false)
                
                if (isTestDataActive) {
                    removeTestData(testButton)
                } else {
                    addTestData(testButton)
                }
            }
        }
    }
    
    private fun updateTestButtonText(button: Button) {
        val isTestDataActive = sharedPreferences.getBoolean(KEY_TEST_DATA_ACTIVE, false)
        button.text = if (isTestDataActive) {
            getString(R.string.remove_test_data)
        } else {
            getString(R.string.add_test_data)
        }
        button.backgroundTintList = ContextCompat.getColorStateList(
            requireContext(),
            if (isTestDataActive) R.color.Secondary else R.color.Accent
        )
    }
    
    private suspend fun addTestData(button: Button) = withContext(Dispatchers.IO) {
        try {
            val currentUser = userRepository.getCurrentUserSuspend() ?: run {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
                }
                return@withContext
            }
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            
            // Generate test data for the past 7 days
            val testSessions = mutableListOf<WorkoutSession>()
            val testEntries = mutableListOf<NutritionEntry>()
            val sessionIds = mutableListOf<String>()
            val entryIds = mutableListOf<Long>()
            
            val workoutTypes = listOf("Running", "Cycling", "Walking", "Strength", "Yoga", "HIIT")
            val mealNames = listOf(
                "Oatmeal with Berries" to MealType.BREAKFAST,
                "Grilled Chicken Salad" to MealType.LUNCH,
                "Salmon with Vegetables" to MealType.DINNER,
                "Protein Shake" to MealType.SNACK,
                "Scrambled Eggs" to MealType.BREAKFAST,
                "Turkey Sandwich" to MealType.LUNCH,
                "Pasta Primavera" to MealType.DINNER,
                "Greek Yogurt" to MealType.SNACK
            )
            
            // Calculate week start (Monday of current week) - matching HomeFragment logic
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
            
            val today = Calendar.getInstance()
            today.set(Calendar.HOUR_OF_DAY, 23)
            today.set(Calendar.MINUTE, 59)
            today.set(Calendar.SECOND, 59)
            today.set(Calendar.MILLISECOND, 999)
            val todayEnd = today.timeInMillis
            
            android.util.Log.d("ProgressFragment", "📅 Week start: ${dateFormat.format(Date(weekStart))}, Today: ${dateFormat.format(Date())}")
            
            // Generate data for all 7 days of the week (Sunday through Saturday)
            // Start from Sunday (1 day before Monday)
            val sundayCalendar = Calendar.getInstance()
            sundayCalendar.timeInMillis = weekStart
            sundayCalendar.add(Calendar.DAY_OF_MONTH, -1) // Go back 1 day from Monday to get Sunday
            val sundayStart = sundayCalendar.timeInMillis
            
            for (dayOffset in 0..6) {
                // Create a fresh calendar instance for each day
                val dayCalendar = Calendar.getInstance()
                dayCalendar.timeInMillis = sundayStart
                dayCalendar.add(Calendar.DAY_OF_MONTH, dayOffset)
                dayCalendar.set(Calendar.HOUR_OF_DAY, 0)
                dayCalendar.set(Calendar.MINUTE, 0)
                dayCalendar.set(Calendar.SECOND, 0)
                dayCalendar.set(Calendar.MILLISECOND, 0)
                
                val dateString = dateFormat.format(Date(dayCalendar.timeInMillis))
                val dayName = dayCalendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
                android.util.Log.d("ProgressFragment", "📅 Generating test data for day offset $dayOffset ($dayName): $dateString")
                
                // Generate 1-2 workout sessions per day
                val numSessions = if (dayOffset == 0) 1 else (1..2).random()
                for (i in 0 until numSessions) {
                    val workoutType = workoutTypes.random()
                    val startHour = if (i == 0) (6..9).random() else (17..20).random()
                    
                    // Create a fresh calendar copy for this session
                    val sessionCalendar = dayCalendar.clone() as Calendar
                    sessionCalendar.set(Calendar.HOUR_OF_DAY, startHour)
                    sessionCalendar.set(Calendar.MINUTE, (0..59).random())
                    sessionCalendar.set(Calendar.SECOND, 0)
                    sessionCalendar.set(Calendar.MILLISECOND, 0)
                    
                    val startTime = sessionCalendar.timeInMillis
                    val durationMinutes = when (workoutType) {
                        "Running", "Cycling" -> (30..60).random()
                        "Walking" -> (20..45).random()
                        "Strength" -> (45..75).random()
                        "Yoga" -> (30..60).random()
                        "HIIT" -> (20..30).random()
                        else -> 30
                    }
                    val durationSeconds = durationMinutes * 60
                    val endTime = startTime + (durationSeconds * 1000L)
                    
                    // Calculate realistic metrics based on workout type
                    val steps = when (workoutType) {
                        "Running" -> durationMinutes * 150
                        "Walking" -> durationMinutes * 100
                        "Cycling" -> durationMinutes * 0
                        else -> durationMinutes * 20
                    }
                    val calories = when (workoutType) {
                        "Running" -> durationMinutes * 12
                        "Cycling" -> durationMinutes * 10
                        "Walking" -> durationMinutes * 5
                        "Strength" -> durationMinutes * 8
                        "Yoga" -> durationMinutes * 3
                        "HIIT" -> durationMinutes * 15
                        else -> durationMinutes * 6
                    }
                    val distance = when (workoutType) {
                        "Running" -> (steps * 0.7) / 1000.0
                        "Cycling" -> (durationMinutes * 0.5)
                        "Walking" -> (steps * 0.7) / 1000.0
                        else -> 0.0
                    }
                    
                    val session = WorkoutSession(
                        sessionId = UUID.randomUUID().toString(),
                        userId = currentUser.userId,
                        workoutName = workoutType,
                        startTime = startTime,
                        endTime = endTime,
                        durationSeconds = durationSeconds,
                        caloriesBurned = calories,
                        distanceKm = distance,
                        steps = steps,
                        status = SessionStatus.COMPLETED,
                        isSynced = false
                    )
                    
                    testSessions.add(session)
                    sessionIds.add(session.sessionId)
                    android.util.Log.d("ProgressFragment", "✅ Generated session: $workoutType on $dateString at ${timeFormat.format(Date(startTime))}, Steps: $steps")
                }
                
                // Generate 2-3 nutrition entries per day
                val numMeals = (2..3).random()
                val selectedMeals = mealNames.shuffled().take(numMeals)
                
                selectedMeals.forEachIndexed { index, (mealName, mealType) ->
                    val mealHour = when (mealType) {
                        MealType.BREAKFAST -> (7..9).random()
                        MealType.LUNCH -> (12..14).random()
                        MealType.DINNER -> (18..20).random()
                        MealType.SNACK -> (10..11).random() + (index * 3)
                    }
                    
                    // Create a fresh calendar copy for this meal
                    val mealCalendar = dayCalendar.clone() as Calendar
                    mealCalendar.set(Calendar.HOUR_OF_DAY, mealHour)
                    mealCalendar.set(Calendar.MINUTE, (0..59).random())
                    mealCalendar.set(Calendar.SECOND, 0)
                    mealCalendar.set(Calendar.MILLISECOND, 0)
                    
                    val mealCalories = when (mealType) {
                        MealType.BREAKFAST -> (300..500).random()
                        MealType.LUNCH -> (400..700).random()
                        MealType.DINNER -> (500..800).random()
                        MealType.SNACK -> (100..300).random()
                    }
                    
                    val entry = NutritionEntry(
                        userId = currentUser.userId,
                        date = dateString,
                        mealType = mealType,
                        time = timeFormat.format(Date(mealCalendar.timeInMillis)),
                        foodName = mealName,
                        servingSize = "1 serving",
                        calories = mealCalories,
                        proteinG = (mealCalories * 0.2 / 4).coerceIn(10.0, 50.0),
                        carbsG = (mealCalories * 0.5 / 4).coerceIn(20.0, 100.0),
                        fatsG = (mealCalories * 0.3 / 9).coerceIn(5.0, 40.0),
                        isSynced = false
                    )
                    
                    val entryId = database.nutritionEntryDao().insertEntry(entry)
                    testEntries.add(entry.copy(entryId = entryId))
                    entryIds.add(entryId)
                    android.util.Log.d("ProgressFragment", "✅ Generated meal: $mealName on $dateString at ${entry.time}")
                }
            }
            
            android.util.Log.d("ProgressFragment", "📊 Generated ${testSessions.size} sessions and ${testEntries.size} nutrition entries")
            
            // Insert all sessions
            database.workoutSessionDao().insertSessions(testSessions)
            
            // Save IDs to SharedPreferences
            sharedPreferences.edit().apply {
                putBoolean(KEY_TEST_DATA_ACTIVE, true)
                putStringSet(KEY_TEST_SESSION_IDS, sessionIds.toSet())
                putString(KEY_TEST_ENTRY_IDS, entryIds.joinToString(","))
                apply()
            }
            
            withContext(Dispatchers.Main) {
                updateTestButtonText(button)
                Toast.makeText(requireContext(), "Test data added successfully!", Toast.LENGTH_SHORT).show()
                // Reload progress data
                rootView?.let { loadProgressData(it) }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("ProgressFragment", "Error adding test data", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Error adding test data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private suspend fun removeTestData(button: Button) = withContext(Dispatchers.IO) {
        try {
            val sessionIds = sharedPreferences.getStringSet(KEY_TEST_SESSION_IDS, emptySet()) ?: emptySet()
            val entryIdsString = sharedPreferences.getString(KEY_TEST_ENTRY_IDS, "") ?: ""
            val entryIds = if (entryIdsString.isNotEmpty()) {
                entryIdsString.split(",").mapNotNull { it.toLongOrNull() }
            } else {
                emptyList()
            }
            
            // Delete sessions
            sessionIds.forEach { sessionId ->
                val session = database.workoutSessionDao().getSessionById(sessionId)
                session?.let {
                    database.workoutSessionDao().deleteSession(it)
                }
            }
            
            // Delete nutrition entries
            entryIds.forEach { entryId ->
                val entry = database.nutritionEntryDao().getEntryById(entryId)
                entry?.let {
                    database.nutritionEntryDao().deleteEntry(it)
                }
            }
            
            // Clear SharedPreferences
            sharedPreferences.edit().apply {
                putBoolean(KEY_TEST_DATA_ACTIVE, false)
                remove(KEY_TEST_SESSION_IDS)
                remove(KEY_TEST_ENTRY_IDS)
                apply()
            }
            
            withContext(Dispatchers.Main) {
                updateTestButtonText(button)
                Toast.makeText(requireContext(), "Test data removed successfully!", Toast.LENGTH_SHORT).show()
                // Reload progress data
                rootView?.let { loadProgressData(it) }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("ProgressFragment", "Error removing test data", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Error removing test data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun loadProgressData(view: View) {
        lifecycleScope.launch {
            // Get current user for goals
            userRepository.getCurrentUser().collect { user ->
                if (user != null) {
                    // Fetch workout sessions from this week
                    val weekSessions = getThisWeekSessions()
                    
                    // Calculate totals from sessions
                    val totalSteps = weekSessions.sumOf { it.steps }
                    val totalWorkouts = weekSessions.size
                    val totalCalories = weekSessions.sumOf { it.caloriesBurned }
                    val totalDistance = weekSessions.sumOf { it.distanceKm }
                    
                    // Update Steps Progress
                    user.dailyStepGoal?.let { dailyGoal ->
                        val weeklyGoal = dailyGoal * 7
                        val stepsProgress = if (weeklyGoal > 0) {
                            ((totalSteps.toFloat() / weeklyGoal) * 100).toInt().coerceIn(0, 100)
                        } else 0
                        
                        view.findViewById<TextView>(R.id.stepsCurrentText)?.text = 
                            getString(R.string.steps_this_week_format, String.format("%,d", totalSteps))
                        view.findViewById<TextView>(R.id.stepsTargetText)?.text = 
                            getString(R.string.target_steps_format, String.format("%,d", weeklyGoal))
                        view.findViewById<TextView>(R.id.stepsPercentageText)?.text = getString(R.string.percentage_format, stepsProgress)
                        view.findViewById<android.widget.ProgressBar>(R.id.stepsProgress)?.progress = stepsProgress
                    }
                    
                    // Update Workouts Progress
                    user.weeklyWorkoutGoal?.let { workoutGoal ->
                        val workoutsProgress = if (workoutGoal > 0) {
                            ((totalWorkouts.toFloat() / workoutGoal) * 100).toInt().coerceIn(0, 100)
                        } else 0
                        
                        view.findViewById<TextView>(R.id.workoutsCurrentText)?.text = 
                            getString(R.string.completed_format, totalWorkouts)
                        view.findViewById<TextView>(R.id.workoutsTargetText)?.text = 
                            getString(R.string.target_workouts_format, workoutGoal)
                        view.findViewById<TextView>(R.id.workoutsPercentageText)?.text = getString(R.string.percentage_format, workoutsProgress)
                        view.findViewById<android.widget.ProgressBar>(R.id.workoutsProgress)?.progress = workoutsProgress
                    }
                    
                    // Update Calories Progress
                    user.dailyCalorieGoal?.let { dailyGoal ->
                        val weeklyGoal = dailyGoal * 7
                        val caloriesProgress = if (weeklyGoal > 0) {
                            ((totalCalories.toFloat() / weeklyGoal) * 100).toInt().coerceIn(0, 100)
                        } else 0
                        
                        view.findViewById<TextView>(R.id.caloriesCurrentText)?.text = 
                            getString(R.string.kcal_this_week_format, String.format("%,d", totalCalories))
                        view.findViewById<TextView>(R.id.caloriesTargetText)?.text = 
                            getString(R.string.target_kcal_format, String.format("%,d", weeklyGoal))
                        view.findViewById<TextView>(R.id.caloriesPercentageText)?.text = getString(R.string.percentage_format, caloriesProgress)
                        view.findViewById<android.widget.ProgressBar>(R.id.caloriesProgress)?.progress = caloriesProgress
                    }
                    
                    // Update Trends Section
                    updateTrends(view, totalDistance, totalWorkouts, weekSessions)
                    
                    // Update Charts (User Defined Feature 4)
                    updateCharts(view, weekSessions)
                    
                    // Update Recent Activity
                    updateRecentActivity(view, weekSessions)
                }
            }
        }
    }
    
    private suspend fun getThisWeekSessions(): List<WorkoutSession> {
        val currentUser = userRepository.getCurrentUserSuspend() ?: return emptyList()
        
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val weekStart = calendar.timeInMillis
        
        return database.workoutSessionDao()
            .getSessionsInTimeRange(currentUser.userId, weekStart, System.currentTimeMillis())
            .first()
            .filter { it.status == SessionStatus.COMPLETED }
    }
    
    private fun updateTrends(view: View, totalDistance: Double, totalWorkouts: Int, sessions: List<WorkoutSession>) {
        // Average workout duration
        val avgDuration = if (sessions.isNotEmpty()) {
            sessions.map { it.durationSeconds / 60 }.average().toInt()
        } else 0
        
        view.findViewById<TextView>(R.id.avgWorkoutText)?.text = getString(R.string.min_format_simple, avgDuration)
        
        // Total distance
        view.findViewById<TextView>(R.id.totalDistanceText)?.text = 
            String.format("%.1f km", totalDistance)
        
        // Workout frequency (this week vs last week)
        view.findViewById<TextView>(R.id.workoutFrequencyText)?.text = 
            getString(R.string.workouts_format, totalWorkouts)
    }
    
    /**
     * Update charts with weekly data (User Defined Feature 4)
     * Adds visual charts alongside progress bars for comprehensive data visualization
     * 
     * References:
     * - MPAndroidChart: https://github.com/PhilJay/MPAndroidChart
     */
    private fun updateCharts(view: View, sessions: List<WorkoutSession>) {
        // Group sessions by day of week
        val calendar = Calendar.getInstance()
        val dayData = mutableMapOf<Int, Pair<Int, Int>>() // Day of week -> (steps, calories)
        
        // Initialize all days with 0
        for (i in Calendar.SUNDAY..Calendar.SATURDAY) {
            dayData[i] = Pair(0, 0)
        }
        
        // Aggregate data by day
        sessions.forEach { session ->
            calendar.timeInMillis = session.startTime
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val current = dayData[dayOfWeek] ?: Pair(0, 0)
            dayData[dayOfWeek] = Pair(current.first + session.steps, current.second + session.caloriesBurned)
        }
        
        // Update Steps Bar Chart
        updateStepsChart(view, dayData)
        
        // Update Calories Line Chart
        updateCaloriesChart(view, dayData)
    }
    
    /**
     * Update weekly steps bar chart
     */
    private fun updateStepsChart(view: View, dayData: Map<Int, Pair<Int, Int>>) {
        val chart = view.findViewById<BarChart>(R.id.weeklyStepsChart) ?: return
        
        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        
        // Order: Sunday, Monday, Tuesday, Wednesday, Thursday, Friday, Saturday
        val dayOrder = listOf(
            Calendar.SUNDAY to getString(R.string.sunday),
            Calendar.MONDAY to getString(R.string.monday),
            Calendar.TUESDAY to getString(R.string.tuesday),
            Calendar.WEDNESDAY to getString(R.string.wednesday),
            Calendar.THURSDAY to getString(R.string.thursday),
            Calendar.FRIDAY to getString(R.string.friday),
            Calendar.SATURDAY to getString(R.string.saturday)
        )
        
        dayOrder.forEachIndexed { index, (dayOfWeek, label) ->
            val (steps, _) = dayData[dayOfWeek] ?: Pair(0, 0)
            entries.add(BarEntry(index.toFloat(), steps.toFloat()))
            labels.add(label)
        }
        
        val dataSet = BarDataSet(entries, getString(R.string.steps)).apply {
            color = requireContext().getColor(R.color.Primary)
            valueTextColor = Color.WHITE
            valueTextSize = 10f
        }
        
        val barData = BarData(dataSet)
        chart.data = barData
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setFitBars(true)
        
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.textSize = 10f
        xAxis.textColor = requireContext().getColor(R.color.Text)
        
        chart.axisLeft.setDrawGridLines(false)
        chart.axisRight.isEnabled = false
        chart.axisLeft.textColor = requireContext().getColor(R.color.Text)
        
        chart.invalidate()
    }
    
    /**
     * Update weekly calories line chart
     */
    private fun updateCaloriesChart(view: View, dayData: Map<Int, Pair<Int, Int>>) {
        val chart = view.findViewById<LineChart>(R.id.weeklyCaloriesChart) ?: return
        
        val entries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()
        
        // Order: Sunday, Monday, Tuesday, Wednesday, Thursday, Friday, Saturday
        val dayOrder = listOf(
            Calendar.SUNDAY to getString(R.string.sunday),
            Calendar.MONDAY to getString(R.string.monday),
            Calendar.TUESDAY to getString(R.string.tuesday),
            Calendar.WEDNESDAY to getString(R.string.wednesday),
            Calendar.THURSDAY to getString(R.string.thursday),
            Calendar.FRIDAY to getString(R.string.friday),
            Calendar.SATURDAY to getString(R.string.saturday)
        )
        
        dayOrder.forEachIndexed { index, (dayOfWeek, label) ->
            val (_, calories) = dayData[dayOfWeek] ?: Pair(0, 0)
            entries.add(Entry(index.toFloat(), calories.toFloat()))
            labels.add(label)
        }
        
        val dataSet = LineDataSet(entries, getString(R.string.calories_burned)).apply {
            color = requireContext().getColor(R.color.Accent)
            valueTextColor = requireContext().getColor(R.color.Text)
            valueTextSize = 10f
            lineWidth = 3f
            setCircleColor(requireContext().getColor(R.color.Accent))
            circleRadius = 5f
            setDrawCircleHole(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        
        val lineData = LineData(dataSet)
        chart.data = lineData
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.textSize = 10f
        xAxis.textColor = requireContext().getColor(R.color.Text)
        
        chart.axisLeft.setDrawGridLines(true)
        chart.axisLeft.gridColor = requireContext().getColor(R.color.Background)
        chart.axisRight.isEnabled = false
        chart.axisLeft.textColor = requireContext().getColor(R.color.Text)
        
        chart.invalidate()
    }
    
    private fun updateRecentActivity(view: View, sessions: List<WorkoutSession>) {
        val recentSessions = sessions.sortedByDescending { it.startTime }.take(5)
        
        view.findViewById<RecyclerView>(R.id.recentActivityRecycler)?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = RecentActivityAdapter(recentSessions)
        }
        
        // Show/hide empty state
        view.findViewById<View>(R.id.emptyRecentActivity)?.visibility = 
            if (recentSessions.isEmpty()) View.VISIBLE else View.GONE
        view.findViewById<RecyclerView>(R.id.recentActivityRecycler)?.visibility = 
            if (recentSessions.isEmpty()) View.GONE else View.VISIBLE
    }
}

// Simple adapter for recent activity
class RecentActivityAdapter(private val sessions: List<WorkoutSession>) : 
    RecyclerView.Adapter<RecentActivityAdapter.ViewHolder>() {
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val workoutName: TextView = view.findViewById(R.id.activityWorkoutName)
        val workoutStats: TextView = view.findViewById(R.id.activityWorkoutStats)
        val workoutDate: TextView = view.findViewById(R.id.activityWorkoutDate)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_activity, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val session = sessions[position]
        val context = holder.itemView.context
        holder.workoutName.text = session.workoutName
        holder.workoutStats.text = context.getString(R.string.activity_format, session.caloriesBurned, session.steps)
        
        val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        holder.workoutDate.text = dateFormat.format(Date(session.startTime))
    }
    
    override fun getItemCount() = sessions.size
}
