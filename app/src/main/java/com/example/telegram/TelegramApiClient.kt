package com.example.telegram

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class TelegramApiClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
) {

    data class BotInfo(
        val id: Long,
        val firstName: String,
        val username: String?
    )

    data class ChatInfo(
        val id: Long,
        val title: String,
        val type: String,
        val username: String?
    )

    data class UploadResult(
        val messageId: Long,
        val fileId: String?,
        val caption: String?
    )

    /**
     * Verifies the bot token by calling getMe.
     */
    suspend fun getMe(botToken: String): Result<BotInfo> = withContext(Dispatchers.IO) {
        val cleanToken = botToken.trim()
        val url = "https://api.telegram.org/bot$cleanToken/getMe"
        try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                if (json.optBoolean("ok")) {
                    val result = json.getJSONObject("result")
                    Result.success(
                        BotInfo(
                            id = result.getLong("id"),
                            firstName = result.getString("first_name"),
                            username = result.optString("username", null)
                        )
                    )
                } else {
                    val description = json.optString("description", "Failed to connect to Telegram bot")
                    Result.failure(IOException(description))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Checks if the bot has access to a specific channel/chat.
     */
    suspend fun getChat(botToken: String, chatId: String): Result<ChatInfo> = withContext(Dispatchers.IO) {
        val cleanToken = botToken.trim()
        val cleanChatId = chatId.trim()
        val url = "https://api.telegram.org/bot$cleanToken/getChat?chat_id=$cleanChatId"
        try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                if (json.optBoolean("ok")) {
                    val result = json.getJSONObject("result")
                    Result.success(
                        ChatInfo(
                            id = result.getLong("id"),
                            title = result.optString("title", result.optString("username", cleanChatId)),
                            type = result.optString("type", "channel"),
                            username = result.optString("username", null)
                        )
                    )
                } else {
                    val description = json.optString("description", "Cannot access target channel/chat. Ensure bot is added as Administrator.")
                    Result.failure(IOException(description))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sends a text message to the channel/chat.
     */
    suspend fun sendMessage(
        botToken: String,
        chatId: String,
        text: String,
        topicId: Int? = null,
        parseMode: String? = "HTML"
    ): Result<Long> = withContext(Dispatchers.IO) {
        val cleanToken = botToken.trim()
        val url = "https://api.telegram.org/bot$cleanToken/sendMessage"
        try {
            val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId.trim())
                .addFormDataPart("text", text)

            if (!parseMode.isNullOrEmpty()) {
                builder.addFormDataPart("parse_mode", parseMode)
            }
            if (topicId != null && topicId > 0) {
                builder.addFormDataPart("message_thread_id", topicId.toString())
            }

            val request = Request.Builder().url(url).post(builder.build()).build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                if (json.optBoolean("ok")) {
                    val msgId = json.getJSONObject("result").getLong("message_id")
                    Result.success(msgId)
                } else {
                    Result.failure(IOException(json.optString("description", "Failed to send message")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Uploads a file/document to the specified Telegram channel/chat.
     * Supports real-time upload progress callback.
     */
    suspend fun sendDocument(
        botToken: String,
        chatId: String,
        file: File,
        fileName: String,
        caption: String? = null,
        topicId: Int? = null,
        onProgress: ((bytesUploaded: Long, totalBytes: Long) -> Unit)? = null
    ): Result<UploadResult> = withContext(Dispatchers.IO) {
        val cleanToken = botToken.trim()
        val url = "https://api.telegram.org/bot$cleanToken/sendDocument"

        try {
            val fileRequestBody = CountingFileRequestBody(file, "application/octet-stream".toMediaTypeOrNull()) { bytesWritten, totalBytes ->
                onProgress?.invoke(bytesWritten, totalBytes)
            }

            val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId.trim())
                .addFormDataPart("document", fileName, fileRequestBody)

            if (!caption.isNullOrBlank()) {
                builder.addFormDataPart("caption", caption)
                builder.addFormDataPart("parse_mode", "HTML")
            }
            if (topicId != null && topicId > 0) {
                builder.addFormDataPart("message_thread_id", topicId.toString())
            }

            val request = Request.Builder().url(url).post(builder.build()).build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                if (json.optBoolean("ok")) {
                    val result = json.getJSONObject("result")
                    val msgId = result.getLong("message_id")
                    val doc = result.optJSONObject("document")
                    val fileId = doc?.optString("file_id")
                    Result.success(
                        UploadResult(
                            messageId = msgId,
                            fileId = fileId,
                            caption = caption
                        )
                    )
                } else {
                    val desc = json.optString("description", "Telegram upload failed")
                    Result.failure(IOException(desc))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Custom RequestBody that notifies progress as bytes are sent over the network socket.
     */
    private class CountingFileRequestBody(
        private val file: File,
        private val contentType: MediaType?,
        private val onProgress: (bytesWritten: Long, totalBytes: Long) -> Unit
    ) : RequestBody() {
        override fun contentType(): MediaType? = contentType

        override fun contentLength(): Long = file.length()

        override fun writeTo(sink: BufferedSink) {
            val fileLength = file.length()
            val buffer = ByteArray(32 * 1024)
            var uploaded: Long = 0

            FileInputStream(file).use { fis ->
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    sink.write(buffer, 0, read)
                    uploaded += read
                    onProgress(uploaded, fileLength)
                }
            }
        }
    }
}
