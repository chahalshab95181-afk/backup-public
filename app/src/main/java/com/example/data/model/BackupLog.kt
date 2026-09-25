package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "backup_logs")
data class BackupLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val jobId: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val level: String = "INFO", // INFO, WARN, ERROR, SUCCESS
    val message: String
)
