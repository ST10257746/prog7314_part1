package com.example.prog7314_part1.ui.main

import android.content.Context
import android.content.Intent
import android.Manifest
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController

import com.example.prog7314_part1.R
import com.example.prog7314_part1.data.model.AuthState
import com.example.prog7314_part1.data.repository.ApiUserRepository
import com.example.prog7314_part1.utils.LocaleHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val EXTRA_IS_LOGOUT = "extra_is_logout"
        private const val KEY_HAS_BIOMETRIC_AUTH = "key_has_biometric_auth"
        private const val REQUEST_CODE_NOTIFICATIONS = 1001
        private const val BIOMETRIC_AUTHENTICATORS =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL

        private const val PROMPT_TITLE = "Unlock your account"
        private const val PROMPT_SUBTITLE = "Confirm it's really you"
        private const val PROMPT_DESCRIPTION = "Use your fingerprint or face to continue"
        private const val PROMPT_ERROR = "Biometric authentication required to continue"
    }

    private var isRecreatingForLogout = false

    private lateinit var userRepository: ApiUserRepository
    private var hasAuthenticatedThisSession = false
    private var biometricPrompt: BiometricPrompt? = null
    private var biometricPromptInfo: BiometricPrompt.PromptInfo? = null

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

        hasAuthenticatedThisSession =
            savedState?.getBoolean(KEY_HAS_BIOMETRIC_AUTH, false) ?: false

        // Set up navigation first (non-blocking, must be done immediately)
        setupNavigation()

        // Defer everything else to allow onCreate() to finish quickly
        // This prevents ANR by letting the activity complete startup
        lifecycleScope.launch {
            // Wait for Firebase to finish initializing (can be slow on emulator)
            delay(1000)
            
            // Setup biometric prompt (non-blocking)
            setupBiometricPrompt()

            // Request notification permission if needed (non-blocking)
            requestNotificationPermissionIfNeeded()
            
            // Initialize repository on background thread
            withContext(Dispatchers.IO) {
                userRepository = ApiUserRepository(this@MainActivity)
            }
            
            // Once repository is ready, observe auth state on main thread
            observeAuthState()
        }
    }

    override fun onResume() {
        super.onResume()
        enforceBiometricIfNeeded()
        
        // Get FCM token after app is fully loaded (non-blocking)
        lifecycleScope.launch {
            // Delay to ensure Firebase is fully initialized
            delay(2000)
            getFcmToken()
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            hasAuthenticatedThisSession = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_HAS_BIOMETRIC_AUTH, hasAuthenticatedThisSession)
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

    private fun setupBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    hasAuthenticatedThisSession = true
                }

                override fun onAuthenticationFailed() {
                    Toast.makeText(this@MainActivity, PROMPT_ERROR, Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    hasAuthenticatedThisSession = false
                    Toast.makeText(this@MainActivity, errString, Toast.LENGTH_SHORT).show()
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_LOCKOUT ||
                        errorCode == BiometricPrompt.ERROR_LOCKOUT_PERMANENT
                    ) {
                        handleBiometricRejection()
                    }
                }
            }
        )

        biometricPromptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(PROMPT_TITLE)
            .setSubtitle(PROMPT_SUBTITLE)
            .setDescription(PROMPT_DESCRIPTION)
            .setAllowedAuthenticators(BIOMETRIC_AUTHENTICATORS)
            .build()
    }

    private fun enforceBiometricIfNeeded() {
        // Skip if repository not initialized yet
        if (!::userRepository.isInitialized) {
            return
        }
        
        if (!userRepository.isLoggedIn()) {
            hasAuthenticatedThisSession = false
            return
        }
        if (hasAuthenticatedThisSession) {
            return
        }

        val prompt = biometricPrompt ?: return
        val promptInfo = biometricPromptInfo ?: return
        val biometricManager = BiometricManager.from(this)
        val capability = biometricManager.canAuthenticate(BIOMETRIC_AUTHENTICATORS)

        when (capability) {
            BiometricManager.BIOMETRIC_SUCCESS -> prompt.authenticate(promptInfo)
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                kotlin.runCatching {
                    val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                        putExtra(
                            Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                            BIOMETRIC_AUTHENTICATORS
                        )
                    }
                    startActivity(enrollIntent)
                }.onFailure {
                    Log.e(TAG, "Unable to launch biometric enrollment settings", it)
                }
                handleBiometricRejection()
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Log.w(TAG, "Biometric hardware unavailable, skipping biometric gate.")
                hasAuthenticatedThisSession = true
            }
            else -> {
                Log.w(TAG, "Biometric authentication failed with code=$capability")
                handleBiometricRejection()
            }
        }
    }

    private fun handleBiometricRejection() {
        Toast.makeText(this, PROMPT_ERROR, Toast.LENGTH_SHORT).show()
        moveTaskToBack(true)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_CODE_NOTIFICATIONS
            )
        }
    }

    /**
     * Get FCM token for testing push notifications
     * Token will be logged to logcat - use: adb logcat | grep FCM_TOKEN
     * Called asynchronously to avoid blocking startup
     */
    private suspend fun getFcmToken() {
        withContext(Dispatchers.IO) {
            try {
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                        return@addOnCompleteListener
                    }

                    // Get new FCM registration token
                    val token = task.result
                    Log.d(TAG, "FCM_TOKEN: $token")
                    Log.d(TAG, "📱 Copy this token to send test notifications via Firebase Console")
                    
                    // Show toast on main thread
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "FCM Token logged - check logcat",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error getting FCM token", e)
            }
        }
    }

    var database: FirebaseDatabase = FirebaseDatabase.getInstance()
}
