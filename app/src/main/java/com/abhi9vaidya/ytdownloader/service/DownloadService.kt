package com.abhi9vaidya.ytdownloader.service

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.abhi9vaidya.ytdownloader.MainActivity
import com.abhi9vaidya.ytdownloader.worker.DownloadWorker

class DownloadService : Service() {

    private val CHANNEL_ID = "download_channel"
    private val NOTIFICATION_ID = 1

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra("url") ?: return START_NOT_STICKY
        val title = intent.getStringExtra("title") ?: "Video"

        // Start a minimal foreground notification while WorkManager handles the heavy lifting
        startForeground(NOTIFICATION_ID, createNotification(title, 0))

        // Enqueue the DownloadWorker with input data
        val input = workDataOf("url" to url, "title" to title)
        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(input)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(workRequest)

        // Stop the service; the worker will run in background with its own foreground state
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        return START_NOT_STICKY
    }

    private fun createNotification(title: String, progress: Int): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Preparing download: $title")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
