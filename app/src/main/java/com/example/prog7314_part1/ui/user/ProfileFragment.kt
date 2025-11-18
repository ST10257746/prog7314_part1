package com.example.prog7314_part1.ui.user

import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.prog7314_part1.R
import com.example.prog7314_part1.data.model.Result
import com.example.prog7314_part1.data.repository.ApiUserRepository
import com.example.prog7314_part1.databinding.FragmentProfileBinding
import com.example.prog7314_part1.utils.show
import com.example.prog7314_part1.utils.showToast
import kotlinx.coroutines.launch

/**
 * ProfileFragment
 * Shows user profile, settings, and logout option
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var userRepository: ApiUserRepository
    
    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleImageSelected(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userRepository = ApiUserRepository(requireContext())

        setupClickListeners()
        loadUserData()
    }

    /**
     * Load user data from repository
     */
    private fun loadUserData() {
        viewLifecycleOwner.lifecycleScope.launch {
            userRepository.getCurrentUser().collect { user ->
                // Check if view is still alive before updating UI
                if (_binding == null) return@collect
                
                if (user != null) {
                    // Update UI with user data
                    binding.userNameText.text = user.displayName
                    binding.userEmailText.text = user.email
                    binding.ageText.text = getString(R.string.years_format, user.age)
                    binding.weightText.text = getString(R.string.kg_format, user.weightKg)

                    // Load profile image
                    loadProfileImage(user.profileImageUrl)

                    // Height (if available)
                    user.heightCm?.let {
                        binding.heightLayout.show()
                        binding.heightText.text = getString(R.string.cm_format, it)
                    }

                    // Goals
                    if (user.dailyStepGoal != null) {
                        val goalsText = getString(R.string.goals_format,
                            user.dailyStepGoal ?: 0,
                            user.dailyCalorieGoal ?: 0,
                            user.dailyWaterGoal ?: 0,
                            user.weeklyWorkoutGoal ?: 0,
                            user.proteinGoalG ?: 0,
                            user.carbsGoalG ?: 0,
                            user.fatsGoalG ?: 0
                        )
                        binding.goalsText.text = goalsText
                    } else {
                        binding.goalsText.text = getString(R.string.no_goals_set)
                    }
                } else {
                    binding.userNameText.text = getString(R.string.not_logged_in)
                    binding.userEmailText.text = ""
                }
            }
        }
    }
    
    /**
     * Load profile image using Glide
     * Handles both Base64 encoded images and URLs
     */
    private fun loadProfileImage(imageData: String?) {
        if (imageData != null && imageData.isNotEmpty()) {
            try {
                // Check if it's a Base64 encoded image
                if (imageData.startsWith("data:image") || !imageData.startsWith("http")) {
                    // It's Base64 - decode and load
                    val base64String = if (imageData.contains(",")) {
                        // Remove data:image/jpeg;base64, prefix if present
                        imageData.substring(imageData.indexOf(",") + 1)
                    } else {
                        imageData
                    }
                    
                    val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    
                    Glide.with(this)
                        .load(bitmap)
                        .circleCrop()
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.ic_launcher_foreground)
                        .into(binding.profileImage)
                } else {
                    // It's a URL - load directly
                    Glide.with(this)
                        .load(imageData)
                        .circleCrop()
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.ic_launcher_foreground)
                        .into(binding.profileImage)
                }
            } catch (e: Exception) {
                // If there's any error, show default image
                binding.profileImage.setImageResource(R.drawable.ic_launcher_foreground)
            }
        } else {
            // Show default image
            binding.profileImage.setImageResource(R.drawable.ic_launcher_foreground)
        }
    }

    /**
     * Setup click listeners
     */
    private fun setupClickListeners() {
        // Profile Image Click - Show options
        binding.profileImage.setOnClickListener {
            showImageOptions()
        }
        
        // Edit Goals
        binding.editGoalsButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_setupGoalsFragment)
        }

        // Settings Button - Navigate to Settings (Language selection)
        binding.settingsButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_settingsFragment)
        }

        // Edit Profile (placeholder for now)
        binding.editProfileButton.setOnClickListener {
            requireContext().showToast("Edit profile coming soon!")
        }

        // Logout
        binding.logoutButton.setOnClickListener {
            handleLogout()
        }
    }
    
    /**
     * Show image options dialog
     */
    private fun showImageOptions() {
        val options = arrayOf("Choose from Gallery", "Remove Photo", "Cancel")
        
        AlertDialog.Builder(requireContext())
            .setTitle("Profile Photo")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> imagePickerLauncher.launch("image/*")
                    1 -> handleRemoveImage()
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }
    
    /**
     * Handle image selected from gallery
     */
    private fun handleImageSelected(imageUri: Uri) {
        if (_binding == null) return
        
        // Show loading state
        requireContext().showToast("Processing image...")
        
        viewLifecycleOwner.lifecycleScope.launch {
            // Convert image to Base64
            when (val uploadResult = userRepository.uploadProfileImage(imageUri)) {
                is Result.Success -> {
                    val base64Image = uploadResult.data
                    
                    // Update user profile with Base64 image
                    when (userRepository.updateProfileImage(base64Image)) {
                        is Result.Success -> {
                            if (_binding != null) {
                                requireContext().showToast("Profile image updated!")
                                loadProfileImage(base64Image)
                            }
                        }
                        is Result.Error -> {
                            if (_binding != null) {
                                requireContext().showToast("Failed to update profile")
                            }
                        }
                        else -> {}
                    }
                }
                is Result.Error -> {
                    if (_binding != null) {
                        requireContext().showToast("Failed: ${uploadResult.message}")
                    }
                }
                else -> {}
            }
        }
    }
    
    /**
     * Handle remove profile image
     */
    private fun handleRemoveImage() {
        if (_binding == null) return
        
        AlertDialog.Builder(requireContext())
            .setTitle("Remove Photo")
            .setMessage("Are you sure you want to remove your profile photo?")
            .setPositiveButton("Remove") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    when (userRepository.deleteProfileImage()) {
                        is Result.Success -> {
                            if (_binding != null) {
                                requireContext().showToast("Profile photo removed")
                                loadProfileImage(null)
                            }
                        }
                        is Result.Error -> {
                            if (_binding != null) {
                                requireContext().showToast("Failed to remove photo")
                            }
                        }
                        else -> {}
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Handle logout
     */
    private fun handleLogout() {
        binding.logoutButton.isEnabled = false
        binding.logoutButton.text = getString(R.string.logging_out)

        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = userRepository.logout()) {
                is Result.Success -> {
                    // Check if view is still alive before showing toast
                    if (_binding != null) {
                        requireContext().showToast("Logged out successfully")
                    }
                    // Navigation is handled automatically by MainActivity's auth state observer
                    // No need to navigate manually here
                }
                is Result.Error -> {
                    // Check if view is still alive before updating UI
                    if (_binding != null) {
                        requireContext().showToast("Logout failed: ${result.message}")
                        binding.logoutButton.isEnabled = true
                        binding.logoutButton.text = getString(R.string.logout)
                    }
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
