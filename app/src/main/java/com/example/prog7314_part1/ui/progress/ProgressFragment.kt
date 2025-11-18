package com.example.prog7314_part1.ui.progress

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prog7314_part1.R
import com.example.prog7314_part1.data.local.AppDatabase
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ProgressFragment : Fragment() {
    
    private lateinit var userRepository: ApiUserRepository
    private lateinit var networkRepository: NetworkRepository
    private lateinit var database: AppDatabase
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_progress, container, false)
        
        userRepository = ApiUserRepository(requireContext())
        networkRepository = NetworkRepository(requireContext())
        database = AppDatabase.getDatabase(requireContext())
        
        loadProgressData(view)
        
        return view
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
