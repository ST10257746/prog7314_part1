package com.example.prog7314_part1.ui.user

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.prog7314_part1.R
import com.example.prog7314_part1.data.model.Result
import com.example.prog7314_part1.data.repository.UserRepository
import com.example.prog7314_part1.databinding.FragmentLoginBinding
import com.example.prog7314_part1.ui.viewmodel.AuthViewModel
import com.example.prog7314_part1.utils.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

/**
 * LoginFragment
 * Handles user login with Firebase Auth
 */
class LoginFragment : Fragment() {
    
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var userRepository: UserRepository
    
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
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        userRepository = UserRepository(requireContext())
        
        setupClickListeners()
        observeViewModel()
        checkExistingSession()
    }
    
    /**
     * Check if user is already logged in
     */
    private fun checkExistingSession() {
        lifecycleScope.launch {
            val user = userRepository.getCurrentUserSuspend()
            if (user != null) {
                // User is already logged in, navigate to appropriate screen
                if (user.dailyStepGoal != null && 
                    user.dailyCalorieGoal != null && 
                    user.dailyWaterGoal != null &&
                    user.weeklyWorkoutGoal != null) {
                    // Goals already set, go to home
                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                } else {
                    // Goals not set, go to setup
                    findNavController().navigate(R.id.action_loginFragment_to_setupGoalsFragment)
                }
            }
        }
    }
    
    /**
     * Setup click listeners
     */
    private fun setupClickListeners() {
        // Login button
        binding.loginButton.setOnClickListener {
            requireContext().hideKeyboard(it)
            handleLogin()
        }
        
        // Register link
        binding.registerLink.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
        
        // Google login button
        binding.googleLoginButton.setOnClickListener {
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
        binding.loginButton.isEnabled = false
        binding.googleLoginButton.isEnabled = false
        requireContext().showToast("Signing in with Google...")
        
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = userRepository.signInWithGoogle(account)) {
                is Result.Success -> {
                    val user = result.data
                    
                    if (_binding != null) {
                        requireContext().showToast("Welcome, ${user.displayName}!")
                        
                        // Check if user needs to complete profile setup
                        if (user.age == 0 || user.weightKg == 0.0) {
                            // New Google user - navigate to profile completion
                            val action = LoginFragmentDirections
                                .actionLoginFragmentToRegisterFragment(
                                    isGoogleProfileCompletion = true,
                                    googleEmail = user.email,
                                    googleDisplayName = user.displayName
                                )
                            findNavController().navigate(action)
                        } else if (user.dailyStepGoal == null) {
                            // Profile set but no goals - go to goal setup
                            findNavController().navigate(R.id.action_loginFragment_to_setupGoalsFragment)
                        } else {
                            // Everything set - go to home
                            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                        }
                    }
                }
                is Result.Error -> {
                    if (_binding != null) {
                        requireContext().showToast("Google sign-in failed: ${result.message}")
                        binding.loginButton.isEnabled = true
                        binding.googleLoginButton.isEnabled = true
                    }
                }
                else -> {}
            }
        }
    }
    
    /**
     * Handle login
     */
    private fun handleLogin() {
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString()
        
        // Call ViewModel to login
        viewModel.login(email, password)
    }
    
    /**
     * Observe ViewModel state changes
     */
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Observe loading state
            viewModel.isLoading.collect { isLoading ->
                binding.loginButton.isEnabled = !isLoading
                binding.loginButton.text = if (isLoading) "Logging In..." else "Log In"
                
                // Disable inputs during loading
                binding.emailInput.isEnabled = !isLoading
                binding.passwordInput.isEnabled = !isLoading
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
                    
                    // Check if user has completed goal setup
                    lifecycleScope.launch {
                        val user = userRepository.getCurrentUserSuspend()
                        if (user != null) {
                            // Check if goals are set
                            if (user.dailyStepGoal != null && 
                                user.dailyCalorieGoal != null && 
                                user.dailyWaterGoal != null &&
                                user.weeklyWorkoutGoal != null) {
                                // Goals already set, go to home
                                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                            } else {
                                // Goals not set, go to setup
                                findNavController().navigate(R.id.action_loginFragment_to_setupGoalsFragment)
                            }
                        } else {
                            // Fallback to home
                            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                        }
                    }
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}