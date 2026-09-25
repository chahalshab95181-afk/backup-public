package com.example.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.data.local.TeleVaultDatabase
import com.example.data.model.BackupFileRecord
import com.example.data.model.BackupJob
import com.example.data.model.BackupLog
import com.example.data.model.TelegramChannel
import com.example.telegram.TelegramApiClient
import com.example.telegram.TelegramChunkUploader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class BackupProgressState(
    val isRunning: Boolean = false,
    val currentJobTitle: String = "",
    val currentFileName: String = "",
    val fileIndex: Int = 0,
    val totalFiles: Int = 0,
    val chunkIndex: Int = 0,
    val totalChunks: Int = 0,
    val chunkProgressPercent: Float = 0f,
    val overallBytesUploaded: Long = 0L,
    val overallTotalBytes: Long = 0L,
    val speedKbps: Long = 0L
)

class BackupRepository(
    private val context: Context,
    private val database: TeleVaultDatabase = TeleVaultDatabase.getInstance(context)
) {
    private val channelDao = database.telegramChannelDao()
    private val jobDao = database.backupJobDao()
    private val fileRecordDao = database.backupFileRecordDao()
    private val logDao = database.backupLogDao()

    private val apiClient = TelegramApiClient()
    private val chunkUploader = TelegramChunkUploader(context, apiClient)

    val allChannels: Flow<List<TelegramChannel>> = channelDao.getAllChannels()
    val allJobs: Flow<List<BackupJob>> = jobDao.getAllJobs()
    val recentFiles: Flow<List<BackupFileRecord>> = fileRecordDao.getRecentFiles()
    val allLogs: Flow<List<BackupLog>> = logDao.getAllLogs()
    val totalFilesCount: Flow<Int> = fileRecordDao.getTotalFilesCount()
    val totalBytesCount: Flow<Long> = fileRecordDao.getTotalBytesCount()

    private val _currentProgress = MutableStateFlow(BackupProgressState())
    val currentProgress = _currentProgress.asStateFlow()

    // --- Channel Operations ---
    suspend fun saveChannel(channel: TelegramChannel): Long = withContext(Dispatchers.IO) {
        val id = channelDao.insertChannel(channel)
        log(null, "INFO", "Configured channel '${channel.name}' (${channel.chatId})")
        id
    }

    suspend fun deleteChannel(channel: TelegramChannel) = withContext(Dispatchers.IO) {
        channelDao.deleteChannel(channel)
        log(null, "WARN", "Removed channel '${channel.name}'")
    }

    suspend fun testChannelConnection(botToken: String, chatId: String): Result<String> {
        val botRes = apiClient.getMe(botToken)
        if (botRes.isFailure) {
            return Result.failure(botRes.exceptionOrNull() ?: Exception("Bot verification failed"))
        }
        val bot = botRes.getOrThrow()

        val chatRes = apiClient.getChat(botToken, chatId)
        if (chatRes.isFailure) {
            return Result.failure(chatRes.exceptionOrNull() ?: Exception("Channel verification failed"))
        }
        val chat = chatRes.getOrThrow()

        return Result.success("Connected to @${bot.username ?: bot.firstName} -> Channel '${chat.title}'")
    }

    // --- Job Operations ---
    suspend fun saveJob(job: BackupJob): Long = withContext(Dispatchers.IO) {
        val id = jobDao.insertJob(job)
        log(id, "INFO", "Saved backup job '${job.title}' for folder ${job.folderDisplayName}")
        id
    }

    suspend fun deleteJob(job: BackupJob) = withContext(Dispatchers.IO) {
        jobDao.deleteJob(job)
        fileRecordDao.deleteFilesForJob(job.id)
        log(null, "WARN", "Deleted backup job '${job.title}' and associated records")
    }

    suspend fun getJobById(id: Long): BackupJob? = withContext(Dispatchers.IO) {
        jobDao.getJobById(id)
    }

    suspend fun log(jobId: Long?, level: String, message: String) = withContext(Dispatchers.IO) {
        logDao.insertLog(
            BackupLog(
                jobId = jobId,
                level = level,
                message = message
            )
        )
    }

    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        logDao.clearAllLogs()
    }

    // --- Backup Execution Engine ---
    suspend fun runBackupJob(jobId: Long): Result<Int> = withContext(Dispatchers.IO) {
        val job = jobDao.getJobById(jobId)
            ?: return@withContext Result.failure(IllegalArgumentException("Job with ID $jobId not found"))

        val channel = channelDao.getChannelById(job.channelId)
            ?: return@withContext Result.failure(IllegalStateException("Target Telegram channel not configured"))

        log(job.id, "INFO", "Starting backup job '${job.title}'...")
        jobDao.updateJobStatus(job.id, "RUNNING", "Scanning folder...", System.currentTimeMillis())

        _currentProgress.value = BackupProgressState(
            isRunning = true,
            currentJobTitle = job.title
        )

        try {
            val rootTreeUri = Uri.parse(job.folderUri)
            val rootDoc = DocumentFile.fromTreeUri(context, rootTreeUri)
                ?: throw IllegalStateException("Cannot access folder: ${job.folderDisplayName}")

            val fileItems = mutableListOf<ScannedFileInfo>()
            scanFolderRecursively(rootDoc, "", fileItems)

            if (fileItems.isEmpty()) {
                val msg = "No files found in folder '${job.folderDisplayName}'"
                log(job.id, "WARN", msg)
                jobDao.updateJobStatus(job.id, "SUCCESS", msg, System.currentTimeMillis())
                _currentProgress.value = BackupProgressState(isRunning = false)
                return@withContext Result.success(0)
            }

            log(job.id, "INFO", "Scanned ${fileItems.size} files in '${job.folderDisplayName}'. Checking for changes...")

            var backedUpCount = 0
            var skippedCount = 0
            var totalBytesProcessed = 0L

            val totalFiles = fileItems.size

            for ((index, item) in fileItems.withIndex()) {
                // Check if file is already backed up with same size and modified time
                val existingRecord = fileRecordDao.getFileRecord(job.id, item.relativePath)
                if (existingRecord != null &&
                    existingRecord.fileSize == item.size &&
                    existingRecord.fileLastModified == item.lastModified &&
                    existingRecord.status == "SUCCESS"
                ) {
                    skippedCount++
                    continue
                }

                _currentProgress.value = _currentProgress.value.copy(
                    currentFileName = item.name,
                    fileIndex = index + 1,
                    totalFiles = totalFiles,
                    chunkIndex = 1,
                    totalChunks = 1,
                    chunkProgressPercent = 0f
                )

                log(job.id, "INFO", "Backing up (${index + 1}/$totalFiles): ${item.relativePath} (${formatBytes(item.size)})")

                val uploadResult = chunkUploader.uploadFile(
                    fileUri = item.uri,
                    originalFileName = item.name,
                    relativeFilePath = item.relativePath,
                    fileSize = item.size,
                    botToken = channel.botToken,
                    chatId = channel.chatId,
                    topicId = channel.topicId,
                    isEncrypted = job.isEncrypted,
                    passphrase = job.encryptionPassphrase.ifBlank { "TeleVaultDefaultKey2026" },
                    chunkSizeMb = job.chunkSizeMb
                ) { progress ->
                    val chunkPercent = if (progress.currentChunkTotalBytes > 0) {
                        (progress.currentChunkBytes.toFloat() / progress.currentChunkTotalBytes.toFloat())
                    } else 0f

                    _currentProgress.value = _currentProgress.value.copy(
                        currentFileName = progress.fileName,
                        fileIndex = index + 1,
                        totalFiles = totalFiles,
                        chunkIndex = progress.currentChunkIndex,
                        totalChunks = progress.totalChunks,
                        chunkProgressPercent = chunkPercent,
                        overallBytesUploaded = progress.overallBytesUploaded,
                        overallTotalBytes = progress.overallTotalBytes
                    )
                }

                if (uploadResult.isSuccess) {
                    val res = uploadResult.getOrThrow()
                    fileRecordDao.insertOrUpdate(
                        BackupFileRecord(
                            jobId = job.id,
                            relativeFilePath = item.relativePath,
                            fileUri = item.uri.toString(),
                            fileSize = item.size,
                            fileLastModified = item.lastModified,
                            sha256Checksum = res.originalSha256,
                            chunkCount = res.totalChunks,
                            isEncrypted = res.isEncrypted,
                            telegramMessageIds = res.messageIds.joinToString(","),
                            backupTimestamp = System.currentTimeMillis(),
                            status = "SUCCESS"
                        )
                    )
                    backedUpCount++
                    totalBytesProcessed += item.size
                    log(job.id, "SUCCESS", "Uploaded '${item.relativePath}' (SHA: ${res.originalSha256.take(8)}...)")
                } else {
                    val err = uploadResult.exceptionOrNull()?.message ?: "Upload error"
                    log(job.id, "ERROR", "Failed to upload '${item.relativePath}': $err")
                    fileRecordDao.insertOrUpdate(
                        BackupFileRecord(
                            jobId = job.id,
                            relativeFilePath = item.relativePath,
                            fileUri = item.uri.toString(),
                            fileSize = item.size,
                            fileLastModified = item.lastModified,
                            sha256Checksum = "",
                            chunkCount = 1,
                            isEncrypted = job.isEncrypted,
                            telegramMessageIds = "",
                            backupTimestamp = System.currentTimeMillis(),
                            status = "FAILED"
                        )
                    )
                }
            }

            // Update stats
            val currentJobTotalFiles = fileRecordDao.getFileCountForJob(job.id)
            val currentJobTotalBytes = fileRecordDao.getTotalBytesForJob(job.id)
            jobDao.updateJobStats(job.id, currentJobTotalFiles, currentJobTotalBytes)

            val summary = "Completed: $backedUpCount uploaded, $skippedCount unchanged."
            log(job.id, "SUCCESS", summary)
            jobDao.updateJobStatus(job.id, "SUCCESS", summary, System.currentTimeMillis())

            _currentProgress.value = BackupProgressState(isRunning = false)
            Result.success(backedUpCount)

        } catch (e: CancellationException) {
            log(job.id, "WARN", "Backup was cancelled by user.")
            jobDao.updateJobStatus(job.id, "IDLE", "Cancelled", System.currentTimeMillis())
            _currentProgress.value = BackupProgressState(isRunning = false)
            throw e
        } catch (e: Exception) {
            val errMsg = e.message ?: "Unknown backup failure"
            log(job.id, "ERROR", "Backup error: $errMsg")
            jobDao.updateJobStatus(job.id, "FAILED", errMsg, System.currentTimeMillis())
            _currentProgress.value = BackupProgressState(isRunning = false)
            Result.failure(e)
        }
    }

    suspend fun runAllBackups(): Int = withContext(Dispatchers.IO) {
        val jobs = jobDao.getAllJobsSync()
        var totalUploaded = 0
        for (job in jobs) {
            val res = runBackupJob(job.id)
            if (res.isSuccess) {
                totalUploaded += res.getOrDefault(0)
            }
        }
        totalUploaded
    }

    private data class ScannedFileInfo(
        val name: String,
        val relativePath: String,
        val uri: Uri,
        val size: Long,
        val lastModified: Long
    )

    private fun scanFolderRecursively(
        dir: DocumentFile,
        currentPath: String,
        outList: MutableList<ScannedFileInfo>
    ) {
        val children = dir.listFiles()
        for (child in children) {
            val childPath = if (currentPath.isEmpty()) child.name.orEmpty() else "$currentPath/${child.name.orEmpty()}"
            if (child.isDirectory) {
                scanFolderRecursively(child, childPath, outList)
            } else if (child.isFile) {
                outList.add(
                    ScannedFileInfo(
                        name = child.name ?: "unnamed_file",
                        relativePath = childPath,
                        uri = child.uri,
                        size = child.length(),
                        lastModified = child.lastModified()
                    )
                )
            }
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        return "%.2f %s".format(bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
