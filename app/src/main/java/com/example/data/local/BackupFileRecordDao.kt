package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BackupFileRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface BackupFileRecordDao {
    @Query("SELECT * FROM backup_file_records WHERE jobId = :jobId ORDER BY backupTimestamp DESC")
    fun getFilesForJob(jobId: Long): Flow<List<BackupFileRecord>>

    @Query("SELECT * FROM backup_file_records ORDER BY backupTimestamp DESC LIMIT 100")
    fun getRecentFiles(): Flow<List<BackupFileRecord>>

    @Query("SELECT * FROM backup_file_records WHERE jobId = :jobId AND relativeFilePath = :relativePath LIMIT 1")
    suspend fun getFileRecord(jobId: Long, relativePath: String): BackupFileRecord?

    @Query("SELECT COUNT(*) FROM backup_file_records WHERE jobId = :jobId")
    suspend fun getFileCountForJob(jobId: Long): Int

    @Query("SELECT COALESCE(SUM(fileSize), 0) FROM backup_file_records WHERE jobId = :jobId")
    suspend fun getTotalBytesForJob(jobId: Long): Long

    @Query("SELECT COUNT(*) FROM backup_file_records")
    fun getTotalFilesCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(fileSize), 0) FROM backup_file_records")
    fun getTotalBytesCount(): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: BackupFileRecord): Long

    @Query("DELETE FROM backup_file_records WHERE jobId = :jobId")
    suspend fun deleteFilesForJob(jobId: Long)
}
