package gas.hostinggo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File

class ServerService : Service() {
    private var server: MyNanoHttpd? = null
    private val channelId = "ServerStatusChannel"
    private val notificationId = 1

    companion object {
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        private const val TAG = "ServerService"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun startServer() {
        val targetDir = File(filesDir, "memogo").apply {
            if (!exists()) mkdirs()
        }

        val webFiles = listOf(
            "login.html", "login.css", "login.js",
            "register.html", "register.css", "register.js",
            "beranda.html", "beranda.css", "beranda.js"
        )

        try {
            webFiles.forEach { filename ->
                assets.open(filename).use { input ->
                    File(targetDir, filename).outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            server?.let {
                Log.d(TAG, "Server already running")
                return
            }

            server = MyNanoHttpd(8080, targetDir).apply {
                start()
                Log.d(TAG, "Server started at: http://localhost:8080/")
            }
            
            updateNotification(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting server", e)
            stopSelf()
        }
    }

    private fun stopServer() {
        try {
            server?.stop()
            server = null
            Log.d(TAG, "Server stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping server", e)
        } finally {
            updateNotification(false)
        }
    }

    private fun updateNotification(isRunning: Boolean) {
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ikon)
            .setContentTitle(getString(R.string.server_status_title)) // Gunakan string resource
            .setContentText(if (isRunning) "Server ON" else "Server OFF")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(notificationId, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Server Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows server running status"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startServer()
            ACTION_STOP -> stopServer()
            else -> Log.w(TAG, "Unknown action: ${intent?.action}")
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopServer()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}