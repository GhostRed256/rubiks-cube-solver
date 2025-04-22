package com.settings.info

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Application class for initializing app-wide components
 */
class ScreenshotApp : Application() {
    private val TAG = "ScreenshotApp"
    
    override fun onCreate() {
        super.onCreate()
        
        Log.d(TAG, "Application onCreate started")
        
        try {
            // Initialize storage
            val result = StorageManager.initialize(this)
            if (result) {
                Log.d(TAG, "Storage initialization successful")
            } else {
                Log.e(TAG, "Storage initialization failed")
            }
            
            // Log storage status after a short delay
            Handler(Looper.getMainLooper()).postDelayed({
                StorageManager.logStorageStatus(applicationContext)
            }, 3000)
            
            Log.d(TAG, "Application started and storage initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Error during application initialization", e)
        }
    }
} 