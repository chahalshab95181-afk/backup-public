package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TelegramChannel
import kotlinx.coroutines.flow.Flow

@Dao
interface TelegramChannelDao {
    @Query("SELECT * FROM telegram_channels ORDER BY createdAt DESC")
    fun getAllChannels(): Flow<List<TelegramChannel>>

    @Query("SELECT * FROM telegram_channels WHERE id = :id LIMIT 1")
    suspend fun getChannelById(id: Long): TelegramChannel?

    @Query("SELECT * FROM telegram_channels WHERE isActive = 1")
    suspend fun getActiveChannelsSync(): List<TelegramChannel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: TelegramChannel): Long

    @Update
    suspend fun updateChannel(channel: TelegramChannel)

    @Delete
    suspend fun deleteChannel(channel: TelegramChannel)

    @Query("DELETE FROM telegram_channels WHERE id = :id")
    suspend fun deleteChannelById(id: Long)
}
