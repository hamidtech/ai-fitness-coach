package com.aifitnesscoach.feature.settings

import android.Manifest
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.aifitnesscoach.core.ui.LayoutSpacing
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.aifitnesscoach.R
import com.aifitnesscoach.core.ai.analysis.SavedInsightRepository
import com.aifitnesscoach.core.ai.local.LocalModelRepository
import com.aifitnesscoach.core.database.AppColorPalette
import com.aifitnesscoach.core.database.AppLanguage
import com.aifitnesscoach.core.database.AppThemeMode
import com.aifitnesscoach.core.database.CameraLensPreference
import com.aifitnesscoach.core.database.MeasurementUnit
import com.aifitnesscoach.core.database.UserPreferences
import com.aifitnesscoach.core.database.UserPreferencesRepository
import com.aifitnesscoach.core.database.WorkoutDao
import com.aifitnesscoach.core.database.WorkoutExportManager
import com.aifitnesscoach.core.database.WorkoutSessionWithDetails
import com.aifitnesscoach.core.localization.LocaleManager
import com.aifitnesscoach.core.media.ExerciseAssetRepository
import com.aifitnesscoach.core.security.SecretStore
import kotlinx.coroutines.launch
import java.util.Locale

enum class SettingsPage(val title: Int, val description: Int) {
    PROFILE(R.string.profile_title, R.string.settings_profile_description),
    APPEARANCE(R.string.settings_appearance_language, R.string.settings_appearance_description),
    WORKOUT(R.string.settings_section_b_workout, R.string.settings_workout_description),
    NOTIFICATIONS(R.string.settings_notifications_master, R.string.settings_notifications_description),
    HEALTH(R.string.nav_health, R.string.settings_health_disabled_sub),
    AI(R.string.settings_section_d_ai, R.string.settings_insights_sub),
    STORAGE(R.string.settings_section_e_storage, R.string.settings_storage_description),
    PRIVACY(R.string.settings_section_f_privacy, R.string.settings_privacy_description)
}

