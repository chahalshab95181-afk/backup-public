package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "telegram_channels")
data class TelegramChannel(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val botToken: String,
    val chatId: String,
    val topicId: Int? = null,
    val botUsername: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
