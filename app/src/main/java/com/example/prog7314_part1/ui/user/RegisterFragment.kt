package com.example.prog7314_part1.ui.user

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.prog7314_part1.R
import com.example.prog7314_part1.data.model.Result
import com.example.prog7314_part1.data.repository.ApiUserRepository
import com.example.prog7314_part1.databinding.FragmentRegisterBinding
import com.example.prog7314_part1.ui.viewmodel.AuthViewModel
import com.example.prog7314_part1.utils.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

/**
 * RegisterFragment
 * Handles user registration with Firebase Auth and Firestore
 * Also handles Google Sign-In profile completion
 */
class RegisterFragment : Fragment() {
    
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var userRepository: ApiUserRepository
    
    // Navigation args for Google Sign-In profile completion
    private val args: RegisterFragmentArgs by navArgs()
    
    // Google Sign-In launcher
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.let { handleGoogleSignIn(it) }
        } catch (e: ApiException) {
            requireContext().showToast("Google sign-in failed: ${e.message}")
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        userRepository = ApiUserRepository(requireContext())
        
        setupSpinners()
        setupClickListeners()
        observeViewModel()
        
        // Configure UI based on mode (normal registration vs Google profile completion)
        configureForMode()
    }
    
    /**
     * Configure UI based on whether this is normal registration
     * or Google Sign-In profile completion
     */
    private fun configureForMode() {
        if (args.isGoogleProfileCompletion) {
            // Google Sign-In profile completion mode
            binding.apply {
                // Pre-fill email from Google (read-only)
                emailInput.setText(args.googleEmail ?: "")
                emailInput.isEnabled = false
                emailInput.isFocusable = false
                
                // Pre-fill name from Google if available
                if (!args.googleDisplayName.isNullOrBlank()) {
                    nameInput.setText(args.googleDisplayName)
                }
                
                // Hide password fields (Google handles authentication)
                passwordInput.visibility = View.GONE
                confirmPasswordInput.visibility = View.GONE
                
                // Hide Google Sign-In button (already signed in with Google)
                googleRegisterButton.visibility = View.GONE
                
                // Change button text
                registerButton.text = "Complete Profile"
                
                // Change title/label if needed
                // You could add a TextView above the form that says "Complete Your Profile"
            }
        }
    }
    
    /**
     * Setup age and weight spinners
     */
    private fun setupSpinners() {
        // Age spinner: 13-100
        val ageList = (13..100).map { it.toString() }
        val ageAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, ageList)
        ageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.ageSpinner.adapter = ageAdapter
        binding.ageSpinner.setSelection(7) // Default to 20 years old (index 7 = 13+7=20)
        
        // Weight spinner: 30-200 kg
        val weightList = (30..200).map { "$it kg" }
        val weightAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, weightList)
        weightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.weightSpinner.adapter = weightAdapter
        binding.weightSpinner.setSelection(40) // Default to 70 kg (index 40 = 30+40=70)
    }
    
    /**
     * Setup click listeners
     */
    private fun setupClickListeners() {
        // Register button
        binding.registerButton.setOnClickListener {
            requireContext().hideKeyboard(it)
            handleRegister()
        }
        
        // Login link
        binding.loginLink.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
        
        // Google register button
        binding.googleRegisterButton.setOnClickListener {
            startGoogleSignIn()
        }
    }
    
    /**
     * Start Google Sign-In flow
     */
    private fun startGoogleSignIn() {
        val googleSignInClient = userRepository.getGoogleSignInClient()
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }
    
    /**
     * Handle Google Sign-In result
     */
    private fun handleGoogleSignIn(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        if (_binding == null) return
        
        // Show loading state
        binding.registerButton.isEnabled = false
        binding.googleRegisterButton.isEnabled = false
        requireContext().showToast("Signing in with Google...")
        
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = userRepository.signInWithGoogle(account)) {
                is Result.Success -> {
                    val user = result.data
                    
                    if (_binding != null) {
                        requireContext().showToast("Welcome, ${user.displayName}!")
                        
                        // Check if user needs to complete profile setup
                        if (user.age == 0 || user.weightKg == 0.0) {
                            // New Google user - stay on register to complete profile
                            // Pre-fill email and name if available
                            binding.nameInput.setText(user.displayName)
                            binding.emailInput.setText(user.email)
                            binding.emailInput.isEnabled = false // Email from Google can't be changed
                            
                            // Hide password fields since Google handles auth
                            binding.passwordInput.visibility = View.GONE
                            binding.confirmPasswordInput.visibility = View.GONE
                            
                            requireContext().showToast("Please complete your profile")
                            binding.registerButton.isEnabled = true
                            binding.googleRegisterButton.isEnabled = true
                        } else if (user.dailyStepGoal == null) {
                            // Profile set but no goals - go to goal setup
                            findNavController().navigate(R.id.action_registerFragment_to_setupGoalsFragment)
                        } else {
                            // Everything set - go to login (which will redirect to home)
                            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                        }
                    }
                }
                is Result.Error -> {
                    if (_binding != null) {
                        requireContext().showToast("Google sign-in failed: ${result.message}")
                        binding.registerButton.isEnabled = true
                        binding.googleRegisterButton.isEnabled = true
                    }
                }
                else -> {}
            }
        }
    }
    
    /**
     * Handle registration or profile completion
     */
    private fun handleRegister() {
        val name = binding.nameInput.text.toString().trim()
        val email = binding.emailInput.text.toString().trim()
        
        // Get age from spinner
        val ageString = binding.ageSpinner.selectedItem.toString()
        val age = ageString.toIntOrNull() ?: 0
        
        // Get weight from spinner (remove " kg" suffix)
        val weightString = binding.weightSpinner.selectedItem.toString().replace(" kg", "")
        val weight = weightString.toDoubleOrNull() ?: 0.0
        
        if (args.isGoogleProfileCompletion) {
            // Google Sign-In profile completion mode
            handleGoogleProfileCompletion(name, age, weight)
        } else {
            // Normal registration mode
            val password = binding.passwordInput.text.toString()
            val confirmPassword = binding.confirmPasswordInput.text.toString()
            
            viewModel.register(
                email = email,
                password = password,
                confirmPassword = confirmPassword,
                displayName = name,
                age = age,
                weightKg = weight
            )
        }
    }
    
    /**
     * Handle completing profile for Google Sign-In user
     */
    private fun handleGoogleProfileCompletion(name: String, age: Int, weight: Double) {
        if (_binding == null) return
        
        // Validate inputs
        if (name.isBlank()) {
            requireContext().showToast("Please enter your name")
            return
        }
        
        if (age < 13 || age > 100) {
            requireContext().showToast("Please select a valid age")
            return
        }
        
        if (weight < 30 || weight > 200) {
            requireContext().showToast("Please select a valid weight")
            return
        }
        
        // Show loading state
        binding.registerButton.isEnabled = false
        requireContext().showToast("Completing profile...")
        
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = userRepository.completeGoogleProfile(name, age, weight)) {
                is Result.Success -> {
                    if (_binding != null) {
                        requireContext().showToast("Profile completed!")
                        // Navigate to goals setup
                        findNavController().navigate(R.id.action_registerFragment_to_setupGoalsFragment)
                    }
                }
                is Result.Error -> {
                    if (_binding != null) {
                        requireContext().showToast("Failed to update profile: ${result.message}")
                        binding.registerButton.isEnabled = true
                    }
                }
                else -> {
                    if (_binding != null) {
                        binding.registerButton.isEnabled = true
                    }
                }
            }
        }
    }
    
    /**
     * Observe ViewModel state changes
     */
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Observe loading state
            viewModel.isLoading.collect { isLoading ->
                binding.registerButton.isEnabled = !isLoading
                binding.registerButton.text = if (isLoading) "Creating Account..." else "Register"
                
                // Disable inputs during loading
                binding.nameInput.isEnabled = !isLoading
                binding.emailInput.isEnabled = !isLoading
                binding.passwordInput.isEnabled = !isLoading
                binding.confirmPasswordInput.isEnabled = !isLoading
                binding.ageSpinner.isEnabled = !isLoading
                binding.weightSpinner.isEnabled = !isLoading
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            // Observe error messages
            viewModel.errorMessage.collect { error ->
                error?.let {
                    requireContext().showToast(it, android.widget.Toast.LENGTH_LONG)
                    viewModel.clearErrorMessage()
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            // Observe success messages
            viewModel.successMessage.collect { message ->
                message?.let {
                    requireContext().showToast(it)
                    viewModel.clearSuccessMessage()
                    
                    // Navigate to goals setup after successful registration
                    findNavController().navigate(R.id.action_registerFragment_to_setupGoalsFragment)
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}