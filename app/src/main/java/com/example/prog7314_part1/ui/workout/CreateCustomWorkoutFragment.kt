package com.example.prog7314_part1.ui.workout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prog7314_part1.R
import com.example.prog7314_part1.data.local.AppDatabase
import com.example.prog7314_part1.data.local.entity.Exercise
import com.example.prog7314_part1.data.local.entity.Workout
import com.example.prog7314_part1.data.local.entity.WorkoutCategory
import com.example.prog7314_part1.data.local.entity.WorkoutDifficulty
import com.example.prog7314_part1.data.repository.WorkoutRepository
import com.example.prog7314_part1.databinding.FragmentCreateCustomWorkoutBinding
import com.example.prog7314_part1.ui.viewmodel.WorkoutViewModel
import com.example.prog7314_part1.ui.viewmodel.WorkoutViewModelFactory
import com.example.prog7314_part1.ui.workout.adapter.ExerciseCreateAdapter
import com.example.prog7314_part1.utils.UserSession
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Fragment for creating custom workouts with exercises
 */
class CreateCustomWorkoutFragment : Fragment() {

    private var _binding: FragmentCreateCustomWorkoutBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: WorkoutViewModel
    private lateinit var exerciseAdapter: ExerciseCreateAdapter
    private val exercises = mutableListOf<ExerciseCreateItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateCustomWorkoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViewModel()
        setupSpinners()
        setupRecyclerView()
        setupClickListeners()
        updateEmptyState()
    }

    private fun initializeViewModel() {
        val userId = UserSession.userId ?: return
        val database = AppDatabase.getDatabase(requireContext())
        val workoutDao = database.workoutDao()
        val exerciseDao = database.exerciseDao()
        val workoutRepository = WorkoutRepository(workoutDao, exerciseDao)
        val factory = WorkoutViewModelFactory(workoutRepository)
        viewModel = ViewModelProvider(this, factory)[WorkoutViewModel::class.java]
    }

    private fun setupSpinners() {
        // Category Spinner
        val categories = WorkoutCategory.values().map { formatCategoryName(it) }
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = categoryAdapter

        // Difficulty Spinner
        val difficulties = WorkoutDifficulty.values().map { formatDifficultyName(it) }
        val difficultyAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, difficulties)
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDifficulty.adapter = difficultyAdapter
    }

    private fun setupRecyclerView() {
        exerciseAdapter = ExerciseCreateAdapter(
            exercises = exercises,
            onRemoveExercise = { position ->
                exercises.removeAt(position)
                exerciseAdapter.notifyItemRemoved(position)
                exerciseAdapter.notifyItemRangeChanged(position, exercises.size)
                updateEmptyState()
            }
        )

        binding.rvExercises.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = exerciseAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnAddExercise.setOnClickListener {
            addNewExercise()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSaveWorkout.setOnClickListener {
            saveCustomWorkout()
        }
    }

    private fun addNewExercise() {
        val newExercise = ExerciseCreateItem(
            id = UUID.randomUUID().toString(),
            name = "",
            description = "",
            muscleGroup = "Upper Body",
            sets = null,
            reps = null,
            durationSeconds = null,
            restSeconds = 60
        )
        
        exercises.add(newExercise)
        exerciseAdapter.notifyItemInserted(exercises.size - 1)
        updateEmptyState()

        // Scroll to the new exercise
        binding.rvExercises.smoothScrollToPosition(exercises.size - 1)
    }

    private fun updateEmptyState() {
        binding.layoutEmptyExercises.visibility = if (exercises.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun saveCustomWorkout() {
        val workoutName = binding.etWorkoutName.text.toString().trim()
        val workoutDescription = binding.etWorkoutDescription.text.toString().trim()
        val duration = binding.etDuration.text.toString().toIntOrNull()

        // Validation
        if (workoutName.isEmpty()) {
            binding.etWorkoutName.error = "Workout name is required"
            return
        }

        if (duration == null || duration <= 0) {
            binding.etDuration.error = "Please enter a valid duration"
            return
        }

        if (exercises.isEmpty()) {
            Toast.makeText(requireContext(), "Please add at least one exercise", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate exercises
        val validatedExercises = mutableListOf<Exercise>()
        var hasError = false

        exercises.forEachIndexed { index, exerciseItem ->
            val exerciseData = exerciseAdapter.getExerciseData(index)
            
            if (exerciseData.name.isEmpty()) {
                Toast.makeText(requireContext(), "Exercise ${index + 1}: Name is required", Toast.LENGTH_SHORT).show()
                hasError = true
                return@forEachIndexed
            }

            if (exerciseData.sets == null && exerciseData.durationSeconds == null) {
                Toast.makeText(requireContext(), "Exercise ${index + 1}: Enter either sets/reps or duration", Toast.LENGTH_SHORT).show()
                hasError = true
                return@forEachIndexed
            }

            if (exerciseData.sets != null && exerciseData.reps == null) {
                Toast.makeText(requireContext(), "Exercise ${index + 1}: Reps required when sets are specified", Toast.LENGTH_SHORT).show()
                hasError = true
                return@forEachIndexed
            }

            validatedExercises.add(exerciseData)
        }

        if (hasError) return

        // Create workout
        val selectedCategory = WorkoutCategory.values()[binding.spinnerCategory.selectedItemPosition]
        val selectedDifficulty = WorkoutDifficulty.values()[binding.spinnerDifficulty.selectedItemPosition]

        val userId: String = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        val workoutId = UUID.randomUUID().toString()
        val workout = Workout(
            workoutId = workoutId,
            name = workoutName,
            description = workoutDescription.ifEmpty { "Custom workout created by user" },
            category = selectedCategory,
            difficulty = selectedDifficulty,
            durationMinutes = duration,
            estimatedCalories = calculateEstimatedCalories(duration, selectedCategory),
            exerciseCount = validatedExercises.size,
            isCustom = true,
            createdBy = userId
        )

        // Update exercise workout IDs and order
        val finalExercises = validatedExercises.mapIndexed { index, exercise ->
            exercise.copy(
                workoutId = workoutId,
                orderIndex = index
            )
        }

        // Save to database
        lifecycleScope.launch {
            try {
                val database = AppDatabase.getDatabase(requireContext())
                database.workoutDao().insertWorkout(workout)
                database.exerciseDao().insertExercises(finalExercises)

                Toast.makeText(requireContext(), "Custom workout saved successfully!", Toast.LENGTH_LONG).show()
                findNavController().navigateUp()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error saving workout: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun calculateEstimatedCalories(durationMinutes: Int, category: WorkoutCategory): Int {
        // Simple calorie estimation based on category and duration
        val caloriesPerMinute = when (category) {
            WorkoutCategory.HIIT -> 12
            WorkoutCategory.CARDIO -> 10
            WorkoutCategory.STRENGTH -> 8
            WorkoutCategory.FULL_BODY -> 9
            WorkoutCategory.UPPER_BODY, WorkoutCategory.LOWER_BODY -> 7
            WorkoutCategory.CORE -> 6
            WorkoutCategory.YOGA, WorkoutCategory.FLEXIBILITY -> 4
        }
        return durationMinutes * caloriesPerMinute
    }

    private fun formatCategoryName(category: WorkoutCategory): String {
        return when (category) {
            WorkoutCategory.HIIT -> "HIIT"
            WorkoutCategory.FULL_BODY -> "Full Body"
            WorkoutCategory.UPPER_BODY -> "Upper Body"
            WorkoutCategory.LOWER_BODY -> "Lower Body"
            else -> category.name.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    private fun formatDifficultyName(difficulty: WorkoutDifficulty): String {
        return difficulty.name.lowercase().replaceFirstChar { it.uppercase() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * Data class for exercise creation
 */
data class ExerciseCreateItem(
    val id: String,
    var name: String,
    var description: String,
    var muscleGroup: String,
    var sets: Int?,
    var reps: Int?,
    var durationSeconds: Int?,
    var restSeconds: Int
)
