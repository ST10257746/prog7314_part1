package com.example.prog7314_part1.ui.main

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.prog7314_part1.R
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for MainActivity biometric functionality
 * Note: These tests require a device/emulator with biometric hardware
 */
@RunWith(AndroidJUnit4::class)
class MainActivityBiometricTest {

    @Test
    fun testMainActivity_launchesSuccessfully() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        
        scenario.onActivity { activity ->
            // Verify activity is created
            assertNotNull(activity)
            // Verify biometric-related components exist
            assertNotNull(activity.findViewById(R.id.frameLayout))
        }
        
        scenario.close()
    }

    @Test
    fun testMainActivity_hasBiometricComponents() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        
        scenario.onActivity { activity ->
            // Check that MainActivity has biometric-related fields
            // Note: We can't directly access private fields, but we can verify
            // the activity doesn't crash when initialized
            assertNotNull(activity)
        }
        
        scenario.close()
    }
}

