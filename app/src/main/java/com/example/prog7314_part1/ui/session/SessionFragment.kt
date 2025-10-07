package com.example.prog7314_part1.ui.session

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.prog7314_part1.R
import com.example.prog7314_part1.databinding.FragmentSessionBinding
import com.example.prog7314_part1.data.local.AppDatabase
import com.example.prog7314_part1.data.repository.ApiUserRepository
import kotlinx.coroutines.launch

class SessionFragment : Fragment() {

    private var _binding: FragmentSessionBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SessionViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize ViewModel with database and user repository
        val database = AppDatabase.getDatabase(requireContext())
        val userRepository = ApiUserRepository(requireContext())
        viewModel = SessionViewModel(database.workoutSessionDao(), userRepository)
        
        setupWorkoutTypeSelection()
        setupControlButtons()
        observeSessionState()
    }

    private fun setupWorkoutTypeSelection() {
        // Set up click listeners for workout type cards
        binding.runningCard.setOnClickListener {
            viewModel.selectWorkoutType(SessionViewModel.availableWorkoutTypes[0])
        }
        
        binding.cyclingCard.setOnClickListener {
            viewModel.selectWorkoutType(SessionViewModel.availableWorkoutTypes[1])
        }
        
        binding.walkingCard.setOnClickListener {
            viewModel.selectWorkoutType(SessionViewModel.availableWorkoutTypes[2])
        }
        
        binding.strengthCard.setOnClickListener {
            viewModel.selectWorkoutType(SessionViewModel.availableWorkoutTypes[3])
        }
        
        binding.yogaCard.setOnClickListener {
            viewModel.selectWorkoutType(SessionViewModel.availableWorkoutTypes[4])
        }
        
        binding.hiitCard.setOnClickListener {
            viewModel.selectWorkoutType(SessionViewModel.availableWorkoutTypes[5])
        }
    }

    private fun setupControlButtons() {
        binding.btnStart.setOnClickListener {
            viewModel.startSession()
        }
        
        binding.btnPause.setOnClickListener {
            val state = viewModel.sessionState.value
            if (state.isSessionPaused) {
                viewModel.resumeSession()
                binding.btnPause.text = "⏸ PAUSE"
            } else {
                viewModel.pauseSession()
                binding.btnPause.text = "▶ RESUME"
            }
        }
        
        binding.btnStop.setOnClickListener {
            viewModel.stopSession()
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
        // Update selected workout info
        if (state.selectedWorkoutType != null) {
            binding.selectedWorkoutInfo.visibility = View.VISIBLE
            binding.selectedWorkoutName.text = "Selected: ${state.selectedWorkoutType.name}"
            binding.selectedWorkoutDetails.text = "${state.selectedWorkoutType.category} • ${state.selectedWorkoutType.description}"
        } else {
            binding.selectedWorkoutInfo.visibility = View.GONE
        }

        // Update timer
        binding.timerText.text = state.timerText

        // Update control buttons
        when {
            state.isSessionActive && !state.isSessionPaused -> {
                binding.btnStart.isEnabled = false
                binding.btnStart.text = "▶ ACTIVE"
                binding.btnPause.isEnabled = true
                binding.btnPause.text = "⏸ PAUSE"
                binding.btnStop.isEnabled = true
            }
            state.isSessionPaused -> {
                binding.btnStart.isEnabled = false
                binding.btnStart.text = "▶ ACTIVE"
                binding.btnPause.isEnabled = true
                binding.btnPause.text = "▶ RESUME"
                binding.btnStop.isEnabled = true
            }
            else -> {
                binding.btnStart.isEnabled = state.selectedWorkoutType != null
                binding.btnStart.text = "▶ START"
                binding.btnPause.isEnabled = false
                binding.btnPause.text = "⏸ PAUSE"
                binding.btnStop.isEnabled = false
            }
        }

        // Disable workout selection during active session
        val workoutCards = listOf(
            binding.runningCard,
            binding.cyclingCard,
            binding.walkingCard,
            binding.strengthCard,
            binding.yogaCard,
            binding.hiitCard
        )
        
        workoutCards.forEach { card ->
            card.isEnabled = !state.isSessionActive
            card.alpha = if (state.isSessionActive) 0.5f else 1.0f
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}