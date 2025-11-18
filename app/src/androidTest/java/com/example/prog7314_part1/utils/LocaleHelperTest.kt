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
        assertEquals(LocaleHelper.Language.ENGLISH, language)
    }

    @Test
    fun testSetLocale_savesLanguage() {
        LocaleHelper.setLocale(context, LocaleHelper.Language.AFRIKAANS)
        val savedLanguage = LocaleHelper.getSavedLanguage(context)
        assertEquals(LocaleHelper.Language.AFRIKAANS, savedLanguage)
    }

    @Test
    fun testSetLocale_savesZulu() {
        LocaleHelper.setLocale(context, LocaleHelper.Language.ZULU)
        val savedLanguage = LocaleHelper.getSavedLanguage(context)
        assertEquals(LocaleHelper.Language.ZULU, savedLanguage)
    }

    @Test
    fun testOnAttach_returnsContextWithLocale() {
        LocaleHelper.setLocale(context, LocaleHelper.Language.AFRIKAANS)
        val attachedContext = LocaleHelper.onAttach(context)
        assertNotNull(attachedContext)
        // Context should have locale applied
        assertEquals("af", attachedContext.resources.configuration.locales[0].language)
    }

    @Test
    fun testLanguagePersistence() {
        // Set language
        LocaleHelper.setLocale(context, LocaleHelper.Language.ZULU)
        
        // Create new context (simulating app restart)
        val newContext: Context = ApplicationProvider.getApplicationContext()
        val persistedLanguage = LocaleHelper.getSavedLanguage(newContext)
        
        assertEquals(LocaleHelper.Language.ZULU, persistedLanguage)
    }
}

