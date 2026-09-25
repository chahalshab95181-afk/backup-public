package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "backup_file_records",
    indices = [
        Index(value = ["jobId", "relativeFilePath"], unique = true)
    ]
)
data class BackupFileRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val jobId: Long,
    val relativeFilePath: String,
    val fileUri: String,
    val fileSize: Long,
    val fileLastModified: Long,
    val sha256Checksum: String,
    val chunkCount: Int = 1,
    val isEncrypted: Boolean = true,
    val telegramMessageIds: String = "",
    val backupTimestamp: Long = System.currentTimeMillis(),
    val status: String = "SUCCESS" // SUCCESS, FAILED
)
