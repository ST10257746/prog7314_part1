package com.example.prog7314_part1

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.prog7314_part1.utils.LocaleHelper
import com.example.prog7314_part1.utils.NetworkMonitor

/**
 * FitTrackr Application
 * 
 * Custom Application class that:
 * - Ensures locale is applied globally across the entire app
 * - Initializes network monitoring and sync workers for offline data synchronization
 * 
 * References:
 * - Application class: https://developer.android.com/reference/android/app/Application
 * - WorkManager: https://developer.android.com/topic/libraries/architecture/workmanager
 * 
 * @author FitTrackr Development Team
 */
class FitTrackrApplication : Application() {
    
    companion object {
        private const val TAG = "FitTrackrApplication"
    }
    
    override fun attachBaseContext(base: Context) {
        // Apply saved locale to the base context
        // This ensures all contexts created from this application use the correct locale
        super.attachBaseContext(LocaleHelper.onAttach(base))
    }
    
    override fun onCreate() {
        super.onCreate()
        // Locale is already set in attachBaseContext, no need to set again here
        // Setting it here would cause configuration changes and potential ANR
        
        // Initialize network monitoring for offline sync
        initializeSync()
    }
    
    /**
     * Initialize offline sync functionality
     * Sets up network monitoring and periodic sync workers
     */
    private fun initializeSync() {
        try {
            // Start monitoring network connectivity
            NetworkMonitor.startMonitoring(this)
            
            // Schedule periodic sync (runs every 15 minutes when network is available)
            NetworkMonitor.schedulePeriodicSync(this)
            
            Log.d(TAG, "✅ Sync system initialized")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize sync system: ${e.message}", e)
        }
    }
    
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // Don't call setLocale here as it can cause configuration change loops
        // The locale is already set in attachBaseContext, and MainActivity handles runtime changes
    }
    
    override fun onTerminate() {
        super.onTerminate()
        // Stop network monitoring when app terminates
        NetworkMonitor.stopMonitoring(this)
    }
}

