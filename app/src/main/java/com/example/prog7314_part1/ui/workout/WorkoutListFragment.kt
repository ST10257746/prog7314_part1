package com.example.prog7314_part1.ui.workout

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prog7314_part1.data.local.AppDatabase
import com.example.prog7314_part1.data.local.entity.WorkoutCategory
import com.example.prog7314_part1.data.repository.WorkoutRepository
import com.example.prog7314_part1.databinding.FragmentWorkoutListBinding
import com.example.prog7314_part1.ui.viewmodel.WorkoutViewModel
import com.example.prog7314_part1.ui.viewmodel.WorkoutViewModelFactory
import com.example.prog7314_part1.ui.workout.adapter.WorkoutAdapter
import com.example.prog7314_part1.utils.UserSession
import kotlinx.coroutines.launch

/**
 * Fragment for displaying workouts filtered by category
 */
class WorkoutListFragment : Fragment() {

    private var _binding: FragmentWorkoutListBinding? = null
    private val binding get() = _binding!!

    private val args: WorkoutListFragmentArgs by navArgs()
    private lateinit var viewModel: WorkoutViewModel
    private lateinit var workoutAdapter: WorkoutAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkoutListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViewModel()
        setupUI()
        setupRecyclerView()
        setupSearchFunctionality()
        setupBackButton()
        observeWorkouts()

        // Set initial category filter
        val category = getCategoryFromString(args.categoryName)
        viewModel.setSelectedCategory(category)
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

    private fun setupUI() {
        val categoryName = args.categoryName
        val category = getCategoryFromString(categoryName)

        binding.apply {
            tvCategoryTitle.text = if (categoryName == "All") "All Workouts" else "$categoryName Workouts"
            tvCategoryDescription.text = getCategoryDescription(category)
        }
    }

    private fun setupRecyclerView() {
        workoutAdapter = WorkoutAdapter(
            onWorkoutClick = { workout ->
                // Handle workout card click - show detailed information
                val categoryName = formatCategoryName(workout.category)
                Toast.makeText(
                    requireContext(),
                    "Viewing ${workout.name} - ${categoryName} workout details",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onStartClick = { workout ->
                // Handle start button click - start the workout
                val categoryName = formatCategoryName(workout.category)
                Toast.makeText(
                    requireContext(),
                    "Starting ${workout.name} - ${categoryName} workout (${workout.durationMinutes} min, ${workout.exerciseCount} exercises)",
                    Toast.LENGTH_LONG
                ).show()
            }
        )

        binding.rvCategoryWorkouts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = workoutAdapter
        }
    }

    private fun setupSearchFunctionality() {
        binding.etSearchWorkouts.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                viewModel.setSearchQuery(query)
            }
        })
    }

    private fun setupBackButton() {
        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeWorkouts() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.workouts.collect { workouts ->
                workoutAdapter.submitList(workouts)
                updateWorkoutCount(workouts.size)
                updateEmptyState(workouts.isEmpty())
            }
        }
    }

    private fun updateWorkoutCount(count: Int) {
        val countText = when (count) {
            0 -> "No workouts found"
            1 -> "1 workout found"
            else -> "$count workouts found"
        }
        binding.tvWorkoutCount.text = countText
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.apply {
            if (isEmpty) {
                rvCategoryWorkouts.visibility = View.GONE
                llEmptyState.visibility = View.VISIBLE
            } else {
                rvCategoryWorkouts.visibility = View.VISIBLE
                llEmptyState.visibility = View.GONE
            }
        }
    }

    private fun getCategoryFromString(categoryName: String): WorkoutCategory? {
        return when (categoryName.uppercase()) {
            "CARDIO" -> WorkoutCategory.CARDIO
            "STRENGTH" -> WorkoutCategory.STRENGTH
            "YOGA" -> WorkoutCategory.YOGA
            "HIIT" -> WorkoutCategory.HIIT
            "FLEXIBILITY" -> WorkoutCategory.FLEXIBILITY
            "FULL_BODY" -> WorkoutCategory.FULL_BODY
            "UPPER_BODY" -> WorkoutCategory.UPPER_BODY
            "LOWER_BODY" -> WorkoutCategory.LOWER_BODY
            "CORE" -> WorkoutCategory.CORE
            else -> null // "All" or unknown categories
        }
    }

    private fun getCategoryDescription(category: WorkoutCategory?): String {
        return when (category) {
            WorkoutCategory.CARDIO -> "Burn calories and improve endurance"
            WorkoutCategory.STRENGTH -> "Build muscle and power"
            WorkoutCategory.YOGA -> "Improve flexibility and mindfulness"
            WorkoutCategory.HIIT -> "High-intensity interval training"
            WorkoutCategory.FLEXIBILITY -> "Improve mobility and range of motion"
            WorkoutCategory.FULL_BODY -> "Complete body conditioning"
            WorkoutCategory.UPPER_BODY -> "Focus on arms, chest, and back"
            WorkoutCategory.LOWER_BODY -> "Strengthen legs and glutes"
            WorkoutCategory.CORE -> "Build core strength and stability"
            null -> "All available workouts"
        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}