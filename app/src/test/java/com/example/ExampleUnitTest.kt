package com.example

import com.example.data.service.AppUpdateManager
import com.example.data.service.MediaAnalyzerEngine
import com.example.data.service.UrlAnalysisOutcome
import com.example.util.FormatUtils
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    private val engine = MediaAnalyzerEngine()

    @Test
    fun `empty url returns ERR_EMPTY_INPUT`() = runBlocking {
        val outcome = engine.analyzeUrl("   ")
        assertTrue(outcome is UrlAnalysisOutcome.Error)
        val err = outcome as UrlAnalysisOutcome.Error
        assertEquals("ERR_EMPTY_INPUT", err.errorCode)
    }

    @Test
    fun `ssrf localhost and private network urls are blocked`() = runBlocking {
        val localhostOutcome = engine.analyzeUrl("http://127.0.0.1:8080/secret.mp4")
        assertTrue(localhostOutcome is UrlAnalysisOutcome.Error)
        assertEquals("ERR_SSRF_BLOCKED", (localhostOutcome as UrlAnalysisOutcome.Error).errorCode)

        val privateLanOutcome = engine.analyzeUrl("https://192.168.1.50/stream.mp4")
        assertTrue(privateLanOutcome is UrlAnalysisOutcome.Error)
        assertEquals("ERR_SSRF_BLOCKED", (privateLanOutcome as UrlAnalysisOutcome.Error).errorCode)
    }

    @Test
    fun `direct mp4 url is analyzed and returns real video option`() = runBlocking {
        val directUrl = "https://archive.org/download/sample_video_stream/sample_clip.mp4"
        val outcome = engine.analyzeUrl(directUrl)
        assertTrue(outcome is UrlAnalysisOutcome.Success)
        val result = (outcome as UrlAnalysisOutcome.Success).result
        assertTrue(result.videoOptions.isNotEmpty())
        assertEquals(directUrl, result.videoOptions.first().downloadUrl)
    }

    @Test
    fun `extractUrlFromSharedText extracts clean url from social media share caption`() {
        val sharedCaption = "Check out this awesome clip! https://www.tiktok.com/@creator/video/1234567890?is_from_webapp=1&sender_device=pcShared via TikTok."
        val extracted = MediaAnalyzerEngine.extractUrlFromSharedText(sharedCaption)
        assertTrue(extracted.startsWith("https://www.tiktok.com/@creator/video/1234567890"))
    }

    @Test
    fun `instagram reel share link resolves to exact embedded video and audio stream`() = runBlocking {
        val realStreamUrl = "https://scontent.cdninstagram.com/v/t66.30100-16/exact_reel_stream.mp4"
        val mockClient = okhttp3.OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request()
                val htmlBody = """
                    <html>
                    <head>
                        <meta property="og:title" content="@creator_handle" />
                        <meta property="og:description" content="Exact Reel Caption" />
                    </head>
                    <body>
                        <script>{"video_url":"$realStreamUrl","username":"creator_handle"}</script>
                    </body>
                    </html>
                """.trimIndent()
                okhttp3.Response.Builder()
                    .request(req)
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .header("Content-Type", "text/html")
                    .body(okhttp3.ResponseBody.create(null, htmlBody))
                    .build()
            }
            .build()

        val testEngine = MediaAnalyzerEngine(mockClient)
        val igOutcome = testEngine.analyzeUrl("https://www.instagram.com/reel/C5xyz123/?igsh=d2NybnF4NWVyeGFj")
        assertTrue(igOutcome is UrlAnalysisOutcome.Success)
        val result = (igOutcome as UrlAnalysisOutcome.Success).result
        assertTrue(result.videoOptions.isNotEmpty())
        assertEquals(realStreamUrl, result.videoOptions.first().downloadUrl)
        assertTrue(result.audioOptions.isNotEmpty())
        assertEquals(realStreamUrl, result.audioOptions.first().downloadUrl)
    }

    @Test
    fun `byte formatting is accurate`() {
        assertEquals("0 B", FormatUtils.formatBytes(0))
        assertEquals("1.0 MB", FormatUtils.formatBytes(1024L * 1024L))
    }

    @Test
    fun `semantic version comparison handles multi-digit versions and pre-releases accurately`() {
        assertTrue(AppUpdateManager.compareSemanticVersions("v1.10.0", "v1.2.9") > 0)
        assertTrue(AppUpdateManager.compareSemanticVersions("2.5", "1.0") > 0)
        assertEquals(0, AppUpdateManager.compareSemanticVersions("v1.8.0", "1.8"))
        assertTrue(AppUpdateManager.compareSemanticVersions("1.8.0", "1.8.0-beta1") > 0)
    }

    @Test
    fun `parseMarkdownChangelog extracts summary and top features accurately`() {
        val markdown = """
            We've improved performance and stability across all streams.
            
            ### Top Features
            - Faster Instagram & TikTok extraction
            - Liquid-Glass navigation bar
            - SHA-256 verified OTA updates
        """.trimIndent()

        val parsed = AppUpdateManager.parseMarkdownChangelog(markdown, "1.0")
        assertTrue(parsed.summaryParagraph.contains("performance"))
        assertEquals(3, parsed.topFeatures.size)
        assertFalse(parsed.isMandatory)
    }
}
