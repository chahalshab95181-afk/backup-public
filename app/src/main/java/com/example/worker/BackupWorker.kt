package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.repository.BackupRepository

class BackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = BackupRepository(applicationContext)
        val jobId = inputData.getLong(KEY_JOB_ID, -1L)

        return try {
            if (jobId > 0) {
                val res = repository.runBackupJob(jobId)
                if (res.isSuccess) Result.success() else Result.retry()
            } else {
                repository.runAllBackups()
                Result.success()
            }
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_JOB_ID = "televault_job_id"
    }
}
