package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.BackupFileRecord
import com.example.data.model.BackupJob
import com.example.data.model.BackupLog
import com.example.data.model.TelegramChannel

@Database(
    entities = [
        TelegramChannel::class,
        BackupJob::class,
        BackupFileRecord::class,
        BackupLog::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TeleVaultDatabase : RoomDatabase() {
    abstract fun telegramChannelDao(): TelegramChannelDao
    abstract fun backupJobDao(): BackupJobDao
    abstract fun backupFileRecordDao(): BackupFileRecordDao
    abstract fun backupLogDao(): BackupLogDao

    companion object {
        @Volatile
        private var INSTANCE: TeleVaultDatabase? = null

        fun getInstance(context: Context): TeleVaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TeleVaultDatabase::class.java,
                    "televault_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
