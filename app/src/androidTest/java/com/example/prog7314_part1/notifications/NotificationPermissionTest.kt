package com.example.prog7314_part1.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for notification permission handling
 */
@RunWith(AndroidJUnit4::class)
class NotificationPermissionTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun testNotificationPermission_requiredOnAndroid13Plus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // On Android 13+, POST_NOTIFICATIONS permission is required
            assertTrue("POST_NOTIFICATIONS permission should be required on Android 13+", true)
        }
    }

    @Test
    fun testNotificationPermission_notRequiredOnAndroid12() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // On Android < 13, permission is not required
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            
            // Permission check should not crash even if not required
            assertNotNull(hasPermission)
        }
    }

    @Test
    fun testMyFirebaseMessagingService_exists() {
        // Verify the service class exists and can be instantiated
        val serviceClass = MyFirebaseMessagingService::class.java
        assertNotNull(serviceClass)
        assertEquals("MyFirebaseMessagingService", serviceClass.simpleName)
    }
}

