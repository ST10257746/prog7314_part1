package com.example.prog7314_part1.ui.user

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.prog7314_part1.R
import com.example.prog7314_part1.data.model.Result
import com.example.prog7314_part1.data.repository.ApiUserRepository
import com.example.prog7314_part1.databinding.FragmentSetupGoalsBinding
import com.example.prog7314_part1.utils.hideKeyboard
import com.example.prog7314_part1.utils.showToast
import kotlinx.coroutines.launch

/**
 * SetupGoalsFragment
 * Onboarding screen for new users to set their fitness goals
 */
class SetupGoalsFragment : Fragment() {

    private var _binding: FragmentSetupGoalsBinding? = null
    private val binding get() = _binding!!

    private lateinit var userRepository: ApiUserRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetupGoalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userRepository = ApiUserRepository(requireContext())

        setupClickListeners()
        loadExistingGoals()
    }

    /**
     * Load existing goals if available, otherwise set suggested defaults
     */
    private fun loadExistingGoals() {
        viewLifecycleOwner.lifecycleScope.launch {
            val user = userRepository.getCurrentUserSuspend()
            
            if (user != null && user.dailyStepGoal != null) {
                // User has existing goals - pre-fill them
                user.heightCm?.let { 
                    binding.heightInput.setText(it.toInt().toString()) 
                }
                binding.stepGoalInput.setText(user.dailyStepGoal.toString())
                binding.calorieGoalInput.setText(user.dailyCalorieGoal.toString())
                binding.waterGoalInput.setText(user.dailyWaterGoal.toString())
                binding.workoutGoalInput.setText(user.weeklyWorkoutGoal.toString())
                binding.proteinGoalInput.setText(user.proteinGoalG.toString())
                binding.carbsGoalInput.setText(user.carbsGoalG.toString())
                binding.fatsGoalInput.setText(user.fatsGoalG.toString())
                
                // Update button text for editing
                binding.continueButton.text = "Save Changes"
            } else {
                // New user - set suggested default values
                setDefaultValues()
            }
        }
    }

    /**
     * Set suggested default values for new users
     */
    private fun setDefaultValues() {
        binding.stepGoalInput.setText("10000")
        binding.calorieGoalInput.setText("2200")
        binding.waterGoalInput.setText("8")
        binding.workoutGoalInput.setText("5")
        binding.proteinGoalInput.setText("150")
        binding.carbsGoalInput.setText("250")
        binding.fatsGoalInput.setText("70")
    }

    /**
     * Setup click listeners
     */
    private fun setupClickListeners() {
        binding.continueButton.setOnClickListener {
            requireContext().hideKeyboard(it)
            handleSaveGoals()
        }
    }

    /**
     * Handle saving goals
     */
    private fun handleSaveGoals() {
        // Get height (optional)
        val heightText = binding.heightInput.text.toString()
        val heightCm = if (heightText.isNotEmpty()) heightText.toDoubleOrNull() else null
        
        // Get values
        val stepGoal = binding.stepGoalInput.text.toString().toIntOrNull()
        val calorieGoal = binding.calorieGoalInput.text.toString().toIntOrNull()
        val waterGoal = binding.waterGoalInput.text.toString().toIntOrNull()
        val workoutGoal = binding.workoutGoalInput.text.toString().toIntOrNull()
        val proteinGoal = binding.proteinGoalInput.text.toString().toIntOrNull()
        val carbsGoal = binding.carbsGoalInput.text.toString().toIntOrNull()
        val fatsGoal = binding.fatsGoalInput.text.toString().toIntOrNull()

        // Validation (height is optional, others are required)
        if (heightText.isNotEmpty() && (heightCm == null || heightCm <= 0 || heightCm > 300)) {
            requireContext().showToast("Please enter a valid height (or leave empty)")
            return
        }
        if (stepGoal == null || stepGoal <= 0) {
            requireContext().showToast("Please enter a valid step goal")
            return
        }
        if (calorieGoal == null || calorieGoal <= 0) {
            requireContext().showToast("Please enter a valid calorie goal")
            return
        }
        if (waterGoal == null || waterGoal <= 0) {
            requireContext().showToast("Please enter a valid water goal")
            return
        }
        if (workoutGoal == null || workoutGoal <= 0) {
            requireContext().showToast("Please enter a valid workout goal")
            return
        }
        if (proteinGoal == null || proteinGoal < 0) {
            requireContext().showToast("Please enter a valid protein goal")
            return
        }
        if (carbsGoal == null || carbsGoal < 0) {
            requireContext().showToast("Please enter a valid carbs goal")
            return
        }
        if (fatsGoal == null || fatsGoal < 0) {
            requireContext().showToast("Please enter a valid fats goal")
            return
        }

        // Show loading
        binding.continueButton.isEnabled = false
        binding.continueButton.text = getString(R.string.saving)

        // Save profile (height + goals)
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = userRepository.completeProfileSetup(
                heightCm = heightCm,
                dailyStepGoal = stepGoal,
                dailyCalorieGoal = calorieGoal,
                dailyWaterGoal = waterGoal,
                weeklyWorkoutGoal = workoutGoal,
                proteinGoalG = proteinGoal,
                carbsGoalG = carbsGoal,
                fatsGoalG = fatsGoal
            )) {
                is Result.Success -> {
                    // Check if view is still alive
                    if (_binding == null) return@launch
                    
                    requireContext().showToast("Goals saved successfully!")
                    
                    // Navigate back or to home
                    val navController = findNavController()
                    if (navController.previousBackStackEntry != null) {
                        // Editing mode - go back to previous screen
                        navController.popBackStack()
                    } else {
                        // First time setup - navigate to home
                        navController.navigate(R.id.action_setupGoalsFragment_to_homeFragment)
                    }
                }
                is Result.Error -> {
                    // Check if view is still alive
                    if (_binding == null) return@launch
                    
                    requireContext().showToast("Error: ${result.message}")
                    binding.continueButton.isEnabled = true
                    binding.continueButton.text = getString(R.string.continue_button)
                }
                else -> {}
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
