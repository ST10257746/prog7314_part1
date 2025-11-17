package com.example.prog7314_part1.ui.progress

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
                            String.format("%,d this week", totalSteps)
                        view.findViewById<TextView>(R.id.stepsTargetText)?.text = 
                            "Target: ${String.format("%,d", weeklyGoal)}"
                        view.findViewById<TextView>(R.id.stepsPercentageText)?.text = "$stepsProgress%"
                        view.findViewById<android.widget.ProgressBar>(R.id.stepsProgress)?.progress = stepsProgress
                    }
                    
                    // Update Workouts Progress
                    user.weeklyWorkoutGoal?.let { workoutGoal ->
                        val workoutsProgress = if (workoutGoal > 0) {
                            ((totalWorkouts.toFloat() / workoutGoal) * 100).toInt().coerceIn(0, 100)
                        } else 0
                        
                        view.findViewById<TextView>(R.id.workoutsCurrentText)?.text = 
                            "$totalWorkouts completed"
                        view.findViewById<TextView>(R.id.workoutsTargetText)?.text = 
                            "Target: $workoutGoal workouts"
                        view.findViewById<TextView>(R.id.workoutsPercentageText)?.text = "$workoutsProgress%"
                        view.findViewById<android.widget.ProgressBar>(R.id.workoutsProgress)?.progress = workoutsProgress
                    }
                    
                    // Update Calories Progress
                    user.dailyCalorieGoal?.let { dailyGoal ->
                        val weeklyGoal = dailyGoal * 7
                        val caloriesProgress = if (weeklyGoal > 0) {
                            ((totalCalories.toFloat() / weeklyGoal) * 100).toInt().coerceIn(0, 100)
                        } else 0
                        
                        view.findViewById<TextView>(R.id.caloriesCurrentText)?.text = 
                            String.format("%,d kcal this week", totalCalories)
                        view.findViewById<TextView>(R.id.caloriesTargetText)?.text = 
                            "Target: ${String.format("%,d", weeklyGoal)} kcal"
                        view.findViewById<TextView>(R.id.caloriesPercentageText)?.text = "$caloriesProgress%"
                        view.findViewById<android.widget.ProgressBar>(R.id.caloriesProgress)?.progress = caloriesProgress
                    }
                    
                    // Update Trends Section
                    updateTrends(view, totalDistance, totalWorkouts, weekSessions)
                    
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
        
        view.findViewById<TextView>(R.id.avgWorkoutText)?.text = "$avgDuration min"
        
        // Total distance
        view.findViewById<TextView>(R.id.totalDistanceText)?.text = 
            String.format("%.1f km", totalDistance)
        
        // Workout frequency (this week vs last week)
        view.findViewById<TextView>(R.id.workoutFrequencyText)?.text = 
            "$totalWorkouts workouts"
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
        holder.workoutName.text = session.workoutName
        holder.workoutStats.text = "${session.caloriesBurned} cal • ${session.steps} steps"
        
        val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        holder.workoutDate.text = dateFormat.format(Date(session.startTime))
    }
    
    override fun getItemCount() = sessions.size
}
