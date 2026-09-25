package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BackupJob
import kotlinx.coroutines.flow.Flow

@Dao
interface BackupJobDao {
    @Query("SELECT * FROM backup_jobs ORDER BY id DESC")
    fun getAllJobs(): Flow<List<BackupJob>>

    @Query("SELECT * FROM backup_jobs WHERE id = :id LIMIT 1")
    suspend fun getJobById(id: Long): BackupJob?

    @Query("SELECT * FROM backup_jobs WHERE isScheduleEnabled = 1")
    suspend fun getScheduledJobsSync(): List<BackupJob>

    @Query("SELECT * FROM backup_jobs")
    suspend fun getAllJobsSync(): List<BackupJob>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: BackupJob): Long

    @Update
    suspend fun updateJob(job: BackupJob)

    @Query("UPDATE backup_jobs SET lastStatus = :status, lastMessage = :message, lastBackupTimestamp = :timestamp WHERE id = :id")
    suspend fun updateJobStatus(id: Long, status: String, message: String, timestamp: Long)

    @Query("UPDATE backup_jobs SET totalFilesCount = :filesCount, totalBytesCount = :bytesCount WHERE id = :id")
    suspend fun updateJobStats(id: Long, filesCount: Int, bytesCount: Long)

    @Delete
    suspend fun deleteJob(job: BackupJob)

    @Query("DELETE FROM backup_jobs WHERE id = :id")
    suspend fun deleteJobById(id: Long)
}
