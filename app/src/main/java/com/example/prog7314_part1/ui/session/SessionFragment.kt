package com.example.prog7314_part1.ui.session

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prog7314_part1.R
import com.example.prog7314_part1.data.local.AppDatabase
import com.example.prog7314_part1.data.repository.ApiUserRepository
import com.example.prog7314_part1.databinding.FragmentSessionBinding
import com.example.prog7314_part1.ui.workout.adapter.ExerciseAdapter
import kotlinx.coroutines.launch

class SessionFragment : Fragment() {

    private var _binding: FragmentSessionBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SessionViewModel
    private lateinit var exerciseAdapter: ExerciseAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize ViewModel with database, user repository, and context for API sync
        val database = AppDatabase.getDatabase(requireContext())
        val userRepository = ApiUserRepository(requireContext())
        viewModel = SessionViewModel(
            database.workoutSessionDao(),
            userRepository,
            requireContext(),
            database.exerciseDao(),
            database.workoutDao()
        )
        
        setupWorkoutTypeSelection()
        setupControlButtons()
        setupExerciseRecyclerView()
        observeSessionState()
        observeSelectedWorkout()

        // Load preselected workout from navArgs
        val workoutId = arguments?.getString("workoutId")
        workoutId?.let { id ->
            viewModel.loadWorkoutWithExercises(id)

            // Automatically select correct card once workout is loaded
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.selectedWorkout.collect { workout ->
                    workout?.let {
                        val type = SessionViewModel.getWorkoutTypeForWorkout(it)
                        type?.let { viewModel.selectWorkoutType(it) }
                    }
                }
            }
        }
    }

    private fun setupWorkoutTypeSelection() {
        val types = SessionViewModel.availableWorkoutTypes
        binding.runningCard.setOnClickListener { viewModel.selectWorkoutType(types[0]) }
        binding.cyclingCard.setOnClickListener { viewModel.selectWorkoutType(types[1]) }
        binding.walkingCard.setOnClickListener { viewModel.selectWorkoutType(types[2]) }
        binding.strengthCard.setOnClickListener { viewModel.selectWorkoutType(types[3]) }
        binding.yogaCard.setOnClickListener { viewModel.selectWorkoutType(types[4]) }
        binding.hiitCard.setOnClickListener { viewModel.selectWorkoutType(types[5]) }
    }

    private fun setupControlButtons() {
        binding.btnStart.setOnClickListener { viewModel.startSession() }

        binding.btnPause.setOnClickListener {
            val state = viewModel.sessionState.value
            if (state.isSessionPaused) {
                viewModel.resumeSession()
                binding.btnPause.text = getString(R.string.pause_button)
            } else {
                viewModel.pauseSession()
                binding.btnPause.text = getString(R.string.resume_button)
            }
        }

        binding.btnStop.setOnClickListener { viewModel.stopSession() }

        binding.btnConnectWatch.setOnClickListener {
            val state = viewModel.sessionState.value
            if (state.watchMetrics.isConnected) viewModel.disconnectWatch()
            else viewModel.connectWatch()
        }
    }

    private fun setupExerciseRecyclerView() {
        exerciseAdapter = ExerciseAdapter()
        binding.exercisesRecyclerView.apply {
            adapter = exerciseAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeSelectedWorkout() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedWorkout.collect { workout ->
                if (workout != null) {
                    binding.selectedWorkoutInfo.visibility = View.VISIBLE
                    binding.selectedWorkoutName.text = getString(R.string.selected_colon, workout.name)
                    binding.selectedWorkoutDetails.text = workout.description
                    binding.exercisesRecyclerView.visibility = View.VISIBLE
                    exerciseAdapter.submitList(workout.exercises)
                } else {
                    binding.selectedWorkoutInfo.visibility = View.GONE
                    binding.exercisesRecyclerView.visibility = View.GONE
                    exerciseAdapter.submitList(emptyList())
                }
            }
        }
    }

    private fun observeSessionState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sessionState.collect { state ->
                updateUI(state)
            }
        }
    }

    private fun updateUI(state: SessionState) {
        // Timer
        binding.timerText.text = state.timerText

        // Control buttons
        when {
            state.isSessionActive && !state.isSessionPaused -> {
                binding.btnStart.isEnabled = false
                binding.btnStart.text = getString(R.string.active_button)
                binding.btnPause.isEnabled = true
                binding.btnPause.text = getString(R.string.pause_button)
                binding.btnStop.isEnabled = true
            }
            state.isSessionPaused -> {
                binding.btnStart.isEnabled = false
                binding.btnStart.text = getString(R.string.active_button)
                binding.btnPause.isEnabled = true
                binding.btnPause.text = getString(R.string.resume_button)
                binding.btnStop.isEnabled = true
            }
            else -> {
                binding.btnStart.isEnabled =
                    state.selectedWorkoutType != null || viewModel.selectedWorkout.value != null
                binding.btnStart.text = getString(R.string.start_button)
                binding.btnPause.isEnabled = false
                binding.btnPause.text = getString(R.string.pause_button)
                binding.btnStop.isEnabled = false
            }
        }

        // Disable workout type cards
        val cards = listOf(
            binding.runningCard,
            binding.cyclingCard,
            binding.walkingCard,
            binding.strengthCard,
            binding.yogaCard,
            binding.hiitCard
        )

        val disableCards =
            state.isSessionActive || state.selectedWorkoutType != null || viewModel.selectedWorkout.value != null
        cards.forEach { card ->
            card.isEnabled = !disableCards
            card.alpha = if (disableCards) 0.5f else 1.0f
        }

        // Highlight selected card by comparing **category**, not object reference
        state.selectedWorkoutType?.let { type ->
            val card = getCardForWorkoutType(type)
            card?.alpha = 1.0f
            card?.isEnabled = false
        }

        // Watch metrics
        updateMetricsDisplay(state.watchMetrics)
    }

    private fun getCardForWorkoutType(type: WorkoutType) = when (type.category) {
        SessionViewModel.availableWorkoutTypes[0].category -> binding.runningCard
        SessionViewModel.availableWorkoutTypes[1].category -> binding.cyclingCard
        SessionViewModel.availableWorkoutTypes[2].category -> binding.walkingCard
        SessionViewModel.availableWorkoutTypes[3].category -> binding.strengthCard
        SessionViewModel.availableWorkoutTypes[4].category -> binding.yogaCard
        SessionViewModel.availableWorkoutTypes[5].category -> binding.hiitCard
        else -> null
    }

    private fun updateMetricsDisplay(metrics: WatchMetrics) {
        if (metrics.isConnected) {
            binding.heartRateValue.text = metrics.heartRate.toString()
            binding.caloriesValue.text = metrics.calories.toString()
            binding.stepsValue.text = metrics.steps.toString()
            binding.distanceValue.text = String.format("%.2f", metrics.distanceKm)
            binding.activeTimeValue.text = metrics.activeTimeMinutes.toString()
            binding.watchStatusText.text = getString(R.string.watch_connected)
            binding.watchStatusSubtext.text = getString(R.string.live_metrics_updating)
            binding.btnConnectWatch.text = getString(R.string.disconnect)
            binding.watchStatusIcon.alpha = 1.0f
        } else {
            binding.heartRateValue.text = "--"
            binding.caloriesValue.text = "--"
            binding.stepsValue.text = "--"
            binding.distanceValue.text = "--"
            binding.activeTimeValue.text = "--"
            binding.watchStatusText.text = getString(R.string.watch_disconnected)
            binding.watchStatusSubtext.text = getString(R.string.connect_smartwatch)
            binding.btnConnectWatch.text = getString(R.string.connect)
            binding.watchStatusIcon.alpha = 0.5f
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
