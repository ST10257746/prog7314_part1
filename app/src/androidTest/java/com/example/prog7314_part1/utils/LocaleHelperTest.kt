package com.example.prog7314_part1.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for LocaleHelper
 * Tests language saving, retrieval, and locale attachment
 */
@RunWith(AndroidJUnit4::class)
class LocaleHelperTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        // Clear preferences before each test
        prefs.edit().clear().apply()
    }

    @Test
    fun testGetSavedLanguage_defaultIsEnglish() {
        val language = LocaleHelper.getSavedLanguage(context)
        assertEquals("en", language)
    }

    @Test
    fun testSetLocale_savesLanguage() {
        LocaleHelper.setLocale(context, "af")
        val savedLanguage = LocaleHelper.getSavedLanguage(context)
        assertEquals("af", savedLanguage)
    }

    @Test
    fun testSetLocale_savesZulu() {
        LocaleHelper.setLocale(context, "zu")
        val savedLanguage = LocaleHelper.getSavedLanguage(context)
        assertEquals("zu", savedLanguage)
    }

    @Test
    fun testOnAttach_returnsContextWithLocale() {
        LocaleHelper.setLocale(context, "af")
        val attachedContext = LocaleHelper.onAttach(context)
        assertNotNull(attachedContext)
        // Context should have locale applied
        assertEquals("af", attachedContext.resources.configuration.locales[0].language)
    }

    @Test
    fun testLanguagePersistence() {
        // Set language
        LocaleHelper.setLocale(context, "zu")
        
        // Create new context (simulating app restart)
        val newContext = ApplicationProvider.getApplicationContext()
        val persistedLanguage = LocaleHelper.getSavedLanguage(newContext)
        
        assertEquals("zu", persistedLanguage)
    }
}

