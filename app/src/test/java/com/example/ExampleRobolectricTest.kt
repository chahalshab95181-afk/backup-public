package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.TeleVaultDatabase
import com.example.data.model.BackupJob
import com.example.data.model.TelegramChannel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: TeleVaultDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TeleVaultDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("TeleVault", appName)
    }

    @Test
    fun `test database channel and job insertion`() = runBlocking {
        val channelDao = database.telegramChannelDao()
        val jobDao = database.backupJobDao()

        val channel = TelegramChannel(
            name = "Test Channel",
            botToken = "123456:ABC-DEF",
            chatId = "-1001234567890"
        )
        val channelId = channelDao.insertChannel(channel)
        val fetchedChannel = channelDao.getChannelById(channelId)
        assertNotNull(fetchedChannel)
        assertEquals("Test Channel", fetchedChannel?.name)

        val job = BackupJob(
            title = "Test Job",
            folderUri = "content://com.android.externalstorage.documents/tree/primary%3ADCIM",
            folderDisplayName = "DCIM",
            channelId = channelId,
            isEncrypted = true,
            isScheduleEnabled = true
        )
        val jobId = jobDao.insertJob(job)
        val fetchedJob = jobDao.getJobById(jobId)
        assertNotNull(fetchedJob)
        assertEquals("Test Job", fetchedJob?.title)
        assertEquals("DCIM", fetchedJob?.folderDisplayName)
    }
}
