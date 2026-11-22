package com.aifitnesscoach.feature.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aifitnesscoach.R
import com.aifitnesscoach.core.ui.LayoutSpacing
import com.aifitnesscoach.core.ai.analysis.AIAnalysisCoordinator
import com.aifitnesscoach.core.ai.analysis.AnalysisRequestOutcome
import com.aifitnesscoach.core.ai.analysis.AnalysisType
import com.aifitnesscoach.core.ai.analysis.SavedInsightRepository
import com.aifitnesscoach.core.database.AiExecutionMode
import com.aifitnesscoach.core.database.UserPreferences
import com.aifitnesscoach.core.database.UserPreferencesRepository
import com.aifitnesscoach.core.database.WorkoutSessionWithDetails
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AIAnalysisScreen(
    isPersian: Boolean,
    preferences: UserPreferences,
    preferencesRepository: UserPreferencesRepository,
    sessions: List<WorkoutSessionWithDetails>,
    focusSessionId: Long? = null,
    coordinator: AIAnalysisCoordinator,
    insightRepository: SavedInsightRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val savedInsights by insightRepository.observeAllInsights()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedMode by remember(preferences.aiExecutionMode) { mutableStateOf(preferences.aiExecutionMode) }
    var useSingleFocus by remember(focusSessionId) { mutableStateOf(focusSessionId != null) }
    var questionText by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    var showExternalConsentDialog by remember { mutableStateOf(false) }
    var lastOutcome by remember { mutableStateOf<AnalysisRequestOutcome?>(null) }

    val targetSessions = remember(sessions, useSingleFocus, focusSessionId) {
        if (useSingleFocus && focusSessionId != null) {
            sessions.filter { it.session.id == focusSessionId }
        } else {
            val cutoff = System.currentTimeMillis() - 7L * 24L * 60L * 60L * 1000L
            val recent = sessions.filter { it.session.startedAtEpochMs >= cutoff }
            recent.ifEmpty { sessions.take(5) }
        }
    }

    val rangeLabel = if (useSingleFocus && focusSessionId != null) {
        targetSessions.firstOrNull()?.let { if (isPersian) it.session.titleFa else it.session.titleEn }
            ?: stringResource(R.string.ai_range_selected_workout)
    } else {
        stringResource(R.string.ai_range_last_7_days)
    }

    fun executeAnalysis(modeToUse: AiExecutionMode, consentConfirmed: Boolean) {
        scope.launch {
            isRunning = true
            val type = if (questionText.isNotBlank()) AnalysisType.QA_FOLLOWUP
            else if (useSingleFocus) AnalysisType.MANUAL_WORKOUT
            else AnalysisType.MANUAL_RANGE

            lastOutcome = coordinator.runAnalysis(
                sessions = targetSessions,
                preferences = preferences,
                modeOverride = modeToUse,
                analysisType = type,
                isPersian = isPersian,
                sourceRangeLabel = rangeLabel,
                userQuestion = questionText,
                externalSharingConfirmedForThisCall = consentConfirmed,
                saveOnSuccess = true
            )
            isRunning = false
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
                text = stringResource(R.string.ai_coach_insights_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(LayoutSpacing.pagePadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.ai_provider_mode_label), style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                AiExecutionMode.RULE_BASED_ONLY to R.string.ai_mode_rule_based,
                                AiExecutionMode.LOCAL_LLM to R.string.ai_mode_local_llm,
                                AiExecutionMode.EXTERNAL_API to R.string.ai_mode_external_api
                            ).forEach { (m, labelRes) ->
                                FilterChip(
                                    selected = selectedMode == m,
                                    onClick = {
                                        selectedMode = m
                                        scope.launch { preferencesRepository.setAiConfiguration(mode = m) }
                                    },
                                    label = { Text(stringResource(labelRes)) }
                                )
                            }
                        }

                        if (focusSessionId != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = useSingleFocus,
                                    onClick = { useSingleFocus = true },
                                    label = { Text(stringResource(R.string.ai_range_selected_workout)) }
                                )
                                FilterChip(
                                    selected = !useSingleFocus,
                                    onClick = { useSingleFocus = false },
                                    label = { Text(stringResource(R.string.ai_range_last_7_days)) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = questionText,
                            onValueChange = { questionText = it },
                            label = { Text(stringResource(R.string.ai_question_label)) },
                            placeholder = { Text(stringResource(R.string.ai_question_hint)) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (selectedMode == AiExecutionMode.EXTERNAL_API && !preferences.allowExternalAiSharing) {
                                    showExternalConsentDialog = true
                                } else {
                                    executeAnalysis(selectedMode, preferences.allowExternalAiSharing)
                                }
                            },
                            enabled = !isRunning && targetSessions.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (isRunning) stringResource(R.string.ai_analyzing_now)
                                else stringResource(R.string.ai_run_analysis_btn)
                            )
                        }

                        if (targetSessions.isEmpty()) {
                            Text(
                                text = stringResource(R.string.home_ai_insight_empty_no_workouts),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Honest Error & Explicit Fallback Offer if Local LLM or External API fails
            lastOutcome?.failedExternalOrLocalReason?.let { err ->
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(err, color = MaterialTheme.colorScheme.onErrorContainer)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (lastOutcome?.canFallbackToLocalLlm == true) {
                                    OutlinedButton(onClick = { executeAnalysis(AiExecutionMode.LOCAL_LLM, false) }) {
                                        Text(stringResource(R.string.ai_fallback_local_llm))
                                    }
                                }
                                if (lastOutcome?.canFallbackToRuleBased == true) {
                                    Button(onClick = {
                                        scope.launch {
                                            coordinator.saveRuleBasedFallback(
                                                sessions = targetSessions,
                                                preferences = preferences,
                                                analysisType = AnalysisType.MANUAL_RANGE,
                                                isPersian = isPersian,
                                                sourceRangeLabel = rangeLabel
                                            )
                                            lastOutcome = null
                                        }
                                    }) {
                                        Text(stringResource(R.string.ai_fallback_rule_based))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.ai_saved_insights_header),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (savedInsights.isEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Text(
                            text = stringResource(R.string.ai_saved_insights_empty),
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(savedInsights, key = { it.id }) { insight ->
                    val issues = remember(insight.issuesJson) {
                        insightRepository.decodeIssues(insight.issuesJson)
                    }
                    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(insight.summaryTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            val dateStr = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                .format(Date(insight.timestampMs))
                            Text(
                                text = "$dateStr · ${insight.modelIdentifier}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(insight.bodyText, style = MaterialTheme.typography.bodyMedium)
                            issues.forEach { issue ->
                                Text(
                                    text = "• ${issue.detail}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { scope.launch { insightRepository.deleteById(insight.id) } }) {
                                    Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExternalConsentDialog) {
        AlertDialog(
            onDismissRequest = { showExternalConsentDialog = false },
            title = { Text(stringResource(R.string.ai_external_consent_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.ai_external_consent_body,
                        targetSessions.size,
                        rangeLabel
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showExternalConsentDialog = false
                    executeAnalysis(AiExecutionMode.EXTERNAL_API, consentConfirmed = true)
                }) {
                    Text(stringResource(R.string.ai_external_consent_send_once))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExternalConsentDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}
