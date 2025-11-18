package com.example.prog7314_part1.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.navigation.NavOptions

import com.example.prog7314_part1.R
import com.example.prog7314_part1.data.model.AuthState
import com.example.prog7314_part1.data.repository.ApiUserRepository
import com.example.prog7314_part1.utils.LocaleHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_IS_LOGOUT = "extra_is_logout"
    }

    private var isRecreatingForLogout = false

    private lateinit var userRepository: ApiUserRepository

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { LocaleHelper.onAttach(it) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // If this is a logout restart, don't restore navigation state
        val isLogoutRestart = intent?.getBooleanExtra(EXTRA_IS_LOGOUT, false) ?: false
        val savedState = if (isLogoutRestart) null else savedInstanceState

        super.onCreate(savedState)

        // Reset the flag when activity is created (fresh start)
        isRecreatingForLogout = false
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize repository
        userRepository = ApiUserRepository(this)

        // Set up navigation
        setupNavigation()

        // Observe authentication state
        observeAuthState()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.frameLayout) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNavView = findViewById<BottomNavigationView>(R.id.navBarView)
        bottomNavView.setupWithNavController(navController)

        // Listen to navigation changes to show/hide bottom nav
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Hide bottom navigation on login/register/setup screens
            bottomNavView.isVisible = destination.id != R.id.loginFragment &&
                    destination.id != R.id.registerFragment &&
                    destination.id != R.id.setupGoalsFragment
        }
    }

    /**
     * Observe authentication state
     * Navigate to login if not authenticated
     */
    private fun observeAuthState() {
        lifecycleScope.launch {
            userRepository.observeAuthState().collect { authState ->
                val navHostFragment = supportFragmentManager
                    .findFragmentById(R.id.frameLayout) as NavHostFragment
                val navController = navHostFragment.navController

                // Navigate to login if not logged in and not already on auth screens
                if (authState is AuthState.Unauthenticated) {
                    val currentDestination = navController.currentDestination?.id
                    if (currentDestination != R.id.loginFragment &&
                        currentDestination != R.id.registerFragment) {

                        // Create intent with flag to indicate this is a logout restart
                        val intent = Intent(this@MainActivity, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            putExtra(EXTRA_IS_LOGOUT, true)
                        }

                        // Finish and restart the activity completely
                        startActivity(intent)
                        finish()
                    }
                }
            }
        }
    }

    var database: FirebaseDatabase = FirebaseDatabase.getInstance()
}