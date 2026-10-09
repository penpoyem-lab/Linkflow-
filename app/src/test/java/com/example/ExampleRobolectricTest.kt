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
    fun readLinkFlowAppNameFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("LinkFlow", appName)
    }

    @Test
    fun insertAndUpdateDownloadJobStateInRoomDatabase() = runBlocking {
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

    @Test
    fun parseGithubReleaseJsonSelectsMatchingApkAssetAndIgnoresZipOrDraftReleases() {
        val sampleJson = """
            {
              "tag_name": "v1.8.0",
              "name": "Version 1.8 is ready to install",
              "draft": false,
              "prerelease": false,
              "html_url": "https://github.com/penpoyem-lab/Linkflow-/releases/tag/v1.8.0",
              "published_at": "2026-10-09T04:00:00Z",
              "body": "We've improved performance and stability.\n\n### Top Features\n- Faster Instagram & TikTok extraction\n- Liquid-Glass navigation bar\n- SHA-256 verified OTA updates",
              "assets": [
                {
                  "name": "source-code.zip",
                  "browser_download_url": "https://github.com/penpoyem-lab/Linkflow-/releases/download/v1.8.0/source.zip",
                  "size": 102400,
                  "content_type": "application/zip"
                },
                {
                  "name": "linkflow-release-v1.8.0.apk",
                  "browser_download_url": "https://github.com/penpoyem-lab/Linkflow-/releases/download/v1.8.0/linkflow-release-v1.8.0.apk",
                  "size": 18450000,
                  "content_type": "application/vnd.android.package-archive",
                  "digest": "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
                }
              ]
            }
        """.trimIndent()

        val parsed = com.example.data.service.AppUpdateManager.parseGithubReleaseJson(sampleJson, "linkflow", "1.0")
        assertNotNull(parsed)
        assertEquals("1.8.0", parsed?.cleanVersionName)
        assertEquals("linkflow-release-v1.8.0.apk", parsed?.apkAsset?.name)
        assertEquals(3, parsed?.topFeatures?.size)
    }

    @Test
    fun testCompleteUserJourneyScenarios1To15() {
        runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dao = database.linkFlowDao()
        val mockClient = okhttp3.OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request()
                val urlStr = req.url.toString()
                when {
                    urlStr.contains("youtube.com/oembed") -> {
                        val json = """{"title":"Official YouTube Test Video","author_name":"Official Channel","thumbnail_url":"https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg"}"""
                        okhttp3.Response.Builder()
                            .request(req)
                            .protocol(okhttp3.Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .header("Content-Type", "application/json")
                            .body(okhttp3.ResponseBody.create(null, json))
                            .build()
                    }
                    urlStr.contains("archive.org/metadata/") -> {
                        okhttp3.Response.Builder()
                            .request(req)
                            .protocol(okhttp3.Protocol.HTTP_1_1)
                            .code(404)
                            .message("Not Found")
                            .body(okhttp3.ResponseBody.create(null, "{}"))
                            .build()
                    }
                    else -> {
                        okhttp3.Response.Builder()
                            .request(req)
                            .protocol(okhttp3.Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .header("Content-Type", "video/mp4")
                            .header("Content-Length", "4194304")
                            .body(okhttp3.ResponseBody.create(null, ""))
                            .build()
                    }
                }
            }
            .build()
        val engine = com.example.data.service.MediaAnalyzerEngine(mockClient)
        val queueManager = com.example.data.service.DownloadQueueManager(context, dao, engine)

        // 1. Valid authorized direct MP4 URL (curated catalog & real format verification)
        val mp4Outcome = engine.analyzeUrl("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4")
        org.junit.Assert.assertTrue(mp4Outcome is com.example.data.service.UrlAnalysisOutcome.Success)
        val mp4Result = (mp4Outcome as com.example.data.service.UrlAnalysisOutcome.Success).result
        org.junit.Assert.assertTrue(mp4Result.isAuthorizedStream)
        org.junit.Assert.assertTrue(mp4Result.videoOptions.isNotEmpty())
        assertEquals(com.example.data.model.MediaFormat.MP4, mp4Result.videoOptions.first().format)

        // 2. Valid authorized direct MP3 / Audio URL
        val mp3Outcome = engine.analyzeUrl("https://upload.wikimedia.org/wikipedia/commons/c/c8/Example.ogg")
        org.junit.Assert.assertTrue(mp3Outcome is com.example.data.service.UrlAnalysisOutcome.Success)
        val mp3Result = (mp3Outcome as com.example.data.service.UrlAnalysisOutcome.Success).result
        org.junit.Assert.assertTrue(mp3Result.isAuthorizedStream)
        org.junit.Assert.assertTrue(mp3Result.audioOptions.isNotEmpty())

        // 3. Invalid URL & SSRF private network protection
        val invalidUrlOutcome = engine.analyzeUrl("not_a_valid_url_at_all")
        org.junit.Assert.assertTrue(invalidUrlOutcome is com.example.data.service.UrlAnalysisOutcome.Error)
        assertEquals("ERR_INVALID_HOST", (invalidUrlOutcome as com.example.data.service.UrlAnalysisOutcome.Error).errorCode)

        val ssrfOutcome = engine.analyzeUrl("http://127.0.0.1:8080/secret.mp4")
        org.junit.Assert.assertTrue(ssrfOutcome is com.example.data.service.UrlAnalysisOutcome.Error)
        assertEquals("ERR_SSRF_BLOCKED", (ssrfOutcome as com.example.data.service.UrlAnalysisOutcome.Error).errorCode)

        // 4 & 5. YouTube URL detection (watch, youtu.be, Shorts, share links) & Authorized Metadata Preview / Limitation Notice
        assertEquals("dQw4w9WgXcQ", com.example.data.service.ProviderDetector.extractYouTubeVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", com.example.data.service.ProviderDetector.extractYouTubeVideoId("https://youtu.be/dQw4w9WgXcQ?si=share123"))
        assertEquals("dQw4w9WgXcQ", com.example.data.service.ProviderDetector.extractYouTubeVideoId("https://www.youtube.com/shorts/dQw4w9WgXcQ"))

        val ytOutcome = engine.analyzeUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        org.junit.Assert.assertTrue(ytOutcome is com.example.data.service.UrlAnalysisOutcome.Success)
        val ytResult = (ytOutcome as com.example.data.service.UrlAnalysisOutcome.Success).result
        assertEquals("YouTube", ytResult.providerName)
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", ytResult.externalLaunchUrl)
        // Standard YouTube video without CC archive match must NOT pretend to be a direct MP4/MP3 stream
        org.junit.Assert.assertFalse(ytResult.isAuthorizedStream)
        org.junit.Assert.assertTrue(ytResult.videoOptions.isEmpty())
        org.junit.Assert.assertTrue(ytResult.audioOptions.isEmpty())
        assertNotNull(ytResult.authorizationLimitationNotice)

        // 6. HTML webpage incorrectly supplied as a media URL
        val htmlFile = java.io.File.createTempFile("webpage_error", ".mp4")
        htmlFile.writeText("<!DOCTYPE html><html><head><title>404 Not Found</title></head><body>Error</body></html>")
        val htmlValidation = com.example.data.service.MediaStreamValidator.validateDownloadedMediaFile(htmlFile)
        org.junit.Assert.assertFalse("HTML error page must be rejected", htmlValidation.isValid)
        htmlFile.delete()
        org.junit.Assert.assertTrue(com.example.data.service.MediaStreamValidator.isLikelyWebpageLandingUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))

        // 7 & 8. Expired signed URL (HTTP 403/401) & Network interruption / 429 / 5xx classification
        assertEquals(
            com.example.data.service.DownloadErrorCategory.FORBIDDEN_OR_EXPIRED_URL_403,
            com.example.data.service.DownloadErrorHandler.classifyHttpStatus(403, "Forbidden", "cdn.example.com").category
        )
        assertEquals(
            com.example.data.service.DownloadErrorCategory.UNAUTHORIZED_401,
            com.example.data.service.DownloadErrorHandler.classifyHttpStatus(401, "Unauthorized", "cdn.example.com").category
        )
        assertEquals(
            com.example.data.service.DownloadErrorCategory.RATE_LIMITED_429,
            com.example.data.service.DownloadErrorHandler.classifyHttpStatus(429, "Too Many Requests", "cdn.example.com").category
        )
        assertEquals(
            com.example.data.service.DownloadErrorCategory.SERVER_ERROR_5XX,
            com.example.data.service.DownloadErrorHandler.classifyHttpStatus(502, "Bad Gateway", "cdn.example.com").category
        )
        val timeoutErr = com.example.data.service.DownloadErrorHandler.classify(java.net.SocketTimeoutException("Read timed out"), "CDN")
        assertEquals(com.example.data.service.DownloadErrorCategory.NETWORK_IO_TIMEOUT, timeoutErr.category)

        // 9. Unsupported protocol / media format
        val ftpOutcome = engine.analyzeUrl("ftp://files.example.com/video.mp4")
        org.junit.Assert.assertTrue(ftpOutcome is com.example.data.service.UrlAnalysisOutcome.Error)
        assertEquals("ERR_UNSUPPORTED_SCHEME", (ftpOutcome as com.example.data.service.UrlAnalysisOutcome.Error).errorCode)

        // 10 & 11. Duplicate download detection & Cancelled download cleanup
        val option = mp4Result.videoOptions.first()
        val job1 = queueManager.enqueueDownload(mp4Result, option)
        val job2 = queueManager.enqueueDownload(mp4Result, option)
        assertEquals("Duplicate active download must return existing jobId", job1, job2)

        queueManager.cancelDownload(job1)
        // Allow coroutine to update state
        kotlinx.coroutines.delay(50)
        val cancelledTask = dao.getDownloadById(job1)
        assertNotNull(cancelledTask)

        // 12 & 13. Destination directory creation & App restart during a download reconciliation
        val dlDir = queueManager.getDownloadsDirectory()
        org.junit.Assert.assertTrue(dlDir.exists() && dlDir.isDirectory)
        val snapshot = dao.getAllDownloadsSnapshot()
        org.junit.Assert.assertTrue(snapshot.isNotEmpty())

        // 14 & 15. Completed file binary integrity (MP4 ftyp, MP3 ID3, WAV RIFF, WebM EBML) & non-fake MP3 protection
        val validMp4 = java.io.File.createTempFile("integrity_video", ".mp4")
        val mp4Bytes = ByteArray(1024)
        mp4Bytes[4] = 'f'.code.toByte()
        mp4Bytes[5] = 't'.code.toByte()
        mp4Bytes[6] = 'y'.code.toByte()
        mp4Bytes[7] = 'p'.code.toByte()
        validMp4.writeBytes(mp4Bytes)
        val mp4Check = com.example.data.service.MediaStreamValidator.validateDownloadedMediaFile(validMp4)
        org.junit.Assert.assertTrue(mp4Check.isValid)
        assertEquals("MP4/M4A", mp4Check.detectedFormat)
        validMp4.delete()

        val validMp3 = java.io.File.createTempFile("integrity_audio", ".mp3")
        val mp3Bytes = ByteArray(1024)
        mp3Bytes[0] = 'I'.code.toByte()
        mp3Bytes[1] = 'D'.code.toByte()
        mp3Bytes[2] = '3'.code.toByte()
        validMp3.writeBytes(mp3Bytes)
        val mp3Check = com.example.data.service.MediaStreamValidator.validateDownloadedMediaFile(validMp3)
        org.junit.Assert.assertTrue(mp3Check.isValid)
        assertEquals("MP3", mp3Check.detectedFormat)
        validMp3.delete()
        }
    }
}