@Composable
fun SettingsHome(onOpenPage: (SettingsPage) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(LayoutSpacing.screen),
        verticalArrangement = Arrangement.spacedBy(LayoutSpacing.small)
    ) {
        Text(stringResource(R.string.nav_settings), style = MaterialTheme.typography.headlineMedium)
        SettingsPage.entries.forEach { page ->
            SettingsNavCard(stringResource(page.title), stringResource(page.description)) { onOpenPage(page) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    page: SettingsPage,
    onBack: () -> Unit,
    preferences: UserPreferences,
    preferencesRepository: UserPreferencesRepository,
    workoutDao: WorkoutDao,
    sessions: List<WorkoutSessionWithDetails>,
    exerciseAssetRepository: ExerciseAssetRepository,
    localModelRepository: LocalModelRepository,
    savedInsightRepository: SavedInsightRepository,
    secretStore: SecretStore,
    onOpenPersonalProfile: () -> Unit,
    onOpenThemePreferences: () -> Unit,
    onOpenHealthConnect: () -> Unit,
    onOpenMediaLibrary: () -> Unit,
    onOpenLocalModels: () -> Unit,
    onOpenExternalProviders: () -> Unit,
    onOpenScheduledAnalysis: () -> Unit,
    onOpenInsights: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var exportStatus by remember { mutableStateOf<String?>(null) }
    var mediaBytes by remember { mutableLongStateOf(0L) }
    var modelBytes by remember { mutableLongStateOf(0L) }

    LaunchedEffect(page) {
        if (page == SettingsPage.STORAGE || page == SettingsPage.PRIVACY) {
            mediaBytes = exerciseAssetRepository.computeStorageBytes()
        }
        if (page == SettingsPage.AI || page == SettingsPage.PRIVACY) {
            modelBytes = localModelRepository.computeModelsStorageBytes()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        scope.launch {
            preferencesRepository.setNotificationPreferences(enabled = granted)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(LayoutSpacing.pagePadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.common_back))
            }
            Text(stringResource(page.title), style = MaterialTheme.typography.headlineSmall)
        }

        when (page) {
            SettingsPage.PROFILE -> {
                val profileSummary = preferences.profile.name.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.settings_profile_unset_sub)
                SettingsNavCard(
                    title = stringResource(R.string.profile_title),
                    subtitle = profileSummary,
                    onClick = onOpenPersonalProfile
                )

            }
            SettingsPage.APPEARANCE -> {
                val appearanceModeText = when (preferences.themeMode) {
                    AppThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                    AppThemeMode.LIGHT -> stringResource(R.string.theme_light)
                    AppThemeMode.DARK -> stringResource(R.string.theme_dark)
                }
                val paletteText = if (preferences.useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    stringResource(R.string.theme_dynamic_color_title)
                } else {
                    when (preferences.colorPalette) {
                        AppColorPalette.AUTO -> stringResource(R.string.palette_auto)
                        AppColorPalette.ATHLETIC_COBALT -> stringResource(R.string.palette_cobalt)
                        AppColorPalette.TEAL -> stringResource(R.string.palette_teal)
                        AppColorPalette.VIOLET -> stringResource(R.string.palette_violet)
                        AppColorPalette.NEUTRAL -> stringResource(R.string.palette_neutral)
                    }
                }
                SettingsNavCard(
                    title = stringResource(R.string.theme_screen_title),
                    subtitle = "$appearanceModeText · $paletteText",
                    onClick = onOpenThemePreferences
                )

                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(LayoutSpacing.small)) {
                            listOf(
                                AppLanguage.SYSTEM to stringResource(R.string.lang_system),
                                AppLanguage.EN to stringResource(R.string.lang_en),
                                AppLanguage.FA to stringResource(R.string.lang_fa)
                            ).forEach { (lang, label) ->
                                FilterChip(
                                    selected = preferences.language == lang,
                                    onClick = {
                                        scope.launch {
                                            preferencesRepository.setLanguage(lang)
                                            LocaleManager.applyLanguage(lang)
                                        }
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }

                        HorizontalDivider()
                        Text(stringResource(R.string.settings_units), style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(LayoutSpacing.small)) {
                            FilterChip(
                                selected = preferences.measurementUnit == MeasurementUnit.METRIC,
                                onClick = { scope.launch { preferencesRepository.setMeasurementUnit(MeasurementUnit.METRIC) } },
                                label = { Text(stringResource(R.string.units_metric)) }
                            )
                            FilterChip(
                                selected = preferences.measurementUnit == MeasurementUnit.IMPERIAL,
                                onClick = { scope.launch { preferencesRepository.setMeasurementUnit(MeasurementUnit.IMPERIAL) } },
                                label = { Text(stringResource(R.string.units_imperial)) }
                            )
                        }
                    }
                }

            }
            SettingsPage.WORKOUT -> {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.settings_camera), style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(LayoutSpacing.small)) {
                            FilterChip(
                                selected = preferences.cameraLens == CameraLensPreference.FRONT,
                                onClick = { scope.launch { preferencesRepository.setCameraLens(CameraLensPreference.FRONT) } },
                                label = { Text(stringResource(R.string.camera_front)) }
                            )
                            FilterChip(
                                selected = preferences.cameraLens == CameraLensPreference.BACK,
                                onClick = { scope.launch { preferencesRepository.setCameraLens(CameraLensPreference.BACK) } },
                                label = { Text(stringResource(R.string.camera_back)) }
                            )
                        }

                        ToggleRow(
                            label = stringResource(R.string.settings_skeleton_overlay),
                            checked = preferences.skeletonOverlayEnabled,
                            onCheckedChange = { scope.launch { preferencesRepository.setSkeletonOverlay(it) } }
                        )
                        ToggleRow(
                            label = stringResource(R.string.settings_voice_feedback),
                            checked = preferences.voiceFeedbackEnabled,
                            onCheckedChange = { scope.launch { preferencesRepository.setVoiceFeedback(it) } }
                        )

                    }
                }
            }
            SettingsPage.NOTIFICATIONS -> {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(LayoutSpacing.screen), verticalArrangement = Arrangement.spacedBy(LayoutSpacing.small)) {
                        ToggleRow(
                            label = stringResource(R.string.settings_notifications_master),
                            checked = preferences.notificationsEnabled,
                            onCheckedChange = { enable ->
                                if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (granted) {
                                        scope.launch { preferencesRepository.setNotificationPreferences(enabled = true) }
                                    } else {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    scope.launch { preferencesRepository.setNotificationPreferences(enabled = enable) }
                                }
                            }
                        )
                        if (preferences.notificationsEnabled) {
                            ToggleRow(
                                label = stringResource(R.string.settings_notif_analysis),
                                checked = preferences.notifyAnalysisComplete,
                                onCheckedChange = { scope.launch { preferencesRepository.setNotificationPreferences(analysisComplete = it) } }
                            )
                            ToggleRow(
                                label = stringResource(R.string.settings_notif_downloads),
                                checked = preferences.notifyDownloads,
                                onCheckedChange = { scope.launch { preferencesRepository.setNotificationPreferences(downloads = it) } }
                            )
                            ToggleRow(
                                label = stringResource(R.string.settings_notif_reminders),
                                checked = preferences.notifyWorkoutReminder,
                                onCheckedChange = { scope.launch { preferencesRepository.setNotificationPreferences(workoutReminder = it) } }
                            )
                        }
                    }
                }

            }
            SettingsPage.HEALTH -> {
                SettingsNavCard(
                    title = stringResource(R.string.nav_health),
                    subtitle = if (preferences.healthConnectEnabled) {
                        stringResource(R.string.settings_health_enabled_sub)
                    } else {
                        stringResource(R.string.settings_health_disabled_sub)
                    },
                    onClick = onOpenHealthConnect
                )

            }
            SettingsPage.AI -> {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ToggleRow(
                            label = stringResource(R.string.settings_allow_external_ai_sharing),
                            checked = preferences.allowExternalAiSharing,
                            onCheckedChange = {
                                scope.launch { preferencesRepository.setAiConfiguration(allowExternalSharing = it) }
                            }
                        )
                    }
                }
                SettingsNavCard(
                    title = stringResource(R.string.local_models_title),
                    subtitle = stringResource(
                        R.string.settings_local_ai_sub,
                        preferences.activeLocalModelId ?: stringResource(R.string.common_none),
                        formatMb(modelBytes)
                    ),
                    onClick = onOpenLocalModels
                )
                SettingsNavCard(
                    title = stringResource(R.string.external_ai_title),
                    subtitle = stringResource(R.string.settings_external_ai_sub),
                    onClick = onOpenExternalProviders
                )
                SettingsNavCard(
                    title = stringResource(R.string.schedule_title),
                    subtitle = if (preferences.scheduledAnalysisEnabled) {
                        stringResource(
                            R.string.settings_schedule_active_sub,
                            String.format(Locale.US, "%02d:%02d", preferences.scheduleHour, preferences.scheduleMinute)
                        )
                    } else {
                        stringResource(R.string.settings_schedule_off_sub)
                    },
                    onClick = onOpenScheduledAnalysis
                )
                SettingsNavCard(
                    title = stringResource(R.string.ai_coach_insights_title),
                    subtitle = stringResource(R.string.settings_insights_sub),
                    onClick = onOpenInsights
                )

            }
            SettingsPage.STORAGE -> {
                SettingsNavCard(
                    title = stringResource(R.string.media_library_title),
                    subtitle = stringResource(R.string.settings_media_sub, formatMb(mediaBytes)),
                    onClick = onOpenMediaLibrary
                )

            }
            SettingsPage.PRIVACY -> {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(
                                R.string.settings_storage_breakdown,
                                sessions.size,
                                formatMb(mediaBytes),
                                formatMb(modelBytes)
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        ToggleRow(
                            label = stringResource(R.string.settings_dev_metrics),
                            checked = preferences.developerMetricsEnabled,
                            onCheckedChange = { scope.launch { preferencesRepository.setDeveloperMetrics(it) } }
                        )

                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    val shareIntent = WorkoutExportManager.writeAndCreateShareIntent(
                                        context = context,
                                        preferences = preferences,
                                        sessions = sessions
                                    )
                                    context.startActivity(
                                        Intent.createChooser(
                                            shareIntent,
                                            context.getString(R.string.settings_export_workouts)
                                        )
                                    )
                                    exportStatus = context.getString(R.string.settings_export_ready)
                                }.onFailure { err ->
                                    exportStatus = err.localizedMessage
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.settings_export_workouts))
                        }

                        exportStatus?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }

                        OutlinedButton(
                            onClick = { showPrivacyDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.settings_privacy))
                        }

                        Button(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.settings_delete_data))
                        }
                    }
            }
        }
        }
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text(stringResource(R.string.settings_privacy)) },
            text = { Text(stringResource(R.string.privacy_body)) },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text(stringResource(R.string.common_ok)) }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.settings_delete_data)) },
            text = { Text(stringResource(R.string.settings_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        workoutDao.deleteAllWorkouts()
                        savedInsightRepository.deleteAll()
                        exerciseAssetRepository.deleteAllAssets()
                        secretStore.clearAllSecrets()
                        preferencesRepository.clearAll()
                        mediaBytes = 0L
                        modelBytes = localModelRepository.computeModelsStorageBytes()
                        showDeleteDialog = false
                    }
                }) {
                    Text(stringResource(R.string.settings_delete_data), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.workout_discard))
                }
            }
        )
    }
}

@Composable
private fun SettingsNavCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(shape = RoundedCornerShape(16.dp), onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun formatMb(bytes: Long): String {
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return String.format(Locale.US, "%.1f MB", mb)
}
