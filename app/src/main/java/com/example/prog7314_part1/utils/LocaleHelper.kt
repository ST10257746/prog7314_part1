package com.example.prog7314_part1.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import com.example.prog7314_part1.R
import java.util.Locale

/**
 * LocaleHelper
 * Utility class for managing app locale and language switching
 */
object LocaleHelper {
    
    private const val PREFS_NAME = "app_prefs"
    private const val PREFS_LANGUAGE = "selected_language"
    
    /**
     * Supported languages
     */
    enum class Language(val code: String, val displayName: String) {
        ENGLISH("en", "English"),
        ZULU("zu", "isiZulu"),
        AFRIKAANS("af", "Afrikaans");
        
        companion object {
            fun fromCode(code: String): Language {
                return values().find { it.code == code } ?: ENGLISH
            }
        }
    }
    
    /**
     * Get saved language from SharedPreferences
     */
    fun getSavedLanguage(context: Context): Language {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val languageCode = prefs.getString(PREFS_LANGUAGE, Language.ENGLISH.code)
        return Language.fromCode(languageCode ?: Language.ENGLISH.code)
    }
    
    /**
     * Save selected language to SharedPreferences
     */
    fun saveLanguage(context: Context, language: Language) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(PREFS_LANGUAGE, language.code).apply()
    }
    
    /**
     * Set app locale based on saved language preference
     */
    fun setLocale(context: Context, language: Language): Context {
        saveLanguage(context, language)
        return updateResources(context, language)
    }
    
    /**
     * Update resources with new locale
     */
    private fun updateResources(context: Context, language: Language): Context {
        val locale = Locale(language.code)
        Locale.setDefault(locale)
        
        val resources: Resources = context.resources
        val configuration: Configuration = resources.configuration
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale)
            return context.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = locale
            @Suppress("DEPRECATION")
            resources.updateConfiguration(configuration, resources.displayMetrics)
            return context
        }
    }
    
    /**
     * Apply saved locale on app start
     */
    fun onAttach(context: Context): Context {
        val language = getSavedLanguage(context)
        return setLocale(context, language)
    }
    
    /**
     * Get display name for language
     * Uses the current app locale to display language names
     */
    fun getLanguageDisplayName(context: Context, language: Language): String {
        // Create a context with the app's current locale to get translated strings
        val localeContext = updateResources(context, getSavedLanguage(context))
        return when (language) {
            Language.ENGLISH -> localeContext.getString(R.string.english)
            Language.ZULU -> localeContext.getString(R.string.zulu)
            Language.AFRIKAANS -> localeContext.getString(R.string.afrikaans)
        }
    }
    
    /**
     * Get context with specific locale for getting localized strings
     */
    fun getLocalizedContext(context: Context, language: Language): Context {
        return updateResources(context, language)
    }
}

