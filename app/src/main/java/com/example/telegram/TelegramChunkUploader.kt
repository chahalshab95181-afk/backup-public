package com.example.telegram

import android.content.Context
import android.net.Uri
import com.example.crypto.CryptoEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

class TelegramChunkUploader(
    private val context: Context,
    private val apiClient: TelegramApiClient = TelegramApiClient()
) {

    data class ChunkUploadProgress(
        val fileName: String,
        val currentChunkIndex: Int,
        val totalChunks: Int,
        val currentChunkBytes: Long,
        val currentChunkTotalBytes: Long,
        val overallBytesUploaded: Long,
        val overallTotalBytes: Long
    )

    data class ChunkUploadResult(
        val originalSha256: String,
        val totalChunks: Int,
        val messageIds: List<Long>,
        val isEncrypted: Boolean
    )

    /**
     * Uploads a file (or folder item) from its content Uri.
     * Automatically performs:
     * 1. SHA-256 calculation for zero loss verification.
     * 2. Splitting into chunks if file exceeds chunkSizeMb (default 45MB).
     * 3. AES-256-GCM encryption if isEncrypted is true.
     * 4. Upload of all chunks + manifest.
     */
    suspend fun uploadFile(
        fileUri: Uri,
        originalFileName: String,
        relativeFilePath: String,
        fileSize: Long,
        botToken: String,
        chatId: String,
        topicId: Int? = null,
        isEncrypted: Boolean,
        passphrase: String,
        chunkSizeMb: Int = 45,
        onProgress: (ChunkUploadProgress) -> Unit
    ): Result<ChunkUploadResult> = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "televault_upload_${System.currentTimeMillis()}")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        try {
            // Step 1: Calculate original SHA-256 hash to ensure zero loss
            val originalSha256 = context.contentResolver.openInputStream(fileUri)?.use {
                CryptoEngine.calculateSha256(it)
            } ?: throw IllegalStateException("Could not read file from Uri: $fileUri")

            val chunkSizeBytes = (chunkSizeMb.toLong() * 1024 * 1024).coerceIn(5 * 1024 * 1024, 48 * 1024 * 1024)
            val totalChunks = if (fileSize <= 0) 1 else ((fileSize + chunkSizeBytes - 1) / chunkSizeBytes).toInt().coerceAtLeast(1)

            val uploadedMessageIds = mutableListOf<Long>()
            val chunkManifestList = mutableListOf<JSONObject>()

            var overallBytesUploaded = 0L

            if (totalChunks == 1 && fileSize < chunkSizeBytes) {
                // Single chunk upload
                val rawTemp = File(cacheDir, "temp_raw_$originalFileName")
                context.contentResolver.openInputStream(fileUri)?.use { input ->
                    FileOutputStream(rawTemp).use { output ->
                        input.copyTo(output)
                    }
                }

                val uploadFile = if (isEncrypted) {
                    val encTemp = File(cacheDir, "$originalFileName.enc")
                    FileInputStream(rawTemp).use { fis ->
                        CryptoEngine.encryptFile(fis, encTemp, passphrase)
                    }
                    rawTemp.delete()
                    encTemp
                } else {
                    rawTemp
                }

                val chunkSha256 = CryptoEngine.calculateFileSha256(uploadFile)
                val caption = buildString {
                    append(if (isEncrypted) "🔐 <b>TeleVault Encrypted Backup</b>\n" else "📦 <b>TeleVault Backup</b>\n")
                    append("📄 <b>File:</b> <code>$relativeFilePath</code>\n")
                    append("📏 <b>Size:</b> ${formatFileSize(fileSize)}\n")
                    append("🛡️ <b>SHA-256:</b> <code>$originalSha256</code>\n")
                    if (isEncrypted) append("🔒 <b>Encryption:</b> AES-256-GCM\n")
                    append("⏱️ <b>Date:</b> ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}")
                }

                val uploadRes = apiClient.sendDocument(
                    botToken = botToken,
                    chatId = chatId,
                    file = uploadFile,
                    fileName = uploadFile.name,
                    caption = caption,
                    topicId = topicId
                ) { bytesUploaded, totalBytes ->
                    onProgress(
                        ChunkUploadProgress(
                            fileName = originalFileName,
                            currentChunkIndex = 1,
                            totalChunks = 1,
                            currentChunkBytes = bytesUploaded,
                            currentChunkTotalBytes = totalBytes,
                            overallBytesUploaded = bytesUploaded,
                            overallTotalBytes = totalBytes
                        )
                    )
                }

                uploadFile.delete()

                val result = uploadRes.getOrThrow()
                uploadedMessageIds.add(result.messageId)

                Result.success(
                    ChunkUploadResult(
                        originalSha256 = originalSha256,
                        totalChunks = 1,
                        messageIds = uploadedMessageIds,
                        isEncrypted = isEncrypted
                    )
                )
            } else {
                // Multi-chunk upload for large files
                val inputStream = context.contentResolver.openInputStream(fileUri)
                    ?: throw IllegalStateException("Could not open input stream")

                inputStream.use { stream ->
                    val buffer = ByteArray(64 * 1024)
                    for (chunkIndex in 1..totalChunks) {
                        val partName = "%s.part%03d".format(originalFileName, chunkIndex)
                        val rawChunkFile = File(cacheDir, "raw_$partName")
                        var chunkBytesRemaining = chunkSizeBytes
                        var chunkBytesWritten = 0L

                        FileOutputStream(rawChunkFile).use { fos ->
                            while (chunkBytesRemaining > 0) {
                                val toRead = buffer.size.toLong().coerceAtMost(chunkBytesRemaining).toInt()
                                val read = stream.read(buffer, 0, toRead)
                                if (read == -1) break
                                fos.write(buffer, 0, read)
                                chunkBytesRemaining -= read
                                chunkBytesWritten += read
                            }
                        }

                        if (chunkBytesWritten == 0L && chunkIndex > 1) {
                            rawChunkFile.delete()
                            break
                        }

                        val uploadChunkFile = if (isEncrypted) {
                            val encChunkFile = File(cacheDir, "$partName.enc")
                            FileInputStream(rawChunkFile).use { fis ->
                                CryptoEngine.encryptFile(fis, encChunkFile, passphrase)
                            }
                            rawChunkFile.delete()
                            encChunkFile
                        } else {
                            rawChunkFile
                        }

                        val chunkSha256 = CryptoEngine.calculateFileSha256(uploadChunkFile)
                        val chunkObj = JSONObject().apply {
                            put("chunkIndex", chunkIndex)
                            put("partName", uploadChunkFile.name)
                            put("chunkSize", uploadChunkFile.length())
                            put("chunkSha256", chunkSha256)
                        }
                        chunkManifestList.add(chunkObj)

                        val caption = buildString {
                            append(if (isEncrypted) "🔐 <b>TeleVault Large File Part ($chunkIndex/$totalChunks)</b>\n" else "📦 <b>TeleVault Part ($chunkIndex/$totalChunks)</b>\n")
                            append("📄 <b>Original:</b> <code>$relativeFilePath</code>\n")
                            append("🧩 <b>Chunk Size:</b> ${formatFileSize(uploadChunkFile.length())}\n")
                            append("🛡️ <b>Chunk SHA:</b> <code>$chunkSha256</code>\n")
                            if (isEncrypted) append("🔒 <b>Encryption:</b> AES-256-GCM\n")
                        }

                        val lastOverall = overallBytesUploaded
                        val uploadRes = apiClient.sendDocument(
                            botToken = botToken,
                            chatId = chatId,
                            file = uploadChunkFile,
                            fileName = uploadChunkFile.name,
                            caption = caption,
                            topicId = topicId
                        ) { bytesUploaded, totalBytes ->
                            onProgress(
                                ChunkUploadProgress(
                                    fileName = originalFileName,
                                    currentChunkIndex = chunkIndex,
                                    totalChunks = totalChunks,
                                    currentChunkBytes = bytesUploaded,
                                    currentChunkTotalBytes = totalBytes,
                                    overallBytesUploaded = lastOverall + bytesUploaded,
                                    overallTotalBytes = fileSize
                                )
                            )
                        }

                        uploadChunkFile.delete()
                        val result = uploadRes.getOrThrow()
                        uploadedMessageIds.add(result.messageId)
                        overallBytesUploaded += chunkBytesWritten
                    }
                }

                // Send Master Manifest for multi-part file to guarantee zero loss reconstruction
                val manifestFile = File(cacheDir, "$originalFileName.manifest.json")
                val manifestJson = JSONObject().apply {
                    put("originalFileName", originalFileName)
                    put("relativeFilePath", relativeFilePath)
                    put("originalFileSize", fileSize)
                    put("originalSha256", originalSha256)
                    put("totalChunks", uploadedMessageIds.size)
                    put("isEncrypted", isEncrypted)
                    put("createdAt", System.currentTimeMillis())
                    put("chunks", JSONArray(chunkManifestList))
                }
                manifestFile.writeText(manifestJson.toString(2))

                val manifestCaption = buildString {
                    append("📋 <b>TeleVault Multi-Part Manifest</b>\n")
                    append("📄 <b>File:</b> <code>$relativeFilePath</code>\n")
                    append("🧩 <b>Total Parts:</b> ${uploadedMessageIds.size}\n")
                    append("📏 <b>Total Size:</b> ${formatFileSize(fileSize)}\n")
                    append("🛡️ <b>Master SHA-256:</b> <code>$originalSha256</code>\n")
                    append("✅ <i>Contains checksums for zero-loss reassembly</i>")
                }

                val manifestRes = apiClient.sendDocument(
                    botToken = botToken,
                    chatId = chatId,
                    file = manifestFile,
                    fileName = manifestFile.name,
                    caption = manifestCaption,
                    topicId = topicId
                )
                manifestFile.delete()
                manifestRes.getOrNull()?.let { uploadedMessageIds.add(it.messageId) }

                Result.success(
                    ChunkUploadResult(
                        originalSha256 = originalSha256,
                        totalChunks = uploadedMessageIds.size,
                        messageIds = uploadedMessageIds,
                        isEncrypted = isEncrypted
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            cacheDir.deleteRecursively()
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        return "%.2f %s".format(bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
