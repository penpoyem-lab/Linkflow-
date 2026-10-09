package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DownloadTaskEntity
import com.example.data.local.LinkFlowDatabase
import com.example.data.model.DownloadJobState
import kotlinx.coroutines.flow.first
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

    private lateinit var database: LinkFlowDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LinkFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read LinkFlow app_name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("LinkFlow", appName)
    }

    @Test
    fun `insert and update download job state in Room database`() = runBlocking {
        val dao = database.linkFlowDao()
        val now = System.currentTimeMillis()
        val job = DownloadTaskEntity(
            jobId = "job_test_01",
            mediaId = "direct_01",
            sourceUrl = "https://archive.org/download/clip/video.mp4",
            targetDownloadUrl = "https://archive.org/download/clip/video.mp4",
            title = "Archive Video Stream",
            providerName = "archive.org",
            format = "MP4",
            qualityLabel = "Original Source Video",
            resolutionOrBitrate = "video/mp4",
            codec = "VIDEO/MP4",
            durationFormatted = "Video Stream",
            thumbnailUrl = null,
            state = DownloadJobState.DOWNLOADING.name,
            progressPercent = 75,
            downloadedBytes = 128_000_000L,
            totalBytes = 170_000_000L,
            speedBytesPerSec = 4_200_000L,
            etaSeconds = 10L,
            localFilePath = "/data/user/0/com.aistudio.linkflow.vqxkmp/files/downloads/video.mp4",
            fileName = "video.mp4",
            errorMessage = null,
            createdAt = now,
            updatedAt = now,
            completedAt = null
        )

        dao.upsertDownload(job)
        val loaded = dao.getDownloadById("job_test_01")
        assertNotNull(loaded)
        assertEquals(75, loaded?.progressPercent)

        dao.renameDownloadFile("job_test_01", "renamed_video.mp4")
        val all = dao.observeAllDownloads().first()
        assertEquals(1, all.size)
        assertEquals("renamed_video.mp4", all.first().fileName)
    }
}
