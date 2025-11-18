package com.example.prog7314_part1.ui.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.prog7314_part1.R
import com.example.prog7314_part1.data.local.entity.User
import com.example.prog7314_part1.data.repository.ApiUserRepository
import com.example.prog7314_part1.databinding.FragmentEditProfileBinding
import com.example.prog7314_part1.utils.showToast
import kotlinx.coroutines.launch

/**
 * EditProfileFragment
 * Full-page fragment for editing user profile information (name, age, weight, height)
 * Pre-fills with current user data
 */
class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var userRepository: ApiUserRepository
    private var currentUser: User? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userRepository = ApiUserRepository(requireContext())

        setupSpinners()
        loadUserData()
        setupClickListeners()
    }

    /**
     * Setup age and weight spinners
     */
    private fun setupSpinners() {
        // Age spinner (13-100)
        val ages = (13..100).map { it.toString() }
        val ageAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, ages)
        ageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.ageSpinner.adapter = ageAdapter

        // Weight spinner (30-200 kg, increments of 0.5)
        val weights = mutableListOf<String>()
        var weight = 30.0
        while (weight <= 200.0) {
            weights.add(String.format("%.1f", weight))
            weight += 0.5
        }
        val weightAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, weights)
        weightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.weightSpinner.adapter = weightAdapter
    }

    /**
     * Load current user data and pre-fill form
     */
    private fun loadUserData() {
        viewLifecycleOwner.lifecycleScope.launch {
            currentUser = userRepository.getCurrentUserSuspend()
            currentUser?.let { user ->
                // Pre-fill form fields
                binding.nameInput.setText(user.displayName)
                
                // Set age spinner
                val ageIndex = (user.age - 13).coerceIn(0, 87)
                binding.ageSpinner.setSelection(ageIndex)
                
                // Set weight spinner (find closest match)
                val weightString = String.format("%.1f", user.weightKg)
                val weightAdapter = binding.weightSpinner.adapter as ArrayAdapter<*>
                for (i in 0 until weightAdapter.count) {
                    if (weightAdapter.getItem(i).toString() == weightString) {
                        binding.weightSpinner.setSelection(i)
                        break
                    }
                }
                
                // Set height if available
                user.heightCm?.let {
                    binding.heightInput.setText(it.toInt().toString())
                }
            }
        }
    }

    /**
     * Setup click listeners
     */
    private fun setupClickListeners() {
        binding.saveButton.setOnClickListener {
            saveProfile()
        }

        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    /**
     * Save profile changes
     */
    private fun saveProfile() {
        val name = binding.nameInput.text.toString().trim()
        
        if (name.isBlank()) {
            requireContext().showToast("Please enter your name")
            return
        }

        val age = binding.ageSpinner.selectedItem.toString().toInt()
        val weight = binding.weightSpinner.selectedItem.toString().toDouble()
        val heightText = binding.heightInput.text.toString().trim()
        val height = if (heightText.isNotEmpty()) {
            try {
                heightText.toDouble()
            } catch (e: NumberFormatException) {
                null
            }
        } else {
            null
        }

        // Validate height if provided
        height?.let {
            if (it < 50 || it > 250) {
                requireContext().showToast("Height must be between 50 and 250 cm")
                return
            }
        }

        val user = currentUser ?: return

        binding.saveButton.isEnabled = false
        binding.saveButton.text = getString(R.string.saving)

        viewLifecycleOwner.lifecycleScope.launch {
            val updatedUser = user.copy(
                displayName = name,
                age = age,
                weightKg = weight,
                heightCm = height,
                updatedAt = System.currentTimeMillis()
            )

            when (val result = userRepository.updateUser(updatedUser)) {
                is com.example.prog7314_part1.data.model.Result.Success -> {
                    requireContext().showToast(getString(R.string.profile_updated))
                    findNavController().navigateUp()
                }
                is com.example.prog7314_part1.data.model.Result.Error -> {
                    requireContext().showToast("Failed to update: ${result.message}")
                    binding.saveButton.isEnabled = true
                    binding.saveButton.text = getString(R.string.save_changes)
                }
                else -> {
                    binding.saveButton.isEnabled = true
                    binding.saveButton.text = getString(R.string.save_changes)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

