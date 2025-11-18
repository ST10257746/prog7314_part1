package com.example.prog7314_part1.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.prog7314_part1.R
import com.example.prog7314_part1.data.repository.NetworkRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        android.util.Log.d("FCM", "🔄 New FCM token received: $token")
        scope.launch {
            try {
                val repository = NetworkRepository(applicationContext)
                repository.registerFcmToken(token)
                android.util.Log.d("FCM", "✅ Token registered with backend")
            } catch (e: Exception) {
                android.util.Log.e("FCM", "❌ Failed to register token: ${e.message}")
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        android.util.Log.d("FCM", "📨 Message received!")
        android.util.Log.d("FCM", "From: ${remoteMessage.from}")
        android.util.Log.d("FCM", "Data: ${remoteMessage.data}")
        android.util.Log.d("FCM", "Notification title: ${remoteMessage.notification?.title}")
        android.util.Log.d("FCM", "Notification body: ${remoteMessage.notification?.body}")
        
        val title = remoteMessage.notification?.title ?: "FitTrackr"
        val body = remoteMessage.notification?.body ?: ""
        
        android.util.Log.d("FCM", "📤 Showing notification: $title - $body")
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        android.util.Log.d("FCM", "🔔 Attempting to show notification...")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            android.util.Log.d("FCM", "📋 Notification permission: $hasPermission")
            if (!hasPermission) {
                android.util.Log.w("FCM", "⚠️ Notification permission not granted!")
                return
            }
        }

        val channelId = "fittrackr_notifications"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "FitTrackr Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(this)) {
                notify(System.currentTimeMillis().toInt(), builder.build())
                android.util.Log.d("FCM", "✅ Notification displayed successfully!")
            }
        } catch (e: Exception) {
            android.util.Log.e("FCM", "❌ Failed to show notification: ${e.message}", e)
        }
    }
}
