package com.aifitnesscoach.feature.health

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import com.aifitnesscoach.R
import com.aifitnesscoach.core.ui.LayoutSpacing
import com.aifitnesscoach.core.database.UserPreferences
import com.aifitnesscoach.core.database.UserPreferencesRepository
import com.aifitnesscoach.core.database.WorkoutDao
import com.aifitnesscoach.integration.healthconnect.HealthConnectAvailability
import com.aifitnesscoach.integration.healthconnect.HealthConnectManager
import com.aifitnesscoach.integration.healthconnect.HealthDashboardMetrics
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun HealthScreen(
    preferences: UserPreferences,
    preferencesRepository: UserPreferencesRepository,
    workoutDao: WorkoutDao,
    healthConnectManager: HealthConnectManager,
    syncedWorkoutCount: Int,
    onBack: (() -> Unit)? = null,
    onOpenInsights: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var metrics by remember { mutableStateOf(HealthDashboardMetrics()) }
    var syncFeedback by remember { mutableStateOf<String?>(null) }

    val requestedPermissions = remember(
        preferences.hcWriteWorkoutsEnabled,
        preferences.hcReadWorkoutsEnabled,
        preferences.hcReadStepsEnabled,
        preferences.hcReadSleepEnabled,
        preferences.hcReadHeartRateEnabled
    ) {
        HealthConnectManager.permissionsForFeatures(
            writeWorkouts = preferences.hcWriteWorkoutsEnabled,
            readWorkouts = preferences.hcReadWorkoutsEnabled,
            readSteps = preferences.hcReadStepsEnabled,
            readSleep = preferences.hcReadSleepEnabled,
            readHeartRate = preferences.hcReadHeartRateEnabled
        )
    }

    suspend fun refreshMetrics() {
        metrics = healthConnectManager.readDashboardMetrics(
            readWorkouts = preferences.healthConnectEnabled && preferences.hcReadWorkoutsEnabled,
            readSteps = preferences.healthConnectEnabled && preferences.hcReadStepsEnabled,
            readSleep = preferences.healthConnectEnabled && preferences.hcReadSleepEnabled,
            readHeartRate = preferences.healthConnectEnabled && preferences.hcReadHeartRateEnabled
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) {
        scope.launch { refreshMetrics() }
    }

    LaunchedEffect(
        preferences.healthConnectEnabled,
        preferences.hcWriteWorkoutsEnabled,
        preferences.hcReadWorkoutsEnabled,
        preferences.hcReadStepsEnabled,
        preferences.hcReadSleepEnabled,
        preferences.hcReadHeartRateEnabled
    ) {
        refreshMetrics()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(LayoutSpacing.pagePadding),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (onBack != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.common_back))
                }
                Text(stringResource(R.string.nav_health), style = MaterialTheme.typography.headlineSmall)
            }
        }

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val statusText = when (metrics.availability) {
                    HealthConnectAvailability.AVAILABLE -> stringResource(R.string.health_status_available)
                    HealthConnectAvailability.NOT_INSTALLED -> stringResource(R.string.health_status_update_required)
                    HealthConnectAvailability.NOT_SUPPORTED -> stringResource(R.string.health_status_unavailable)
                }
                Text(text = statusText, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = stringResource(R.string.health_personal_apps_note),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FeatureSwitchRow(
                    label = stringResource(R.string.health_enable_toggle),
                    checked = preferences.healthConnectEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch { preferencesRepository.setHealthConnectEnabled(enabled) }
                    },
                    isTitle = true
                )

                if (preferences.healthConnectEnabled) {
                    HorizontalDivider()
                    Text(
                        text = stringResource(R.string.health_features_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.health_features_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FeatureSwitchRow(
                        label = stringResource(R.string.health_feat_write_workouts),
                        checked = preferences.hcWriteWorkoutsEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch { preferencesRepository.setHcWriteWorkouts(enabled) }
                        }
                    )
                    if (preferences.hcWriteWorkoutsEnabled) {
                        FeatureSwitchRow(
                            label = stringResource(R.string.health_feat_auto_sync),
                            checked = preferences.healthAutoSyncEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch { preferencesRepository.setHealthAutoSync(enabled) }
                            }
                        )
                    }
                    FeatureSwitchRow(
                        label = stringResource(R.string.health_feat_read_workouts),
                        checked = preferences.hcReadWorkoutsEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch { preferencesRepository.setHcReadWorkouts(enabled) }
                        }
                    )
                    FeatureSwitchRow(
                        label = stringResource(R.string.health_feat_read_steps),
                        checked = preferences.hcReadStepsEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch { preferencesRepository.setHcReadSteps(enabled) }
                        }
                    )
                    FeatureSwitchRow(
                        label = stringResource(R.string.health_feat_read_sleep),
                        checked = preferences.hcReadSleepEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch { preferencesRepository.setHcReadSleep(enabled) }
                        }
                    )
                    FeatureSwitchRow(
                        label = stringResource(R.string.health_feat_read_heart_rate),
                        checked = preferences.hcReadHeartRateEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch { preferencesRepository.setHcReadHeartRate(enabled) }
                        }
                    )

                    val grantedForEnabled = requestedPermissions.count { metrics.grantedPermissions.contains(it) }
                    Text(
                        text = if (requestedPermissions.isEmpty()) {
                            stringResource(R.string.health_select_feature_first)
                        } else {
                            stringResource(
                                R.string.health_permissions_summary,
                                grantedForEnabled,
                                requestedPermissions.size
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (metrics.availability == HealthConnectAvailability.AVAILABLE) {
                        val missingPermissions = requestedPermissions - metrics.grantedPermissions
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val toRequest = if (missingPermissions.isNotEmpty()) {
                                        missingPermissions
                                    } else {
                                        requestedPermissions
                                    }
                                    if (toRequest.isNotEmpty()) {
                                        permissionLauncher.launch(toRequest)
                                    }
                                },
                                enabled = requestedPermissions.isNotEmpty(),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.health_request_permissions))
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        val unsynced = workoutDao.getUnsyncedSessionsWithDetails()
                                        var successCount = 0
                                        unsynced.forEach { item ->
                                            val res = healthConnectManager.writeWorkoutSession(
                                                session = item.session,
                                                sets = item.sets
                                            )
                                            res.onSuccess { recordId ->
                                                workoutDao.markSessionSynced(item.session.id, recordId)
                                                successCount += 1
                                            }.onFailure { err ->
                                                syncFeedback = err.localizedMessage
                                            }
                                        }
                                        if (successCount > 0) {
                                            preferencesRepository.setLastHealthSync(System.currentTimeMillis())
                                            syncFeedback = context.getString(R.string.health_sync_result, successCount)
                                        }
                                        refreshMetrics()
                                    }
                                },
                                enabled = preferences.hcWriteWorkoutsEnabled,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.health_sync_now))
                            }
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.health_synced_count, syncedWorkoutCount),
                    style = MaterialTheme.typography.bodyMedium
                )
                val lastSyncStr = preferences.lastHealthSyncEpochMs?.let {
                    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it))
                } ?: stringResource(R.string.health_never_synced)
                Text(
                    text = stringResource(R.string.health_last_sync, lastSyncStr),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                syncFeedback?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                metrics.lastError?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        // Real Read Metrics (strictly from Health Connect when enabled & granted)
        MetricCard(
            title = stringResource(R.string.health_steps),
            value = metrics.todaySteps?.toString(),
            enabled = preferences.healthConnectEnabled && preferences.hcReadStepsEnabled
        )
        MetricCard(
            title = stringResource(R.string.health_sleep),
            value = metrics.latestSleepMinutes?.let { "${it / 60}h ${it % 60}m" },
            enabled = preferences.healthConnectEnabled && preferences.hcReadSleepEnabled
        )
        MetricCard(
            title = stringResource(R.string.health_heart_rate),
            value = metrics.latestHeartRateAvgBpm?.let { "$it bpm (24h avg)" },
            enabled = preferences.healthConnectEnabled && preferences.hcReadHeartRateEnabled
        )

        if (preferences.healthConnectEnabled && preferences.hcReadWorkoutsEnabled && metrics.recentImportedSessionTitles.isNotEmpty()) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.health_imported_sessions), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    metrics.recentImportedSessionTitles.forEach { title ->
                        Text("• $title", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        if (onOpenInsights != null) {
            OutlinedButton(
                onClick = onOpenInsights,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.ai_analyze_history_btn))
            }
        }
    }
}

@Composable
private fun FeatureSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isTitle: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isTitle) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String?,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = when {
                    !enabled -> stringResource(R.string.health_feature_disabled)
                    value != null -> value
                    else -> stringResource(R.string.health_no_data)
                },
                style = if (value != null && enabled) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.bodyMedium,
                color = if (value != null && enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
