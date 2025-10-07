package com.example.prog7314_part1.ui.workout

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.navigation.fragment.findNavController
import com.example.prog7314_part1.R
import com.example.prog7314_part1.data.local.AppDatabase
import com.example.prog7314_part1.data.local.entity.Workout
import com.example.prog7314_part1.data.local.entity.WorkoutCategory
import com.example.prog7314_part1.data.repository.WorkoutRepository
import com.example.prog7314_part1.databinding.FragmentWorkoutBinding
import com.example.prog7314_part1.ui.viewmodel.WorkoutViewModel
import com.example.prog7314_part1.ui.viewmodel.WorkoutViewModelFactory
import com.example.prog7314_part1.ui.workout.adapter.WorkoutAdapter
import com.example.prog7314_part1.utils.UserSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WorkoutFragment : Fragment() {

    private var _binding: FragmentWorkoutBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: WorkoutViewModel
    private lateinit var workoutAdapter: WorkoutAdapter
    private lateinit var customWorkoutAdapter: WorkoutAdapter

    private var searchJob: Job? = null
    private var selectedButton: View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWorkoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViewModel()
        setupWorkoutRecyclerView()
        setupCustomWorkoutRecyclerView()
        setupSearchFunctionality()
        setupCategoryButtons()
        setupCreateCustomWorkoutButton()
        observeWorkouts()
        observeCustomWorkouts()
    }

    private fun initializeViewModel() {
        val userId = UserSession.userId ?: return
        val database = AppDatabase.getDatabase(requireContext())
        val workoutDao = database.workoutDao()
        val exerciseDao = database.exerciseDao()
        val repository = WorkoutRepository(workoutDao, exerciseDao)
        val factory = WorkoutViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[WorkoutViewModel::class.java]
    }

    /** Main vertical workout list */
    private fun setupWorkoutRecyclerView() {
        workoutAdapter = WorkoutAdapter(
            onWorkoutClick = { workout -> Toast.makeText(requireContext(), "Viewing ${workout.name}", Toast.LENGTH_SHORT).show() },
            onStartClick = { workout -> Toast.makeText(requireContext(), "Starting ${workout.name}", Toast.LENGTH_SHORT).show() }
        )
        binding.rvWorkouts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = workoutAdapter
        }
    }

    /** Horizontal custom workout list */
    private fun setupCustomWorkoutRecyclerView() {
        customWorkoutAdapter = WorkoutAdapter(
            onWorkoutClick = { workout -> Toast.makeText(requireContext(), "Viewing ${workout.name}", Toast.LENGTH_SHORT).show() },
            onStartClick = { workout -> Toast.makeText(requireContext(), "Starting ${workout.name}", Toast.LENGTH_SHORT).show() }
        )
        binding.rvCustomWorkouts.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
            adapter = customWorkoutAdapter
        }
    }

    private fun setupSearchFunctionality() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(600)
                    viewModel.setSearchQuery(query)
                }
            }
        })
    }

    private fun setupCategoryButtons() {
        val buttonsMap = mapOf(
            binding.btnCategoryAll to null,
            binding.btnCategoryCardio to WorkoutCategory.CARDIO,
            binding.btnCategoryStrength to WorkoutCategory.STRENGTH,
            binding.btnCategoryYoga to WorkoutCategory.YOGA,
            binding.btnCategoryHiit to WorkoutCategory.HIIT,
            binding.btnCategoryFlexibility to WorkoutCategory.FLEXIBILITY
        )

        buttonsMap.forEach { (button, category) ->
            button.setOnClickListener {
                viewModel.setSelectedCategory(category)
                highlightSelectedButton(button)
            }
        }
    }

    private fun highlightSelectedButton(button: View) {
        selectedButton?.let { prev ->
            prev.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.Background))
            if (prev is android.widget.Button) prev.setTextColor(ContextCompat.getColor(requireContext(), R.color.Text))
        }
        button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.Primary))
        if (button is android.widget.Button) button.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        selectedButton = button
    }

    private fun setupCreateCustomWorkoutButton() {
        binding.btnCreateCustomWorkout.setOnClickListener {
            val action = WorkoutFragmentDirections.actionWorkoutToCreateCustomWorkout()
            findNavController().navigate(action)
        }
    }

    /** Observe main workouts list */
    private fun observeWorkouts() {
        lifecycleScope.launch {
            viewModel.workouts.collectLatest { workouts ->
                workoutAdapter.submitList(workouts)
                val query = viewModel.searchQuery.value
                if (query.isNotBlank() && workouts.isEmpty()) {
                    Toast.makeText(requireContext(), "No workouts found for '$query'", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /** Observe custom workouts created by the current user */
    private fun observeCustomWorkouts() {
        val userId = UserSession.userId ?: return
        lifecycleScope.launch {
            viewModel.getCustomWorkoutsByUser(userId).collectLatest { customWorkouts ->
                if (customWorkouts.isEmpty()) {
                    binding.tvNoCustomWorkouts.visibility = View.VISIBLE
                    binding.rvCustomWorkouts.visibility = View.GONE
                } else {
                    binding.tvNoCustomWorkouts.visibility = View.GONE
                    binding.rvCustomWorkouts.visibility = View.VISIBLE
                    customWorkoutAdapter.submitList(customWorkouts)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
