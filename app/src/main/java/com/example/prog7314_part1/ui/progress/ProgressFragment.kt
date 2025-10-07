package com.example.prog7314_part1.ui.progress

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.prog7314_part1.R
import com.example.prog7314_part1.data.repository.ApiUserRepository
import kotlinx.coroutines.launch

class ProgressFragment : Fragment() {
    
    private lateinit var userRepository: ApiUserRepository
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_progress, container, false)
        
        userRepository = ApiUserRepository(requireContext())
        
        loadUserGoals(view)
        
        return view
    }
    
    private fun loadUserGoals(view: View) {
        lifecycleScope.launch {
            userRepository.getCurrentUser().collect { user ->
                if (user != null) {
                    // Update weekly steps target (daily goal * 7)
                    user.dailyStepGoal?.let { dailyGoal ->
                        val weeklyGoal = dailyGoal * 7
                        view.findViewById<TextView>(R.id.stepsTargetText)?.text = 
                            "Target: ${String.format("%,d", weeklyGoal)} steps"
                    }
                    
                    // Update workouts target
                    user.weeklyWorkoutGoal?.let { workoutGoal ->
                        view.findViewById<TextView>(R.id.workoutsTargetText)?.text = 
                            "Target: $workoutGoal workouts"
                    }
                    
                    // Update weekly calories target (daily goal * 7)
                    user.dailyCalorieGoal?.let { dailyGoal ->
                        val weeklyGoal = dailyGoal * 7
                        view.findViewById<TextView>(R.id.caloriesTargetText)?.text = 
                            "Target: ${String.format("%,d", weeklyGoal)} kcal"
                    }
                }
            }
        }
    }
}


