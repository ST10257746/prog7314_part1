package com.example.prog7314_part1.ui.settings

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.example.prog7314_part1.R
import com.example.prog7314_part1.data.repository.ApiUserRepository
import com.example.prog7314_part1.utils.LocaleHelper
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Settings Fragment
 * Displays user settings and preferences including language selection
 */
class SettingsFragment : Fragment(), LanguageSelectionDialog.LanguageSelectionListener {

    private lateinit var userRepository: ApiUserRepository
    private lateinit var usernameTextView: TextView
    private lateinit var editProfileButton: Button
    private lateinit var setGoalsButton: Button
    private lateinit var notificationsButton: Button
    private lateinit var languageButton: Button
    private lateinit var logoutButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userRepository = ApiUserRepository(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        
        // Initialize views
        usernameTextView = view.findViewById(R.id.username)
        editProfileButton = view.findViewById(R.id.editProfileButton)
        setGoalsButton = view.findViewById(R.id.setGoalsButton)
        notificationsButton = view.findViewById(R.id.notificationsButton)
        languageButton = view.findViewById(R.id.languageButton)
        logoutButton = view.findViewById(R.id.logoutButton)
        
        // Set button text from strings.xml
        editProfileButton.text = getString(R.string.edit_profile)
        setGoalsButton.text = getString(R.string.set_fitness_goals)
        notificationsButton.text = getString(R.string.notification_preferences)
        languageButton.text = getString(R.string.change_language)
        logoutButton.text = getString(R.string.log_out)
        
        // Set up click listeners
        setupClickListeners()
        
        // Load user data
        loadUserData()
        
        return view
    }

    private fun setupClickListeners() {
        editProfileButton.setOnClickListener {
            // Navigate to edit profile fragment
            findNavController().navigate(R.id.action_settingsFragment_to_editProfileFragment)
        }
        
        setGoalsButton.setOnClickListener {
            // Navigate to setup goals fragment
            findNavController().navigate(R.id.action_settingsFragment_to_setupGoalsFragment)
        }
        
        notificationsButton.setOnClickListener {
            // TODO: Implement notification preferences
            Snackbar.make(requireView(), "Notification preferences coming soon", Snackbar.LENGTH_SHORT).show()
        }
        
        languageButton.setOnClickListener {
            showLanguageSelectionDialog()
        }
        
        logoutButton.setOnClickListener {
            handleLogout()
        }
    }

    private fun showLanguageSelectionDialog() {
        val dialog = LanguageSelectionDialog()
        dialog.setLanguageSelectionListener(this)
        dialog.show(parentFragmentManager, "LanguageSelectionDialog")
    }

    override fun onLanguageSelected(language: LocaleHelper.Language) {
        // Language change is handled by dialog which recreates the activity
        Snackbar.make(requireView(), getString(R.string.language_changed), Snackbar.LENGTH_SHORT).show()
    }

    private fun loadUserData() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val currentUser = userRepository.getCurrentUserSuspend()
                currentUser?.let { user ->
                    usernameTextView.text = user.displayName ?: "User"
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsFragment", "Error loading user data: ${e.message}", e)
            }
        }
    }

    private fun handleLogout() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                when (val result = userRepository.logout()) {
                    is com.example.prog7314_part1.data.model.Result.Success -> {
                        // Navigation will be handled by MainActivity observing auth state
                        requireActivity().finish()
                    }
                    is com.example.prog7314_part1.data.model.Result.Error -> {
                        Snackbar.make(
                            requireView(),
                            "Logout failed: ${result.message}",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsFragment", "Error during logout: ${e.message}", e)
                Snackbar.make(requireView(), "Error during logout", Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}
