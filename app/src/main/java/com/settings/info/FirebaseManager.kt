package com.settings.info

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File

/**
 * Utility class to manage local storage operations
 */
class StorageManager {
    companion object {
        private const val TAG = "StorageManager"
        private var isInitialized = false
        
        /**
         * Initialize storage - creates necessary folders
         */
        fun initialize(context: Context): Boolean {
            if (isInitialized) {
                Log.d(TAG, "Storage already initialized")
                return true
            }
            
            try {
                // Create screenshots directory if it doesn't exist
                val screenshotsDir = getScreenshotsDirectory(context)
                if (!screenshotsDir.exists()) {
                    val created = screenshotsDir.mkdirs()
                    Log.d(TAG, "Screenshots directory created: $created (${screenshotsDir.absolutePath})")
                } else {
                    Log.d(TAG, "Screenshots directory already exists: ${screenshotsDir.absolutePath}")
                }
                
                isInitialized = true
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing storage", e)
                return false
            }
        }
        
        /**
         * Get the directory where screenshots will be stored
         */
        fun getScreenshotsDirectory(context: Context): File {
            return File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Screenshots")
        }
        
        /**
         * Debug method to print storage status
         */
        fun logStorageStatus(context: Context) {
            try {
                val dir = getScreenshotsDirectory(context)
                Log.d(TAG, "Screenshots directory: ${dir.absolutePath}")
                Log.d(TAG, "Directory exists: ${dir.exists()}")
                Log.d(TAG, "Directory can write: ${dir.canWrite()}")
                
                val files = dir.listFiles()
                Log.d(TAG, "Files count: ${files?.size ?: 0}")
                files?.forEach { file ->
                    Log.d(TAG, "File: ${file.name} (${file.length()} bytes)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting storage status", e)
            }
        }
    }
} 