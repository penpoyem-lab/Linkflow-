package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.MediaFormat
import com.example.data.service.OtaUpdateUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "linkflow_preferences")

data class UserPreferencesState(
    val appBrandName: String = "LinkFlow",
    val themeMode: String = "DARK", // "DARK", "LIGHT", "SYSTEM"
    val accentPreset: String = "ELECTRIC_CYAN", // "ELECTRIC_CYAN", "VIOLET_PULSE", "EMERALD_GLOW"
    val preferredFormat: MediaFormat = MediaFormat.MP4,
    val preferredVideoQuality: String = "1080p Full HD",
    val preferredAudioBitrate: String = "320kbps",
    val reducedMotion: Boolean = false,
    val hapticFeedbackEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val wifiOnlyDownloads: Boolean = false,
    val smartClipboardPrompt: Boolean = true,
    val githubRepoSlug: String = OtaUpdateUiState.DEFAULT_GITHUB_REPO,
    val dismissedUpdateTag: String? = null,
    val dismissedUpdateTimestampMs: Long = 0L
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val APP_BRAND_NAME = stringPreferencesKey("app_brand_name")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_PRESET = stringPreferencesKey("accent_preset")
        val PREFERRED_FORMAT = stringPreferencesKey("preferred_format")
        val PREFERRED_VIDEO_QUALITY = stringPreferencesKey("preferred_video_quality")
        val PREFERRED_AUDIO_BITRATE = stringPreferencesKey("preferred_audio_bitrate")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val SMART_CLIPBOARD = booleanPreferencesKey("smart_clipboard")
        val GITHUB_REPO_SLUG = stringPreferencesKey("github_repo_slug")
        val DISMISSED_UPDATE_TAG = stringPreferencesKey("dismissed_update_tag")
        val DISMISSED_UPDATE_TS = longPreferencesKey("dismissed_update_timestamp_ms")
    }

    val preferencesFlow: Flow<UserPreferencesState> = context.dataStore.data.map { prefs ->
        UserPreferencesState(
            appBrandName = prefs[Keys.APP_BRAND_NAME] ?: "LinkFlow",
            themeMode = prefs[Keys.THEME_MODE] ?: "DARK",
            accentPreset = prefs[Keys.ACCENT_PRESET] ?: "ELECTRIC_CYAN",
            preferredFormat = if (prefs[Keys.PREFERRED_FORMAT] == "MP3") MediaFormat.MP3 else MediaFormat.MP4,
            preferredVideoQuality = prefs[Keys.PREFERRED_VIDEO_QUALITY] ?: "1080p Full HD",
            preferredAudioBitrate = prefs[Keys.PREFERRED_AUDIO_BITRATE] ?: "320kbps",
            reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
            hapticFeedbackEnabled = prefs[Keys.HAPTIC_FEEDBACK] ?: true,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            wifiOnlyDownloads = prefs[Keys.WIFI_ONLY] ?: false,
            smartClipboardPrompt = prefs[Keys.SMART_CLIPBOARD] ?: true,
            githubRepoSlug = prefs[Keys.GITHUB_REPO_SLUG] ?: OtaUpdateUiState.DEFAULT_GITHUB_REPO,
            dismissedUpdateTag = prefs[Keys.DISMISSED_UPDATE_TAG],
            dismissedUpdateTimestampMs = prefs[Keys.DISMISSED_UPDATE_TS] ?: 0L
        )
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setAccentPreset(preset: String) {
        context.dataStore.edit { it[Keys.ACCENT_PRESET] = preset }
    }

    suspend fun setAppBrandName(name: String) {
        val clean = name.trim().ifEmpty { "LinkFlow" }.take(24)
        context.dataStore.edit { it[Keys.APP_BRAND_NAME] = clean }
    }

    suspend fun setPreferredFormat(format: MediaFormat) {
        context.dataStore.edit { it[Keys.PREFERRED_FORMAT] = format.name }
    }

    suspend fun setPreferredVideoQuality(quality: String) {
        context.dataStore.edit { it[Keys.PREFERRED_VIDEO_QUALITY] = quality }
    }

    suspend fun setPreferredAudioBitrate(bitrate: String) {
        context.dataStore.edit { it[Keys.PREFERRED_AUDIO_BITRATE] = bitrate }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REDUCED_MOTION] = enabled }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTIC_FEEDBACK] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setWifiOnlyDownloads(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WIFI_ONLY] = enabled }
    }

    suspend fun setGithubRepoSlug(slug: String) {
        val clean = slug.trim().removePrefix("https://github.com/").trim('/')
        if (clean.contains('/')) {
            context.dataStore.edit { it[Keys.GITHUB_REPO_SLUG] = clean }
        }
    }

    suspend fun recordDismissedUpdate(tagName: String, timestampMs: Long = System.currentTimeMillis()) {
        context.dataStore.edit {
            it[Keys.DISMISSED_UPDATE_TAG] = tagName
            it[Keys.DISMISSED_UPDATE_TS] = timestampMs
        }
    }
}
