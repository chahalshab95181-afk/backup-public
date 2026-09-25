package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.CryptoEngine
import com.example.data.model.BackupFileRecord
import com.example.data.model.BackupJob
import com.example.data.model.BackupLog
import com.example.data.model.TelegramChannel
import com.example.data.repository.BackupProgressState
import com.example.data.repository.BackupRepository
import com.example.service.BackupForegroundService
import com.example.worker.WorkSchedulerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class DecryptResultState(
    val isSuccess: Boolean,
    val message: String,
    val sha256: String? = null,
    val outputFileName: String? = null
)

class TeleVaultViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BackupRepository(application)

    val allChannels: StateFlow<List<TelegramChannel>> = repository.allChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allJobs: StateFlow<List<BackupJob>> = repository.allJobs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentFiles: StateFlow<List<BackupFileRecord>> = repository.recentFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLogs: StateFlow<List<BackupLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalFilesCount: StateFlow<Int> = repository.totalFilesCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalBytesCount: StateFlow<Long> = repository.totalBytesCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val currentProgress: StateFlow<BackupProgressState> = repository.currentProgress

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage = _uiMessage.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection = _isTestingConnection.asStateFlow()

    private val _decryptResult = MutableStateFlow<DecryptResultState?>(null)
    val decryptResult = _decryptResult.asStateFlow()

    private val _isDecrypting = MutableStateFlow(false)
    val isDecrypting = _isDecrypting.asStateFlow()

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    fun clearDecryptResult() {
        _decryptResult.value = null
    }

    // --- Channel Operations ---
    fun saveChannel(channel: TelegramChannel, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.saveChannel(channel)
                _uiMessage.value = "Channel '${channel.name}' saved"
                onComplete()
            } catch (e: Exception) {
                _uiMessage.value = "Error saving channel: ${e.message}"
            }
        }
    }

    fun deleteChannel(channel: TelegramChannel) {
        viewModelScope.launch {
            try {
                repository.deleteChannel(channel)
                _uiMessage.value = "Channel deleted"
            } catch (e: Exception) {
                _uiMessage.value = "Error deleting channel: ${e.message}"
            }
        }
    }

    fun testChannelConnection(botToken: String, chatId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isTestingConnection.value = true
            try {
                val result = repository.testChannelConnection(botToken, chatId)
                if (result.isSuccess) {
                    val msg = result.getOrNull() ?: "Connection Successful!"
                    onResult(true, msg)
                } else {
                    val err = result.exceptionOrNull()?.message ?: "Connection Failed"
                    onResult(false, err)
                }
            } finally {
                _isTestingConnection.value = false
            }
        }
    }

    // --- Job Operations ---
    fun saveJob(job: BackupJob, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val savedId = repository.saveJob(job)
                val fullJob = job.copy(id = if (job.id == 0L) savedId else job.id)
                WorkSchedulerHelper.updateJobSchedule(getApplication(), fullJob)
                _uiMessage.value = "Backup job '${job.title}' saved & scheduled"
                onComplete()
            } catch (e: Exception) {
                _uiMessage.value = "Error saving backup job: ${e.message}"
            }
        }
    }

    fun deleteJob(job: BackupJob) {
        viewModelScope.launch {
            try {
                WorkSchedulerHelper.cancelJobSchedule(getApplication(), job.id)
                repository.deleteJob(job)
                _uiMessage.value = "Backup job '${job.title}' removed"
            } catch (e: Exception) {
                _uiMessage.value = "Error removing backup job: ${e.message}"
            }
        }
    }

    fun runBackupJobNow(jobId: Long) {
        BackupForegroundService.startBackup(getApplication(), jobId)
        _uiMessage.value = "Backup started in background"
    }

    fun runAllJobsNow() {
        BackupForegroundService.startBackup(getApplication(), -1L)
        _uiMessage.value = "Full backup started for all folders"
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            _uiMessage.value = "Activity logs cleared"
        }
    }

    // --- Built-in Zero-Loss Decrypt & Restore Tool ---
    fun decryptAndRestoreFile(
        sourceUri: Uri,
        outputFileName: String,
        passphrase: String
    ) {
        viewModelScope.launch {
            _isDecrypting.value = true
            _decryptResult.value = null
            try {
                withContext(Dispatchers.IO) {
                    val context = getApplication<Application>()
                    val cacheIn = File(context.cacheDir, "decrypt_in_${System.currentTimeMillis()}.tmp")
                    val cacheOut = File(context.cacheDir, "restored_${System.currentTimeMillis()}_$outputFileName")

                    // Copy input stream to cache file
                    context.contentResolver.openInputStream(sourceUri)?.use { input ->
                        FileOutputStream(cacheIn).use { output ->
                            input.copyTo(output)
                        }
                    } ?: throw IllegalStateException("Could not open selected encrypted file")

                    // Decrypt
                    CryptoEngine.decryptFile(cacheIn, cacheOut, passphrase)

                    // Verify SHA-256 of restored file
                    val restoredSha256 = CryptoEngine.calculateFileSha256(cacheOut)
                    val fileSize = cacheOut.length()

                    // Cleanup in temp
                    cacheIn.delete()

                    _decryptResult.value = DecryptResultState(
                        isSuccess = true,
                        message = "Successfully decrypted! Size: ${formatBytes(fileSize)}",
                        sha256 = restoredSha256,
                        outputFileName = cacheOut.name
                    )
                }
            } catch (e: Exception) {
                _decryptResult.value = DecryptResultState(
                    isSuccess = false,
                    message = "Decryption failed: ${e.message}. Check passphrase or file integrity."
                )
            } finally {
                _isDecrypting.value = false
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
