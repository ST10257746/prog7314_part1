package com.example.prog7314_part1

import android.app.Application
import android.content.Context
import com.example.prog7314_part1.utils.LocaleHelper

/**
 * FitTrackr Application
 * Custom Application class to ensure locale is applied globally across the entire app
 * This ensures all activities, fragments, and services use the correct locale
 */
class FitTrackrApplication : Application() {
    
    override fun attachBaseContext(base: Context) {
        // Apply saved locale to the base context
        // This ensures all contexts created from this application use the correct locale
        super.attachBaseContext(LocaleHelper.onAttach(base))
    }
    
    override fun onCreate() {
        super.onCreate()
        // Locale is already set in attachBaseContext, no need to set again here
        // Setting it here would cause configuration changes and potential ANR
    }
    
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // Don't call setLocale here as it can cause configuration change loops
        // The locale is already set in attachBaseContext, and MainActivity handles runtime changes
    }
}

