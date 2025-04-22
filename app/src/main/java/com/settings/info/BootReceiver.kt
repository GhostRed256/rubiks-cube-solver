package com.settings.info

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * This receiver starts the MainActivity to get screenshot permission when the device boots up
 */
class BootReceiver : BroadcastReceiver() {
    private val TAG = "BootReceiver"
    
    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
                Log.d(TAG, "Boot completed received, starting app after delay")
                
                // Initialize storage
                StorageManager.initialize(context.applicationContext)
                
                // Always start the MainActivity to get fresh projection permission
                // We need to delay this to ensure system is fully booted
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        // Start the MainActivity which will request projection permission
                        val mainIntent = Intent(context, MainActivity::class.java)
                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(mainIntent)
                        Log.d(TAG, "Started MainActivity to get screenshot permission after boot")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error starting MainActivity after boot", e)
                    }
                }, 10000) // 10 second delay for system to be ready
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in BootReceiver", e)
        }
    }
} 