package com.aifitnesscoach.core.ai.analysis

import com.aifitnesscoach.core.ai.external.AIProviderRepository
import com.aifitnesscoach.core.ai.local.LocalLlmRuntime
import com.aifitnesscoach.core.ai.local.LocalModelCatalog
import com.aifitnesscoach.core.ai.local.LocalModelRepository
import com.aifitnesscoach.core.database.AiExecutionMode
import com.aifitnesscoach.core.database.SavedInsightEntity
import com.aifitnesscoach.core.database.UserPreferences
import com.aifitnesscoach.core.database.WorkoutSessionWithDetails
import com.aifitnesscoach.core.exercise.definitions.ExerciseCatalog
import com.google.gson.Gson
import com.aifitnesscoach.core.personalization.PersonalizedGreeting
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Coordinates workout summary & form-question analysis across:
 * 1. Rule-Based deterministic engine (always offline, zero downloads required)
 * 2. On-device LiteRT-LM Local LLM (requires checksum + compatibility verified model)
 * 3. External OpenAI-compatible AI Provider (requires explicit user sharing opt-in)
 *
 * Strictly builds structured JSON from real recorded workouts and optional Health Connect
 * metrics; never invents sessions, weights, or biometrics.
 */
class AIAnalysisCoordinator(
    private val localModelRepository: LocalModelRepository,
    private val localInferenceEngine: LocalLlmRuntime,
    private val externalProviderRepository: AIProviderRepository,
    private val savedInsightRepository: SavedInsightRepository
) {
    private val gson = Gson()
    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.systemDefault())

    fun buildStructuredPayload(
        sessions: List<WorkoutSessionWithDetails>,
        preferences: UserPreferences,
        analysisType: AnalysisType,
        isPersian: Boolean,
        sourceRangeLabel: String,
        healthMetrics: AnalyzedHealthMetrics? = null,
        userQuestion: String? = null
    ): WorkoutAnalysisPayload {
        val workouts = sessions.map { item ->
            val breakdowns = item.sets.groupBy { it.exerciseId }.map { (exId, exSets) ->
                val ex = ExerciseCatalog.findById(exId)
                val name = if (isPersian) ex?.nameFa ?: exId else ex?.nameEn ?: exId
                val equipNote = exSets.firstOrNull()?.let { s ->
                    when {
                        s.weightValue != null -> "${s.weightValue} ${s.weightUnit ?: "kg"} (${s.weightLoadMode ?: ""})"
                        s.bandType != null -> "${s.bandType}${s.bandResistanceLevel?.let { " [$it]" } ?: ""}"
                        else -> null
                    }
                }
                AnalyzedExerciseBreakdown(
                    exerciseId = exId,
                    exerciseName = name,
                    category = exSets.firstOrNull()?.categoryName ?: "",
                    setsCompleted = exSets.size,
                    totalReps = exSets.sumOf { it.completedReps },
                    totalHoldSeconds = exSets.sumOf { it.holdDurationMs } / 1000L,
                    equipmentNote = equipNote
                )
            }
            val issues = item.observations.map { obs ->
                if (isPersian) obs.detailFa else obs.detailEn
            }.distinct()

            AnalyzedWorkoutSummary(
                sessionId = item.session.id,
                dateIso = isoFormatter.format(Instant.ofEpochMilli(item.session.startedAtEpochMs)),
                title = if (isPersian) item.session.titleFa else item.session.titleEn,
                durationMinutes = (item.session.totalDurationMs / 60_000L).coerceAtLeast(1L).toInt(),
                exercises = breakdowns,
                observedFormIssues = issues
            )
        }

        val cleanHealth = healthMetrics?.takeIf {
            it.todaySteps != null || it.recentSleepMinutes != null || it.recentHeartRateAvgBpm != null
        }

        return WorkoutAnalysisPayload(
            schemaVersion = 1,
            generatedAtEpochMs = System.currentTimeMillis(),
            analysisType = analysisType.name,
            locale = if (isPersian) "fa" else "en",
            sourceRangeLabel = sourceRangeLabel,
            athleteContext = AnalyzedAthleteContext(
                athleteDisplayName = PersonalizedGreeting.sanitizeName(preferences.profile.name),
                fitnessGoal = if (preferences.profile.customGoalLabel.isNotBlank()) {
                    preferences.profile.customGoalLabel
                } else {
                    preferences.profile.fitnessGoal.name
                },
                experienceLevel = preferences.profile.experienceLevel.name,
                measurementUnit = preferences.measurementUnit.name
            ),
            workouts = workouts,
            optionalHealthMetrics = cleanHealth,
            userQuestion = userQuestion?.trim()?.takeIf { it.isNotEmpty() }
        )
    }

    suspend fun runAnalysis(
        sessions: List<WorkoutSessionWithDetails>,
        preferences: UserPreferences,
        modeOverride: AiExecutionMode? = null,
        analysisType: AnalysisType = AnalysisType.MANUAL_RANGE,
        isPersian: Boolean,
        sourceRangeLabel: String,
        healthMetrics: AnalyzedHealthMetrics? = null,
        userQuestion: String? = null,
        externalSharingConfirmedForThisCall: Boolean = false,
        saveOnSuccess: Boolean = true
    ): AnalysisRequestOutcome {
        val selectedMode = modeOverride ?: preferences.aiExecutionMode
        val payload = buildStructuredPayload(
            sessions = sessions,
            preferences = preferences,
            analysisType = analysisType,
            isPersian = isPersian,
            sourceRangeLabel = sourceRangeLabel,
            healthMetrics = healthMetrics,
            userQuestion = userQuestion
        )
        val payloadJson = gson.toJson(payload)
        val workoutIdsCsv = sessions.joinToString(",") { it.session.id.toString() }

        val systemPrompt = buildSystemPrompt(isPersian)
        val userPrompt = buildUserPrompt(payloadJson, userQuestion, isPersian)

        val verifiedLocal = localModelRepository.getVerifiedExecutableModel(preferences.activeLocalModelId)

        return when (selectedMode) {
            AiExecutionMode.RULE_BASED_ONLY -> {
                val insight = buildRuleBasedInsight(
                    payload = payload,
                    analysisType = analysisType,
                    workoutIdsCsv = workoutIdsCsv,
                    isPersian = isPersian,
                    fallbackStatus = false
                )
                if (saveOnSuccess) savedInsightRepository.save(insight)
                AnalysisRequestOutcome(
                    insight = insight,
                    usedProviderName = if (isPersian) "تحلیلگر قانون‌محور آفلاین" else "Offline Rule-Based Coach"
                )
            }

            AiExecutionMode.LOCAL_LLM -> {
                if (verifiedLocal == null) {
                    val reason = if (isPersian) {
                        "هیچ مدل آفلاین تأییدشده‌ای انتخاب نشده است. ابتدا در تنظیمات مدل را دانلود و تست کنید."
                    } else {
                        "No verified local model is active. Download a model and run Test Model first."
                    }
                    val insight = buildFailedInsight(
                        analysisType = analysisType,
                        providerType = AiExecutionMode.LOCAL_LLM.name,
                        modelIdentifier = preferences.activeLocalModelId ?: "none",
                        workoutIdsCsv = workoutIdsCsv,
                        sourceRangeLabel = sourceRangeLabel,
                        isPersian = isPersian,
                        errorMessage = reason
                    )
                    return AnalysisRequestOutcome(
                        insight = insight,
                        usedProviderName = preferences.activeLocalModelId ?: "Local LLM",
                        failedExternalOrLocalReason = reason,
                        canFallbackToLocalLlm = false,
                        canFallbackToRuleBased = true
                    )
                }

                val spec = LocalModelCatalog.findById(verifiedLocal.modelId)
                val preferGpu = spec?.backendHint?.contains("GPU", ignoreCase = true) == true
                val res = localInferenceEngine.runCompletion(
                    modelPath = verifiedLocal.localPath,
                    systemPrompt = systemPrompt,
                    userPrompt = userPrompt,
                    preferGpu = preferGpu
                )
                res.fold(
                    onSuccess = { completion ->
                        val insight = SavedInsightEntity(
                            id = "ins_${UUID.randomUUID()}",
                            timestampMs = System.currentTimeMillis(),
                            analysisType = analysisType.name,
                            providerType = AiExecutionMode.LOCAL_LLM.name,
                            modelIdentifier = "${spec?.displayName ?: verifiedLocal.modelId} (${completion.backendUsed})",
                            relatedWorkoutIdsCsv = workoutIdsCsv,
                            sourceRangeLabel = sourceRangeLabel,
                            status = InsightStatus.COMPLETED.name,
                            summaryTitle = if (isPersian) "تحلیل هوش مصنوعی آفلاین" else "On-Device AI Insight",
                            bodyText = completion.text,
                            issuesJson = savedInsightRepository.encodeIssues(extractIssuesFromWorkouts(payload, isPersian)),
                            errorMessage = null
                        )
                        if (saveOnSuccess) savedInsightRepository.save(insight)
                        AnalysisRequestOutcome(
                            insight = insight,
                            usedProviderName = spec?.displayName ?: verifiedLocal.modelId
                        )
                    },
                    onFailure = { err ->
                        val reason = err.localizedMessage ?: "Local inference failed"
                        val insight = buildFailedInsight(
                            analysisType = analysisType,
                            providerType = AiExecutionMode.LOCAL_LLM.name,
                            modelIdentifier = spec?.displayName ?: verifiedLocal.modelId,
                            workoutIdsCsv = workoutIdsCsv,
                            sourceRangeLabel = sourceRangeLabel,
                            isPersian = isPersian,
                            errorMessage = reason
                        )
                        AnalysisRequestOutcome(
                            insight = insight,
                            usedProviderName = spec?.displayName ?: verifiedLocal.modelId,
                            failedExternalOrLocalReason = reason,
                            canFallbackToLocalLlm = false,
                            canFallbackToRuleBased = true
                        )
                    }
                )
            }

            AiExecutionMode.EXTERNAL_API -> {
                val allowed = preferences.allowExternalAiSharing || externalSharingConfirmedForThisCall
                if (!allowed) {
                    val reason = if (isPersian) {
                        "اشتراک‌گذاری خلاصه تمرین با سرویس خارجی هنوز تأیید نشده است."
                    } else {
                        "External AI workout sharing is not enabled."
                    }
                    val insight = buildFailedInsight(
                        analysisType = analysisType,
                        providerType = AiExecutionMode.EXTERNAL_API.name,
                        modelIdentifier = "external",
                        workoutIdsCsv = workoutIdsCsv,
                        sourceRangeLabel = sourceRangeLabel,
                        isPersian = isPersian,
                        errorMessage = reason
                    )
                    return AnalysisRequestOutcome(
                        insight = insight,
                        usedProviderName = "External AI",
                        failedExternalOrLocalReason = reason,
                        canFallbackToLocalLlm = verifiedLocal != null,
                        canFallbackToRuleBased = true
                    )
                }

                val provider = preferences.activeExternalProviderId?.let { externalProviderRepository.getProvider(it) }
                if (provider == null || !provider.enabled) {
                    val reason = if (isPersian) {
                        "سرویس هوش مصنوعی خارجی فعالی انتخاب نشده یا غیرفعال است."
                    } else {
                        "No enabled external AI provider is selected."
                    }
                    val insight = buildFailedInsight(
                        analysisType = analysisType,
                        providerType = AiExecutionMode.EXTERNAL_API.name,
                        modelIdentifier = provider?.modelId ?: "none",
                        workoutIdsCsv = workoutIdsCsv,
                        sourceRangeLabel = sourceRangeLabel,
                        isPersian = isPersian,
                        errorMessage = reason
                    )
                    return AnalysisRequestOutcome(
                        insight = insight,
                        usedProviderName = provider?.name ?: "External AI",
                        failedExternalOrLocalReason = reason,
                        canFallbackToLocalLlm = verifiedLocal != null,
                        canFallbackToRuleBased = true
                    )
                }

                val apiKey = externalProviderRepository.getApiKey(provider.id)
                val res = externalProviderRepository.client.runChatCompletion(
                    config = provider,
                    apiKey = apiKey,
                    systemPrompt = systemPrompt,
                    userPrompt = userPrompt
                )
                res.fold(
                    onSuccess = { reply ->
                        val insight = SavedInsightEntity(
                            id = "ins_${UUID.randomUUID()}",
                            timestampMs = System.currentTimeMillis(),
                            analysisType = analysisType.name,
                            providerType = AiExecutionMode.EXTERNAL_API.name,
                            modelIdentifier = "${provider.name} · ${provider.modelId}",
                            relatedWorkoutIdsCsv = workoutIdsCsv,
                            sourceRangeLabel = sourceRangeLabel,
                            status = InsightStatus.COMPLETED.name,
                            summaryTitle = if (isPersian) "تحلیل مربی هوشمند (${provider.name})" else "AI Insight (${provider.name})",
                            bodyText = reply,
                            issuesJson = savedInsightRepository.encodeIssues(extractIssuesFromWorkouts(payload, isPersian)),
                            errorMessage = null
                        )
                        if (saveOnSuccess) savedInsightRepository.save(insight)
                        AnalysisRequestOutcome(
                            insight = insight,
                            usedProviderName = provider.name
                        )
                    },
                    onFailure = { err ->
                        val reason = err.localizedMessage ?: "External AI request failed"
                        val insight = buildFailedInsight(
                            analysisType = analysisType,
                            providerType = AiExecutionMode.EXTERNAL_API.name,
                            modelIdentifier = "${provider.name} · ${provider.modelId}",
                            workoutIdsCsv = workoutIdsCsv,
                            sourceRangeLabel = sourceRangeLabel,
                            isPersian = isPersian,
                            errorMessage = reason
                        )
                        AnalysisRequestOutcome(
                            insight = insight,
                            usedProviderName = provider.name,
                            failedExternalOrLocalReason = reason,
                            canFallbackToLocalLlm = verifiedLocal != null,
                            canFallbackToRuleBased = true
                        )
                    }
                )
            }
        }
    }

    suspend fun saveRuleBasedFallback(
        sessions: List<WorkoutSessionWithDetails>,
        preferences: UserPreferences,
        analysisType: AnalysisType,
        isPersian: Boolean,
        sourceRangeLabel: String,
        healthMetrics: AnalyzedHealthMetrics? = null
    ): SavedInsightEntity {
        val payload = buildStructuredPayload(
            sessions = sessions,
            preferences = preferences,
            analysisType = analysisType,
            isPersian = isPersian,
            sourceRangeLabel = sourceRangeLabel,
            healthMetrics = healthMetrics
        )
        val insight = buildRuleBasedInsight(
            payload = payload,
            analysisType = analysisType,
            workoutIdsCsv = sessions.joinToString(",") { it.session.id.toString() },
            isPersian = isPersian,
            fallbackStatus = true
        )
        savedInsightRepository.save(insight)
        return insight
    }

    private fun buildSystemPrompt(isPersian: Boolean): String {
        return if (isPersian) {
            """
            شما دستیار هوشمند تحلیل تمرین هستید. فقط بر اساس داده‌های واقعی JSON ورودی تحلیل کنید.
            هرگز تمرین، وزنه یا داده سلامتی خیالی اضافه نکنید و تشخیص پزشکی ندهید.
            خلاصه واضح، نکات فرم مشاهده‌شده و پیشنهاد عملی برای جلسه بعد بنویسید.
            """.trimIndent()
        } else {
            """
            You are a practical fitness summary assistant. Base every statement strictly on the provided workout JSON.
            Do NOT invent exercises, weights, or health metrics, and do NOT give medical diagnoses.
            Provide a concise summary, observed form notes, and 1–2 realistic focus points for the next session.
            """.trimIndent()
        }
    }

    private fun buildUserPrompt(payloadJson: String, userQuestion: String?, isPersian: Boolean): String {
        return buildString {
            if (!userQuestion.isNullOrBlank()) {
                append(if (isPersian) "پرسش کاربر: $userQuestion\n\n" else "User Question: $userQuestion\n\n")
            }
            append(if (isPersian) "داده ساختاریافته تمرین (JSON):\n" else "Structured Workout Data (JSON):\n")
            append(payloadJson)
        }
    }

    private fun buildRuleBasedInsight(
        payload: WorkoutAnalysisPayload,
        analysisType: AnalysisType,
        workoutIdsCsv: String,
        isPersian: Boolean,
        fallbackStatus: Boolean
    ): SavedInsightEntity {
        val totalWorkouts = payload.workouts.size
        val totalMinutes = payload.workouts.sumOf { it.durationMinutes }
        val totalSets = payload.workouts.sumOf { w -> w.exercises.sumOf { it.setsCompleted } }
        val totalReps = payload.workouts.sumOf { w -> w.exercises.sumOf { it.totalReps } }
        val totalHoldSec = payload.workouts.sumOf { w -> w.exercises.sumOf { it.totalHoldSeconds } }
        val formIssues = payload.workouts.flatMap { it.observedFormIssues }.distinct()
        val issues = extractIssuesFromWorkouts(payload, isPersian)

        val athleteName = PersonalizedGreeting.sanitizeName(payload.athleteContext.athleteDisplayName)
        val body = if (totalWorkouts == 0) {
            if (isPersian) {
                if (athleteName != null) {
                    "$athleteName، در بازه انتخاب‌شده هنوز جلسه تمرینی ثبت نشده است. پس از ثبت اولین تمرین، خلاصه و نکات فرم در اینجا نمایش داده می‌شود."
                } else {
                    "در بازه انتخاب‌شده هیچ جلسه تمرینی ثبت نشده است. پس از ثبت اولین تمرین، خلاصه و نکات فرم در اینجا نمایش داده می‌شود."
                }
            } else {
                if (athleteName != null) {
                    "$athleteName, no workouts were recorded in this range yet. Complete and save a session to generate a summary."
                } else {
                    "No workouts were recorded in this range yet. Complete and save a session to generate a summary."
                }
            }
        } else if (isPersian) {
            buildString {
                if (athleteName != null) {
                    append("$athleteName، در بازه «${payload.sourceRangeLabel}»، $totalWorkouts جلسه تمرینی ($totalMinutes دقیقه، $totalSets ست) ثبت شده است.\n")
                } else {
                    append("در بازه «${payload.sourceRangeLabel}»، $totalWorkouts جلسه تمرینی ($totalMinutes دقیقه، $totalSets ست) ثبت شده است.\n")
                }
                if (totalReps > 0) append("• مجموع تکرارهای ثبت‌شده: $totalReps\n")
                if (totalHoldSec > 0) append("• مجموع زمان نگهداشتن (ایزومتریک): ${totalHoldSec} ثانیه\n")
                payload.optionalHealthMetrics?.let { h ->
                    h.todaySteps?.let { append("• قدم‌های ثبت‌شده در Health Connect: $it\n") }
                    h.recentSleepMinutes?.let { append("• خواب اخیر: ${it / 60} ساعت و ${it % 60} دقیقه\n") }
                }
                if (formIssues.isNotEmpty()) {
                    append("\nنکات فرم مشاهده‌شده:\n")
                    formIssues.take(4).forEach { append("• $it\n") }
                } else {
                    append("\nهشدار فرم خاصی در این جلسات ثبت نشده است.\n")
                }
                if (!payload.userQuestion.isNullOrBlank()) {
                    append("\nپاسخ به پرسش شما (خلاصه قانون‌محور): بر اساس حرکات ثبت‌شده، تمرکز بر تکمیل دامنه کامل حرکت و ثبت منظم ست‌ها پیشنهاد می‌شود.")
                }
            }
        } else {
            buildString {
                if (athleteName != null) {
                    append("$athleteName, across \"${payload.sourceRangeLabel}\", you logged $totalWorkouts workout(s) totaling $totalMinutes min and $totalSets sets.\n")
                } else {
                    append("Across \"${payload.sourceRangeLabel}\", you logged $totalWorkouts workout(s) totaling $totalMinutes min and $totalSets sets.\n")
                }
                if (totalReps > 0) append("• Total recorded reps: $totalReps\n")
                if (totalHoldSec > 0) append("• Total timed hold duration: ${totalHoldSec}s\n")
                payload.optionalHealthMetrics?.let { h ->
                    h.todaySteps?.let { append("• Health Connect steps today: $it\n") }
                    h.recentSleepMinutes?.let { append("• Recent sleep logged: ${it / 60}h ${it % 60}m\n") }
                }
                if (formIssues.isNotEmpty()) {
                    append("\nObserved form cues:\n")
                    formIssues.take(4).forEach { append("• $it\n") }
                } else {
                    append("\nNo major form warnings were logged across these sets.\n")
                }
                if (!payload.userQuestion.isNullOrBlank()) {
                    append("\nRegarding your question: based on your logged sets, prioritize controlled full range of motion and consistent pacing before increasing volume.")
                }
            }
        }

        val title = if (isPersian) "خلاصه تمرینی آفلاین" else "Offline Training Summary"
        return SavedInsightEntity(
            id = "ins_${UUID.randomUUID()}",
            timestampMs = System.currentTimeMillis(),
            analysisType = analysisType.name,
            providerType = AiExecutionMode.RULE_BASED_ONLY.name,
            modelIdentifier = if (isPersian) "موتور قانون‌محور محلی" else "Deterministic Rule Engine",
            relatedWorkoutIdsCsv = workoutIdsCsv,
            sourceRangeLabel = payload.sourceRangeLabel,
            status = if (fallbackStatus) InsightStatus.FALLBACK_RULE_BASED.name else InsightStatus.COMPLETED.name,
            summaryTitle = title,
            bodyText = body.trim(),
            issuesJson = savedInsightRepository.encodeIssues(issues),
            errorMessage = null
        )
    }

    private fun buildFailedInsight(
        analysisType: AnalysisType,
        providerType: String,
        modelIdentifier: String,
        workoutIdsCsv: String,
        sourceRangeLabel: String,
        isPersian: Boolean,
        errorMessage: String
    ): SavedInsightEntity {
        return SavedInsightEntity(
            id = "ins_${UUID.randomUUID()}",
            timestampMs = System.currentTimeMillis(),
            analysisType = analysisType.name,
            providerType = providerType,
            modelIdentifier = modelIdentifier,
            relatedWorkoutIdsCsv = workoutIdsCsv,
            sourceRangeLabel = sourceRangeLabel,
            status = InsightStatus.FAILED.name,
            summaryTitle = if (isPersian) "خطا در اجرای تحلیل هوش مصنوعی" else "AI Analysis Failed",
            bodyText = errorMessage,
            issuesJson = "[]",
            errorMessage = errorMessage
        )
    }

    private fun extractIssuesFromWorkouts(
        payload: WorkoutAnalysisPayload,
        isPersian: Boolean
    ): List<InsightIssueItem> {
        val formIssues = payload.workouts.flatMap { it.observedFormIssues }.distinct()
        return formIssues.take(5).map { cue ->
            InsightIssueItem(
                severity = "SUGGESTION",
                title = if (isPersian) "نکته فرم حرکت" else "Form Cue",
                detail = cue
            )
        }
    }
}
