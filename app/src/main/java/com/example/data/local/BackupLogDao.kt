package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.BackupLog
import kotlinx.coroutines.flow.Flow

@Dao
interface BackupLogDao {
    @Query("SELECT * FROM backup_logs ORDER BY timestamp DESC LIMIT 300")
    fun getAllLogs(): Flow<List<BackupLog>>

    @Query("SELECT * FROM backup_logs WHERE jobId = :jobId ORDER BY timestamp DESC LIMIT 100")
    fun getLogsForJob(jobId: Long): Flow<List<BackupLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: BackupLog): Long

    @Query("DELETE FROM backup_logs WHERE jobId = :jobId")
    suspend fun deleteLogsForJob(jobId: Long)

    @Query("DELETE FROM backup_logs")
    suspend fun clearAllLogs()
}
