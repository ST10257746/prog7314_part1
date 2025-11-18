package com.example.prog7314_part1.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.prog7314_part1.sync.SyncWorker
import java.util.concurrent.TimeUnit

/**
 * NetworkMonitor
 * 
 * Utility class that monitors network connectivity and triggers sync operations
 * when network becomes available. Uses Android's ConnectivityManager to detect
 * network state changes and WorkManager to schedule sync workers.
 * 
 * References:
 * - ConnectivityManager: https://developer.android.com/reference/android/net/ConnectivityManager
 * - WorkManager: https://developer.android.com/topic/libraries/architecture/workmanager
 * 
 * @author FitTrackr Development Team
 */
object NetworkMonitor {

    private const val TAG = "NetworkMonitor"
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    /**
     * Start monitoring network connectivity
     * Registers a callback to detect when network becomes available
     */
    fun startMonitoring(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Cancel existing callback if any
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to unregister existing callback: ${e.message}")
            }
        }

        // Create network request to monitor all network types
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        // Create callback to handle network changes
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "🌐 Network available - triggering sync")
                triggerSync(context)
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "📴 Network lost")
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                
                if (hasInternet && isValidated) {
                    Log.d(TAG, "🌐 Network validated - triggering sync")
                    triggerSync(context)
                }
            }
        }

        // Register the callback
        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
            Log.d(TAG, "✅ Network monitoring started")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to register network callback: ${e.message}", e)
        }
    }

    /**
     * Stop monitoring network connectivity
     */
    fun stopMonitoring(context: Context) {
        networkCallback?.let {
            try {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                connectivityManager.unregisterNetworkCallback(it)
                networkCallback = null
                Log.d(TAG, "⏸️ Network monitoring stopped")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to unregister network callback: ${e.message}", e)
            }
        }
    }

    /**
     * Trigger sync operation immediately
     * Enqueues a one-time sync worker
     */
    fun triggerSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = androidx.work.OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(syncRequest)
        Log.d(TAG, "📤 Sync worker enqueued")
    }

    /**
     * Schedule periodic sync
     * Runs sync every 15 minutes when network is available
     */
    fun schedulePeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicSyncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(SyncWorker.WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncRequest
        )
        Log.d(TAG, "⏰ Periodic sync scheduled (every 15 minutes)")
    }
}

