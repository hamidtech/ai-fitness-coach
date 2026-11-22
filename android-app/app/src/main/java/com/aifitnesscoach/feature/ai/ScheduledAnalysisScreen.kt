package com.aifitnesscoach.feature.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aifitnesscoach.R
import com.aifitnesscoach.core.ai.schedule.AnalysisScheduler
import com.aifitnesscoach.core.database.AiExecutionMode
import com.aifitnesscoach.core.database.AnalysisFrequency
import com.aifitnesscoach.core.database.AnalysisRangeScope
import com.aifitnesscoach.core.database.UserPreferences
import com.aifitnesscoach.core.database.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScheduledAnalysisScreen(
    preferences: UserPreferences,
    preferencesRepository: UserPreferencesRepository,
    scheduler: AnalysisScheduler,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var hourText by remember(preferences.scheduleHour) { mutableStateOf(preferences.scheduleHour.toString()) }
    var minuteText by remember(preferences.scheduleMinute) { mutableStateOf(preferences.scheduleMinute.toString()) }
    var runQueuedMessage by remember { mutableStateOf(false) }

    fun updateAndSync(block: suspend () -> Unit) {
        scope.launch {
            block()
            val latest = preferencesRepository.preferencesFlow.first()
            scheduler.syncSchedule(latest)
        }
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.common_back))
            }
            Text(
                text = stringResource(R.string.schedule_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.schedule_enable_toggle), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Switch(
                            checked = preferences.scheduledAnalysisEnabled,
                            onCheckedChange = { enabled ->
                                updateAndSync { preferencesRepository.setScheduleConfiguration(enabled = enabled) }
                            }
                        )
                    }
                    Text(
                        text = stringResource(R.string.schedule_workmanager_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(stringResource(R.string.schedule_time_window_label), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = hourText,
                            onValueChange = { v ->
                                hourText = v.filter { it.isDigit() }.take(2)
                                hourText.toIntOrNull()?.let { h ->
                                    if (h in 0..23) updateAndSync { preferencesRepository.setScheduleConfiguration(hour = h) }
                                }
                            },
                            label = { Text(stringResource(R.string.schedule_hour_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = minuteText,
                            onValueChange = { v ->
                                minuteText = v.filter { it.isDigit() }.take(2)
                                minuteText.toIntOrNull()?.let { m ->
                                    if (m in 0..59) updateAndSync { preferencesRepository.setScheduleConfiguration(minute = m) }
                                }
                            },
                            label = { Text(stringResource(R.string.schedule_minute_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(stringResource(R.string.schedule_frequency_label), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = preferences.scheduleFrequency == AnalysisFrequency.DAILY,
                            onClick = { updateAndSync { preferencesRepository.setScheduleConfiguration(frequency = AnalysisFrequency.DAILY) } },
                            label = { Text(stringResource(R.string.schedule_freq_daily)) }
                        )
                        FilterChip(
                            selected = preferences.scheduleFrequency == AnalysisFrequency.WEEKLY,
                            onClick = { updateAndSync { preferencesRepository.setScheduleConfiguration(frequency = AnalysisFrequency.WEEKLY) } },
                            label = { Text(stringResource(R.string.schedule_freq_weekly)) }
                        )
                    }

                    Text(stringResource(R.string.schedule_scope_label), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            AnalysisRangeScope.SINCE_LAST_ANALYSIS to R.string.schedule_scope_since_last,
                            AnalysisRangeScope.LAST_DAY to R.string.schedule_scope_last_day,
                            AnalysisRangeScope.LAST_7_DAYS to R.string.schedule_scope_last_7_days
                        ).forEach { (sc, res) ->
                            FilterChip(
                                selected = preferences.scheduleScope == sc,
                                onClick = { updateAndSync { preferencesRepository.setScheduleConfiguration(scope = sc) } },
                                label = { Text(stringResource(res)) }
                            )
                        }
                    }

                    Text(stringResource(R.string.ai_provider_mode_label), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            AiExecutionMode.RULE_BASED_ONLY to R.string.ai_mode_rule_based,
                            AiExecutionMode.LOCAL_LLM to R.string.ai_mode_local_llm,
                            AiExecutionMode.EXTERNAL_API to R.string.ai_mode_external_api
                        ).forEach { (mode, res) ->
                            FilterChip(
                                selected = preferences.scheduleProviderMode == mode,
                                onClick = { updateAndSync { preferencesRepository.setScheduleConfiguration(providerMode = mode) } },
                                label = { Text(stringResource(res)) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.common_wifi_only), modifier = Modifier.weight(1f))
                        Switch(
                            checked = preferences.scheduleWifiOnly,
                            onCheckedChange = { updateAndSync { preferencesRepository.setScheduleConfiguration(wifiOnly = it) } }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.schedule_charging_only), modifier = Modifier.weight(1f))
                        Switch(
                            checked = preferences.scheduleChargingRequired,
                            onCheckedChange = { updateAndSync { preferencesRepository.setScheduleConfiguration(chargingRequired = it) } }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.schedule_notify_toggle), modifier = Modifier.weight(1f))
                        Switch(
                            checked = preferences.scheduleNotifyOnComplete,
                            onCheckedChange = { updateAndSync { preferencesRepository.setScheduleConfiguration(notifyOnComplete = it) } }
                        )
                    }
                }
            }

            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.schedule_status_header), style = MaterialTheme.typography.titleMedium)
                    val lastRunStr = preferences.lastScheduledRunMs?.let {
                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it))
                    } ?: stringResource(R.string.health_never_synced)
                    Text(stringResource(R.string.schedule_last_run, lastRunStr, preferences.lastAnalyzedCount))
                    preferences.lastScheduledError?.let { err ->
                        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = {
                            scheduler.runNow()
                            runQueuedMessage = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.schedule_run_now_btn))
                    }
                    if (runQueuedMessage) {
                        Text(
                            text = stringResource(R.string.schedule_run_queued_msg),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
