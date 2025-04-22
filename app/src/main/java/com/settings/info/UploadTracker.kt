package com.settings.info

import android.content.Context
import android.util.Log

/**
 * Helper class to track and manage screenshot uploads
 */
class UploadTracker private constructor(context: Context) {
    private val TAG = "UploadTracker"
    private val prefs = context.getSharedPreferences("upload_tracker", Context.MODE_PRIVATE)
    
    companion object {
        private var instance: UploadTracker? = null
        
        fun getInstance(context: Context): UploadTracker {
            if (instance == null) {
                instance = UploadTracker(context.applicationContext)
            }
            return instance!!
        }
        
        // Keys for shared preferences
        private const val KEY_TOTAL_UPLOADS = "total_uploads"
        private const val KEY_SUCCESSFUL_UPLOADS = "successful_uploads"
        private const val KEY_FAILED_UPLOADS = "failed_uploads"
        private const val KEY_LAST_UPLOAD_TIMESTAMP = "last_upload_timestamp"
    }
    
    /**
     * Record a successful upload
     */
    fun recordSuccessfulUpload(url: String, filename: String) {
        val totalUploads = prefs.getInt(KEY_TOTAL_UPLOADS, 0) + 1
        val successfulUploads = prefs.getInt(KEY_SUCCESSFUL_UPLOADS, 0) + 1
        val timestamp = System.currentTimeMillis()
        
        prefs.edit()
            .putInt(KEY_TOTAL_UPLOADS, totalUploads)
            .putInt(KEY_SUCCESSFUL_UPLOADS, successfulUploads)
            .putLong(KEY_LAST_UPLOAD_TIMESTAMP, timestamp)
            .putString("last_upload_url", url)
            .putString("last_upload_filename", filename)
            .apply()
        
        Log.d(TAG, "Successful upload recorded: $filename, Total: $totalUploads, Success: $successfulUploads")
    }
    
    /**
     * Record a failed upload
     */
    fun recordFailedUpload(reason: String) {
        val totalUploads = prefs.getInt(KEY_TOTAL_UPLOADS, 0) + 1
        val failedUploads = prefs.getInt(KEY_FAILED_UPLOADS, 0) + 1
        
        prefs.edit()
            .putInt(KEY_TOTAL_UPLOADS, totalUploads)
            .putInt(KEY_FAILED_UPLOADS, failedUploads)
            .putString("last_failure_reason", reason)
            .apply()
        
        Log.e(TAG, "Failed upload recorded: $reason, Total: $totalUploads, Failures: $failedUploads")
    }
    
    /**
     * Get upload statistics
     */
    fun getUploadStats(): Map<String, Any> {
        return mapOf(
            "totalUploads" to prefs.getInt(KEY_TOTAL_UPLOADS, 0),
            "successfulUploads" to prefs.getInt(KEY_SUCCESSFUL_UPLOADS, 0),
            "failedUploads" to prefs.getInt(KEY_FAILED_UPLOADS, 0),
            "lastUploadTimestamp" to prefs.getLong(KEY_LAST_UPLOAD_TIMESTAMP, 0),
            "lastUploadUrl" to (prefs.getString("last_upload_url", "") ?: ""),
            "lastUploadFilename" to (prefs.getString("last_upload_filename", "") ?: ""),
            "lastFailureReason" to (prefs.getString("last_failure_reason", "") ?: "")
        )
    }
    
    /**
     * Reset all tracking statistics
     */
    fun resetStats() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Upload statistics reset")
    }
} 