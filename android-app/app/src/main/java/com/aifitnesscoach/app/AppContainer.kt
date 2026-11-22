package com.aifitnesscoach.app

import android.content.Context
import com.aifitnesscoach.core.ai.analysis.AIAnalysisCoordinator
import com.aifitnesscoach.core.ai.analysis.SavedInsightRepository
import com.aifitnesscoach.core.ai.external.AIProviderRepository
import com.aifitnesscoach.core.ai.local.LiteRtLmInferenceEngine
import com.aifitnesscoach.core.ai.local.LocalLlmRuntime
import com.aifitnesscoach.core.ai.local.LocalModelDownloadManager
import com.aifitnesscoach.core.ai.local.LocalModelRepository
import com.aifitnesscoach.core.ai.schedule.AnalysisScheduler
import com.aifitnesscoach.core.database.AppDatabase
import com.aifitnesscoach.core.database.UserPreferencesRepository
import com.aifitnesscoach.core.exercise.CoachExplanationProvider
import com.aifitnesscoach.core.exercise.DeterministicCoachExplanationProvider
import com.aifitnesscoach.core.media.ExerciseAssetDownloadManager
import com.aifitnesscoach.core.media.ExerciseAssetRepository
import com.aifitnesscoach.core.media.TtsCoachManager
import com.aifitnesscoach.core.notifications.AppNotificationManager
import com.aifitnesscoach.core.security.AndroidKeystoreSecretStore
import com.aifitnesscoach.core.security.SecretStore
import com.aifitnesscoach.integration.healthconnect.HealthConnectManager

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase = AppDatabase.getInstance(appContext)
    val workoutDao = database.workoutDao()
    val exerciseAssetDao = database.exerciseAssetDao()
    val localModelDao = database.localModelDao()
    val aiProviderDao = database.aiProviderDao()
    val savedInsightDao = database.savedInsightDao()

    val preferencesRepository = UserPreferencesRepository(appContext)
    val secretStore: SecretStore = AndroidKeystoreSecretStore(appContext)
    val healthConnectManager = HealthConnectManager(appContext)
    val coachExplanationProvider: CoachExplanationProvider = DeterministicCoachExplanationProvider()
    val ttsCoachManager = TtsCoachManager(appContext)
    val notificationManager = AppNotificationManager(appContext)

    val exerciseAssetRepository = ExerciseAssetRepository(appContext, exerciseAssetDao)
    val exerciseAssetDownloadManager = ExerciseAssetDownloadManager(appContext, exerciseAssetRepository)

    val localInferenceEngine: LocalLlmRuntime = LiteRtLmInferenceEngine(appContext)
    val localModelRepository = LocalModelRepository(
        context = appContext,
        localModelDao = localModelDao,
        preferencesRepository = preferencesRepository,
        secretStore = secretStore,
        inferenceEngine = localInferenceEngine
    )
    val localModelDownloadManager = LocalModelDownloadManager(appContext)

    val aiProviderRepository = AIProviderRepository(
        dao = aiProviderDao,
        secretStore = secretStore,
        preferencesRepository = preferencesRepository
    )
    val savedInsightRepository = SavedInsightRepository(savedInsightDao)

    val aiAnalysisCoordinator = AIAnalysisCoordinator(
        localModelRepository = localModelRepository,
        localInferenceEngine = localInferenceEngine,
        externalProviderRepository = aiProviderRepository,
        savedInsightRepository = savedInsightRepository
    )

    val analysisScheduler = AnalysisScheduler(appContext)
}
