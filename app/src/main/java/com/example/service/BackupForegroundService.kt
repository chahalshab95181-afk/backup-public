package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.repository.BackupRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BackupForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var repository: BackupRepository

    override fun onCreate() {
        super.onCreate()
        repository = BackupRepository(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val jobId = intent?.getLongExtra(EXTRA_JOB_ID, -1L) ?: -1L
        val action = intent?.action

        if (action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val initialNotification = buildNotification("Preparing TeleVault backup...", 0, 0)
        startForeground(NOTIFICATION_ID, initialNotification)

        // Observe repository progress and update notification
        serviceScope.launch {
            repository.currentProgress.collectLatest { progress ->
                if (progress.isRunning) {
                    val percent = (progress.chunkProgressPercent * 100).toInt()
                    val text = if (progress.totalFiles > 0) {
                        "File ${progress.fileIndex}/${progress.totalFiles}: ${progress.currentFileName} (${percent}%)"
                    } else {
                        "Uploading: ${progress.currentFileName}"
                    }
                    val notification = buildNotification(text, percent, 100)
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(NOTIFICATION_ID, notification)
                }
            }
        }

        serviceScope.launch(Dispatchers.IO) {
            try {
                if (jobId > 0) {
                    repository.runBackupJob(jobId)
                } else {
                    repository.runAllBackups()
                }
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun buildNotification(contentText: String, progress: Int, max: Int): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TeleVault Backup Active")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .setProgress(max, progress, max == 0)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "TeleVault Backup Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live upload and backup progress to Telegram channels"
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "televault_backup_channel"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_JOB_ID = "extra_job_id"
        const val ACTION_STOP = "action_stop_backup"

        fun startBackup(context: Context, jobId: Long) {
            val intent = Intent(context, BackupForegroundService::class.java).apply {
                putExtra(EXTRA_JOB_ID, jobId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
