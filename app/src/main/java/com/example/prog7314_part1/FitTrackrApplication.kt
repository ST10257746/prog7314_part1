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
        // Ensure locale is set on app start
        // This sets the default locale for the entire application
        val savedLanguage = LocaleHelper.getSavedLanguage(this)
        LocaleHelper.setLocale(this, savedLanguage)
    }
    
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // Update locale when configuration changes
        val savedLanguage = LocaleHelper.getSavedLanguage(this)
        LocaleHelper.setLocale(this, savedLanguage)
    }
}

