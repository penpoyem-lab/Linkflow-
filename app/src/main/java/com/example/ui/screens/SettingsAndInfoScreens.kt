package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.UserPreferencesState
import com.example.data.model.MediaFormat
import com.example.data.model.ProviderStatusInfo
import com.example.data.model.ProviderSupportLevel
import com.example.data.service.OtaInstallPhase
import com.example.data.service.OtaUpdateUiState
import com.example.ui.StorageMetrics
import com.example.ui.components.LinkFlowLogoEmblem
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassPrimaryButton
import com.example.ui.components.StaggeredAnimatedEntrance
import com.example.ui.components.springBounceClickable
import com.example.ui.theme.LocalLinkFlowTokens
import com.example.util.FormatUtils

@Composable
fun SettingsScreen(
    preferences: UserPreferencesState,
    storageMetrics: StorageMetrics,
    otaState: OtaUpdateUiState = OtaUpdateUiState(),
    onThemeModeChange: (String) -> Unit,
    onAccentPresetChange: (String) -> Unit,
    onBrandNameChange: (String) -> Unit,
    onPreferredFormatChange: (MediaFormat) -> Unit,
    onPreferredVideoQualityChange: (String) -> Unit,
    onPreferredAudioBitrateChange: (String) -> Unit,
    onReducedMotionChange: (Boolean) -> Unit,
    onHapticFeedbackChange: (Boolean) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onWifiOnlyChange: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
    isRefreshing: Boolean = false,
    onPullToRefresh: () -> Unit = {},
    onCheckForUpdates: () -> Unit = {},
    onPreviewUpdateDialog: () -> Unit = {},
    onGithubRepoChange: (String) -> Unit = {},
    onShowAbout: () -> Unit,
    onShowSupportedSources: () -> Unit,
    onShowPrivacyTerms: () -> Unit
) {
    val tokens = LocalLinkFlowTokens.current
    val uriHandler = LocalUriHandler.current
    var showBrandDialog by remember { mutableStateOf(false) }
    var brandInput by remember(preferences.appBrandName) { mutableStateOf(preferences.appBrandName) }
    var showRepoDialog by remember { mutableStateOf(false) }
    var repoInput by remember(preferences.githubRepoSlug) { mutableStateOf(preferences.githubRepoSlug) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showLicenseSheet by remember { mutableStateOf(false) }

    com.example.ui.components.PlayStorePullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onPullToRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .testTag("settings_lazy_column"),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 14.dp,
            bottom = 114.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            StaggeredAnimatedEntrance(index = 0, reducedMotion = preferences.reducedMotion) {
                Column {
                    Text(
                        text = "Settings & Preferences",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Customize appearance, default quality, storage, and security.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 1. Appearance & Branding Section
        item {
            StaggeredAnimatedEntrance(index = 1, reducedMotion = preferences.reducedMotion) {
            LiquidGlassCard(cornerRadius = 24.dp, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SectionHeaderRow(
                        icon = Icons.Default.Palette,
                        title = "Appearance & Branding"
                    )

                    // Theme mode selector: Dark / Light / System
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Theme Mode",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("DARK" to "Dark Glass", "LIGHT" to "Light Glass", "SYSTEM" to "System").forEach { (key, label) ->
                                val selected = preferences.themeMode == key
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (selected) tokens.accentBlue else tokens.glassSurfaceElevated)
                                        .springBounceClickable { onThemeModeChange(key) }
                                        .padding(vertical = 10.dp)
                                        .testTag("theme_mode_$key"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Accent Gradient Preset
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Liquid-Glass Accent Palette",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "ELECTRIC_CYAN" to "Electric Cyan",
                                "VIOLET_PULSE" to "Violet Pulse",
                                "EMERALD_GLOW" to "Emerald Glow"
                            ).forEach { (key, label) ->
                                val selected = preferences.accentPreset == key
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (selected) tokens.accentBlue else tokens.glassSurfaceElevated)
                                        .springBounceClickable { onAccentPresetChange(key) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Configurable Brand Name
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.2f))
                            .springBounceClickable { showBrandDialog = true }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "App Brand Name",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Current: ${preferences.appBrandName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Customize >",
                            style = MaterialTheme.typography.labelMedium,
                            color = tokens.accentCyan
                        )
                    }
                }
            }
            }
        }

        // 2. Default Output Format & Quality Section
        item {
            StaggeredAnimatedEntrance(index = 2, reducedMotion = preferences.reducedMotion) {
            LiquidGlassCard(cornerRadius = 24.dp, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SectionHeaderRow(
                        icon = Icons.Default.Tune,
                        title = "Download Defaults"
                    )

                    // Preferred Format
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Preferred Container Format",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MediaFormat.entries.forEach { fmt ->
                                val selected = preferences.preferredFormat == fmt
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (selected) tokens.accentBlue else tokens.glassSurfaceElevated)
                                        .springBounceClickable { onPreferredFormatChange(fmt) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = fmt.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Preferred Video Resolution
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Preferred Video Quality",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("720p", "1080p Full HD", "4K Ultra").forEach { q ->
                                val selected = preferences.preferredVideoQuality == q
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) tokens.accentBlue else tokens.glassSurfaceElevated)
                                        .springBounceClickable { onPreferredVideoQualityChange(q) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = q,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Preferred Audio Bitrate
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Preferred MP3 Audio Bitrate",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("128kbps", "192kbps", "320kbps").forEach { br ->
                                val selected = preferences.preferredAudioBitrate == br
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) tokens.accentBlue else tokens.glassSurfaceElevated)
                                        .springBounceClickable { onPreferredAudioBitrateChange(br) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = br,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    SettingToggleRow(
                        title = "Wi-Fi Only Downloads",
                        subtitle = "Pause large 4K transfers when on cellular data",
                        checked = preferences.wifiOnlyDownloads,
                        onCheckedChange = onWifiOnlyChange
                    )

                    SettingToggleRow(
                        title = "Download Notifications",
                        subtitle = "Show completion & background queue alerts",
                        checked = preferences.notificationsEnabled,
                        onCheckedChange = onNotificationsChange
                    )
                }
            }
            }
        }

        // 3. Accessibility & Motion System
        item {
            StaggeredAnimatedEntrance(index = 3, reducedMotion = preferences.reducedMotion) {
            LiquidGlassCard(cornerRadius = 24.dp, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionHeaderRow(
                        icon = Icons.Default.VerifiedUser,
                        title = "Accessibility & Motion"
                    )

                    SettingToggleRow(
                        title = "Reduced Motion Mode",
                        subtitle = "Simplify particle, bokeh, and spring animations",
                        checked = preferences.reducedMotion,
                        onCheckedChange = onReducedMotionChange
                    )

                    SettingToggleRow(
                        title = "Tactile Haptic Feedback",
                        subtitle = "Subtle vibration on download trigger & completion",
                        checked = preferences.hapticFeedbackEnabled,
                        onCheckedChange = onHapticFeedbackChange
                    )
                }
            }
            }
        }

        // 4. Storage Usage & History Management
        item {
            StaggeredAnimatedEntrance(index = 4, reducedMotion = preferences.reducedMotion) {
            LiquidGlassCard(cornerRadius = 24.dp, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionHeaderRow(
                        icon = Icons.Default.Storage,
                        title = "Storage & History"
                    )

                    val usedRatio = if (storageMetrics.totalDeviceBytes > 0) {
                        ((storageMetrics.totalDeviceBytes - storageMetrics.availableDeviceBytes).toFloat() /
                            storageMetrics.totalDeviceBytes.toFloat()).coerceIn(0.05f, 0.95f)
                    } else 0.25f

                    LinearProgressIndicator(
                        progress = { usedRatio },
                        color = tokens.accentCyan,
                        trackColor = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "LinkFlow Media: ${FormatUtils.formatBytes(storageMetrics.usedByAppBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.accentCyan
                        )
                        Text(
                            text = "Free Device Space: ${FormatUtils.formatBytes(storageMetrics.availableDeviceBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    LiquidGlassPrimaryButton(
                        text = "Clear Analysis & Completed History",
                        icon = Icons.Default.DeleteSweep,
                        isDestructive = true,
                        onClick = { showClearConfirm = true },
                        cornerRadius = 16.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_history_button")
                    )
                }
            }
            }
        }

        // 5. Legal, Supported Sources & About Links
        item {
            StaggeredAnimatedEntrance(index = 5, reducedMotion = preferences.reducedMotion) {
            LiquidGlassCard(cornerRadius = 24.dp, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionHeaderRow(
                        icon = Icons.Default.Security,
                        title = "Compliance, Sources & About"
                    )

                    SettingsLinkRow(
                        title = "Check for Software Updates (GitHub OTA)",
                        subtitle = when (otaState.phase) {
                            OtaInstallPhase.CHECKING -> "Checking GitHub Releases (${preferences.githubRepoSlug})…"
                            OtaInstallPhase.AVAILABLE -> "Update ${otaState.releaseInfo?.tagName ?: ""} ready to install!"
                            OtaInstallPhase.UP_TO_DATE -> "Up to date (v${otaState.installedVersionName}) • Repo: ${preferences.githubRepoSlug}"
                            else -> "Installed v${otaState.installedVersionName} • Source: github.com/${preferences.githubRepoSlug}"
                        },
                        isLoading = otaState.phase == OtaInstallPhase.CHECKING,
                        reducedMotion = preferences.reducedMotion,
                        onClick = onCheckForUpdates
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    SettingsLinkRow(
                        title = "Preview Software Update Modal",
                        subtitle = "Open the full-screen Liquid-Glass OTA Update dialog & changelog viewer",
                        onClick = onPreviewUpdateDialog
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    SettingsLinkRow(
                        title = "GitHub Release Repository",
                        subtitle = "https://github.com/${preferences.githubRepoSlug}/releases/latest",
                        onClick = { showRepoDialog = true }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    SettingsLinkRow(
                        title = "Supported Sources & Adapter Status",
                        subtitle = "Inspect verified Open-CDN, Direct HTTP, and oEmbed adapters",
                        onClick = onShowSupportedSources
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    SettingsLinkRow(
                        title = "Privacy Policy & Terms of Use",
                        subtitle = "SSRF protection, copyright compliance, and zero-tracking policy",
                        onClick = onShowPrivacyTerms
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    SettingsLinkRow(
                        title = "Open-Source License & Notices",
                        subtitle = "MIT License • Apache 2.0 Third-Party attributions",
                        onClick = { showLicenseSheet = true }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    SettingsLinkRow(
                        title = "About ${preferences.appBrandName}",
                        subtitle = "Version 2.4.0 (Build 240) • Liquid-Glass Architecture",
                        onClick = onShowAbout
                    )
                }
            }
            }
        }

        // 6. Download Latest APK Section (Last section of Settings page)
        item {
            StaggeredAnimatedEntrance(index = 6, reducedMotion = preferences.reducedMotion) {
                LiquidGlassCard(cornerRadius = 24.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SectionHeaderRow(
                            icon = Icons.Default.CloudDownload,
                            title = "Latest Official Release"
                        )
                        Text(
                            text = "Get the newest version of ${preferences.appBrandName} directly from the official GitHub Releases page.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val latestReleaseUrl = otaState.releaseInfo?.htmlUrl?.takeIf { it.isNotBlank() }
                            ?: "https://github.com/${preferences.githubRepoSlug}/releases/latest"
                        LiquidGlassPrimaryButton(
                            text = "Download Latest APK",
                            icon = Icons.Default.OpenInNew,
                            onClick = {
                                runCatching {
                                    uriHandler.openUri(latestReleaseUrl)
                                }
                            },
                            cornerRadius = 16.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("download_latest_apk_button")
                        )
                    }
                }
            }
        }
    }
    }

    if (showLicenseSheet) {
        OpenSourceLicenseDialog(
            brandName = preferences.appBrandName,
            onDismiss = { showLicenseSheet = false }
        )
    }

    if (showRepoDialog) {
        AlertDialog(
            onDismissRequest = { showRepoDialog = false },
            title = { Text("GitHub Releases Repository") },
            text = {
                OutlinedTextField(
                    value = repoInput,
                    onValueChange = { repoInput = it },
                    singleLine = true,
                    label = { Text("OWNER/REPOSITORY") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onGithubRepoChange(repoInput)
                        showRepoDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRepoDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBrandDialog) {
        AlertDialog(
            onDismissRequest = { showBrandDialog = false },
            title = { Text("Customize App Branding") },
            text = {
                OutlinedTextField(
                    value = brandInput,
                    onValueChange = { brandInput = it },
                    singleLine = true,
                    label = { Text("Brand Name") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onBrandNameChange(brandInput)
                        showBrandDialog = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBrandDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear History?") },
            text = {
                Text("This will clear your recent URL analysis records and completed task history.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearHistory()
                        showClearConfirm = false
                    }
                ) {
                    Text("Clear All", color = tokens.errorColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SectionHeaderRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    val tokens = LocalLinkFlowTokens.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tokens.accentCyan,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val tokens = LocalLinkFlowTokens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = tokens.accentBlue
            )
        )
    }
}

@Composable
private fun SettingsLinkRow(
    title: String,
    subtitle: String,
    isLoading: Boolean = false,
    reducedMotion: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isLoading) {
            com.example.ui.components.PlayStoreScallopedLoader(
                size = 26.dp,
                color = Color(0xFFEBD0C7),
                trackColor = Color(0xFF5D423B),
                isSpinning = true,
                reducedMotion = reducedMotion
            )
        } else {
            Text(
                text = ">",
                style = MaterialTheme.typography.titleMedium,
                color = LocalLinkFlowTokens.current.accentCyan
            )
        }
    }
}

@Composable
fun SupportedSourcesSheet(
    adapters: List<ProviderStatusInfo>,
    onClose: () -> Unit
) {
    val tokens = LocalLinkFlowTokens.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6050811))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(16.dp)
            .testTag("supported_sources_sheet"),
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassCard(
            cornerRadius = 28.dp,
            isHighlighted = true,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Supported Sources & Adapters",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Text(
                    text = "LinkFlow verifies every source adapter before exposing download options.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(adapters, key = { it.id }) { adapter ->
                        val isFull = adapter.status == ProviderSupportLevel.VERIFIED_ACTIVE
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(
                                    1.dp,
                                    if (isFull) tokens.successColor.copy(alpha = 0.4f) else tokens.warningColor.copy(alpha = 0.4f),
                                    RoundedCornerShape(18.dp)
                                )
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = adapter.name,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (isFull) tokens.successColor.copy(alpha = 0.2f) else tokens.warningColor.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = adapter.status.badgeText,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = if (isFull) tokens.successColor else tokens.warningColor
                                    )
                                }
                            }
                            Text(
                                text = adapter.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )
                            Text(
                                text = "Patterns: ${adapter.domainPatterns.joinToString(", ")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.accentCyan
                            )
                            Text(
                                text = "Policy: ${adapter.limitationNote}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrivacyAndTermsSheet(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6050811))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassCard(
            cornerRadius = 28.dp,
            isHighlighted = true,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Privacy, Security & Terms",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Text(
                    text = "1. Authorized Media & Copyright Compliance\n" +
                        "LinkFlow supports user-owned media, Creative Commons streams, public-domain archives, and authorized direct HTTPS streams. LinkFlow never circumvents DRM, paywalls, or private-account restrictions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1)
                )
                Text(
                    text = "2. SSRF & Network Security\n" +
                        "All pasted URLs undergo strict syntax, protocol, and SSRF validation. Loopback (127.0.0.1/localhost) and private RFC-1918 network ranges are blocked prior to any socket connection.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1)
                )
                Text(
                    text = "3. On-Device Data Privacy\n" +
                        "Your download queue, recent link analyses, and preferences are stored locally on your device using Room and Jetpack DataStore. Clipboard contents are read only when you explicitly tap Paste.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1)
                )
                Text(
                    text = "4. Software License (MIT License)\n" +
                        "Copyright (c) 2026 LinkFlow Contributors. Released under the MIT Open-Source License. Bundled AndroidX, Jetpack Compose, OkHttp, Room, and Coil libraries are licensed under the Apache License 2.0.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1)
                )

                Button(
                    onClick = onClose,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Understood", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun OpenSourceLicenseDialog(
    brandName: String,
    onDismiss: () -> Unit
) {
    val tokens = LocalLinkFlowTokens.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            LinkFlowLogoEmblem(size = 48.dp, animated = true)
        },
        title = {
            Text(
                text = "$brandName License & Open-Source Notices",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.height(360.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Image(
                        painter = painterResource(id = R.drawable.img_linkflow_hero_banner_1791571240767),
                        contentDescription = "$brandName License Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                }
                item {
                    Text(
                        text = "MIT License (SPDX-License-Identifier: MIT)\nCopyright (c) 2026 $brandName Contributors",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = tokens.accentCyan
                    )
                }
                item {
                    Text(
                        text = "Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the \"Software\"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:\n\n" +
                            "The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.\n\n" +
                            "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                item {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                }
                item {
                    Text(
                        text = "Third-Party Open-Source Software Notices",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = tokens.accentCyan
                    )
                }
                item {
                    Text(
                        text = "• AndroidX Core, Lifecycle, Activity & Navigation Compose — Apache License 2.0\n" +
                            "• Android Jetpack Room SQLite, DataStore & WorkManager — Apache License 2.0\n" +
                            "• Android MediaExtractor & MediaMuxer Demuxing Pipeline — Apache License 2.0\n" +
                            "• Square OkHttp 4 HTTP/2 & Byte-Range Client — Apache License 2.0\n" +
                            "• Coil Compose Image Loader — Apache License 2.0\n" +
                            "• JetBrains Kotlin Coroutines & Serialization — Apache License 2.0\n" +
                            "• Space Grotesk, Plus Jakarta Sans & JetBrains Mono — SIL Open Font License 1.1\n" +
                            "• JUnit 4 & Robolectric Test Suite — EPL 1.0 / MIT License",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                item {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                }
                item {
                    Text(
                        text = "Authorized Media & Copyright Compliance",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = tokens.accentCyan
                    )
                }
                item {
                    Text(
                        text = "$brandName is designed for user-owned media, Creative Commons streams, Public Domain collections, and authorized direct media links. Users are responsible for complying with applicable copyright laws and platform Terms of Service.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun AboutLinkFlowDialog(
    brandName: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            LinkFlowLogoEmblem(size = 52.dp, animated = true)
        },
        title = {
            Text(
                text = "$brandName v2.4.0",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.img_linkflow_hero_banner_1791571240767),
                    contentDescription = "$brandName Studio Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
                Text(
                    text = "Premium Liquid-Glass Video & Audio Downloader for Android.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "• Multi-Resolution MP4 & WebM (144p to 4K Ultra)\n" +
                        "• Genuine MP3 / M4A / WAV Audio & AAC Demuxing\n" +
                        "• YouTube, Instagram, Facebook, TikTok & Archive.org Support\n" +
                        "• Real-Time HTTP Byte-Stream Queue Engine with Resume\n" +
                        "• Licensed under the MIT Open-Source License",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
