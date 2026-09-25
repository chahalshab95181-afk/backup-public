package com.example.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.model.BackupJob
import java.util.concurrent.TimeUnit

object WorkSchedulerHelper {

    fun updateJobSchedule(context: Context, job: BackupJob) {
        val workManager = WorkManager.getInstance(context)
        val uniqueWorkName = "televault_job_${job.id}"

        if (!job.isScheduleEnabled) {
            workManager.cancelUniqueWork(uniqueWorkName)
            return
        }

        // WorkManager minimum periodic interval is 15 minutes
        val intervalMinutes = job.scheduleIntervalMinutes.coerceAtLeast(15)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (job.requireWifi) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresCharging(job.requireCharging)
            .build()

        val inputData = Data.Builder()
            .putLong(BackupWorker.KEY_JOB_ID, job.id)
            .build()

        val periodicWork = PeriodicWorkRequestBuilder<BackupWorker>(
            intervalMinutes, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()

        workManager.enqueueUniquePeriodicWork(
            uniqueWorkName,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWork
        )
    }

    fun cancelJobSchedule(context: Context, jobId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork("televault_job_$jobId")
    }

    fun triggerImmediateJob(context: Context, jobId: Long) {
        val inputData = Data.Builder()
            .putLong(BackupWorker.KEY_JOB_ID, jobId)
            .build()

        val oneTimeWork = OneTimeWorkRequestBuilder<BackupWorker>()
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "televault_immediate_$jobId",
            ExistingWorkPolicy.REPLACE,
            oneTimeWork
        )
    }
}
