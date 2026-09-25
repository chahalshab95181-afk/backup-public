package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "backup_jobs")
data class BackupJob(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val folderUri: String,
    val folderDisplayName: String,
    val channelId: Long,
    val isEncrypted: Boolean = true,
    val encryptionPassphrase: String = "",
    val isScheduleEnabled: Boolean = true,
    val scheduleIntervalMinutes: Long = 1440, // 24 hours default
    val requireWifi: Boolean = true,
    val requireCharging: Boolean = false,
    val chunkSizeMb: Int = 45, // default 45MB chunk size for reliable Telegram Bot API upload
    val lastBackupTimestamp: Long = 0L,
    val lastStatus: String = "IDLE", // IDLE, RUNNING, SUCCESS, FAILED
    val lastMessage: String = "Not run yet",
    val totalFilesCount: Int = 0,
    val totalBytesCount: Long = 0L
)
