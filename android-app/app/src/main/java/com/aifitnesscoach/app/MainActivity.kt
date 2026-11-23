package com.aifitnesscoach.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aifitnesscoach.R
import com.aifitnesscoach.core.database.UserPreferences
import com.aifitnesscoach.core.exercise.ExerciseCategory
import com.aifitnesscoach.core.exercise.definitions.ExerciseCatalog
import com.aifitnesscoach.core.localization.LocaleManager
import com.aifitnesscoach.core.motivation.MotivationalQuote
import com.aifitnesscoach.core.motivation.VerifiedQuoteRepository
import com.aifitnesscoach.core.notifications.AppNotificationManager
import com.aifitnesscoach.core.ui.theme.AIFitnessCoachTheme
import com.aifitnesscoach.feature.ai.AIAnalysisScreen
import com.aifitnesscoach.feature.ai.AIProviderSettingsScreen
import com.aifitnesscoach.feature.ai.LocalModelManagerScreen
import com.aifitnesscoach.feature.ai.ScheduledAnalysisScreen
import com.aifitnesscoach.feature.exercises.ExercisesScreen
import com.aifitnesscoach.feature.exercises.ExerciseWorkoutSetup
import com.aifitnesscoach.feature.health.HealthScreen
import com.aifitnesscoach.feature.history.HistoryScreen
import com.aifitnesscoach.feature.home.HomeScreen
import com.aifitnesscoach.feature.media.ExerciseMediaLibraryScreen
import com.aifitnesscoach.feature.onboarding.OnboardingScreen
import com.aifitnesscoach.feature.profile.ProfileScreen
import com.aifitnesscoach.feature.settings.SettingsScreen
import com.aifitnesscoach.feature.settings.SettingsHome
import com.aifitnesscoach.feature.settings.SettingsPage
import com.aifitnesscoach.core.ui.CenteredContent
import androidx.compose.foundation.layout.consumeWindowInsets
import com.aifitnesscoach.feature.settings.ThemePreferencesScreen
import com.aifitnesscoach.feature.workout.CameraPositionGuide
import com.aifitnesscoach.feature.workout.WorkoutScreen
import com.aifitnesscoach.feature.workout.WorkoutViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.Duration
import kotlinx.coroutines.delay
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aifitnesscoach.feature.widget.WorkoutWidgetRefresh
import com.aifitnesscoach.feature.widget.WorkoutWidgetRoutes

sealed class MainDestination(val route: String) {
    data object Onboarding : MainDestination("onboarding")
    data object Home : MainDestination("home")
    data object Exercises : MainDestination("exercises")
    data object ExerciseSetup : MainDestination("exercise_setup/{exercise}")
    data object QuickWorkout : MainDestination("quick_workout")
    data object History : MainDestination("history")
    data object Health : MainDestination("health")
    data object HealthSettings : MainDestination("health_connect_settings")
    data object Settings : MainDestination("settings")
    data object SettingsDetail : MainDestination("settings/{section}")
    data object PersonalProfile : MainDestination("personal_profile")
    data object ThemePreferences : MainDestination("theme_preferences")
    data object Workout : MainDestination("workout")
    data object CameraGuide : MainDestination("camera_guide/{exercise}")
    data object MediaLibrary : MainDestination("media_library")
    data object MediaLibraryExercise : MainDestination("media_library?exercise={exercise}")
    data object LocalModels : MainDestination("local_models")
    data object AiProviders : MainDestination("ai_providers")
    data object AiInsights : MainDestination("ai_insights")
    data object ScheduledAnalysis : MainDestination("scheduled_analysis")
}

class MainActivity : AppCompatActivity() {

    private lateinit var appContainer: AppContainer
    private var pendingDeepLinkRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appContainer = ViewModelProvider(this)[AppContainerViewModel::class.java].container
        pendingDeepLinkRoute = extractDeepLinkRoute(intent)

        setContent {
            val nullablePrefs by appContainer.preferencesRepository.preferencesFlow
                .collectAsStateWithLifecycle(initialValue = null)

            val activePrefs = nullablePrefs ?: UserPreferences()

            LaunchedEffect(nullablePrefs?.language) {
                nullablePrefs?.language?.let(LocaleManager::applyLanguage)
            }

            val isPersian = LocaleManager.isPersianActive(activePrefs.language)
            val layoutDirection = LocaleManager.layoutDirectionFor(activePrefs.language)

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                AIFitnessCoachTheme(
                    gender = activePrefs.profile.gender,
                    themeMode = activePrefs.themeMode,
                    colorPalette = activePrefs.colorPalette,
                    useDynamicColor = activePrefs.useDynamicColor
                ) {
                    val loadedPrefs = nullablePrefs
                    if (loadedPrefs == null) {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    } else {
                        AIFitnessCoachAppRoot(
                            container = appContainer,
                            preferences = loadedPrefs,
                            isPersian = isPersian,
                            pendingDeepLinkRoute = pendingDeepLinkRoute,
                            onConsumeDeepLink = { pendingDeepLinkRoute = null }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLinkRoute = extractDeepLinkRoute(intent)
    }

    private fun extractDeepLinkRoute(intent: Intent?): String? {
        return routeForLaunchExtra(intent?.getStringExtra(AppNotificationManager.EXTRA_OPEN_ROUTE))
    }
}

internal fun routeForLaunchExtra(extra: String?): String? = when (extra) {
    AppNotificationManager.ROUTE_INSIGHTS -> MainDestination.AiInsights.route
    WorkoutWidgetRoutes.QUICK_START -> MainDestination.QuickWorkout.route
    WorkoutWidgetRoutes.ACTIVITY -> MainDestination.History.route
    WorkoutWidgetRoutes.MOTIVATION -> MainDestination.Home.route
    else -> null
}

internal fun initialAppRoute(profile: com.aifitnesscoach.core.database.UserProfile): String =
    if (profile.hasCompletedOnboarding) MainDestination.Home.route else MainDestination.Onboarding.route

@Composable
private fun AIFitnessCoachAppRoot(
    container: AppContainer,
    preferences: UserPreferences,
    isPersian: Boolean,
    pendingDeepLinkRoute: String?,
    onConsumeDeepLink: () -> Unit
) {
    val navController = rememberNavController()
    val appContext = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        container.notificationManager.ensureChannels()
        container.exerciseAssetRepository.reconcileOrphanedDownloads()
        container.localModelRepository.reconcileOnStartup()
        if (preferences.scheduledAnalysisEnabled) {
            container.analysisScheduler.syncSchedule(preferences)
        }
    }

    val sessions by container.workoutDao.observeAllSessionsWithDetails()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val recentExerciseIds by container.workoutDao.observeRecentExerciseIds()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val syncedCount by container.workoutDao.observeSyncedCount()
        .collectAsStateWithLifecycle(initialValue = 0)
    val downloadedAssetsMap by remember {
        container.exerciseAssetDao.observeAll().map { list -> list.associateBy { it.exerciseId } }
    }.collectAsStateWithLifecycle(initialValue = emptyMap())
    val latestInsight by container.savedInsightRepository.observeLatestInsight()
        .collectAsStateWithLifecycle(initialValue = null)

    val workoutViewModel = viewModel {
        WorkoutViewModel(
            workoutDao = container.workoutDao,
            coachExplanationProvider = container.coachExplanationProvider,
            ttsCoachManager = container.ttsCoachManager,
            healthConnectManager = container.healthConnectManager,
            onWorkoutPersisted = { WorkoutWidgetRefresh.request(appContext) }
        )
    }
    val workoutUiState by workoutViewModel.uiState.collectAsStateWithLifecycle()

    var selectedCategoryFilter by rememberSaveable { mutableStateOf<ExerciseCategory?>(null) }
    var selectedAnalysisSessionId by remember { mutableStateOf<Long?>(null) }

    val quoteRepository = remember { VerifiedQuoteRepository() }
    val lifecycleOwner = LocalLifecycleOwner.current
    var dailyQuote by remember { mutableStateOf<MotivationalQuote?>(null) }
    LaunchedEffect(lifecycleOwner, preferences.dailyQuoteId) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val now = ZonedDateTime.now()
                dailyQuote = container.preferencesRepository.dailyQuote(now.toLocalDate(), quoteRepository)
                val next = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
                delay(Duration.between(now, next).toMillis().coerceAtLeast(1_000))
            }
        }
    }
    LaunchedEffect(preferences.language, preferences.themeMode, sessions.size) {
        WorkoutWidgetRefresh.request(appContext)
    }

    val startDestination = remember { initialAppRoute(preferences.profile) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(pendingDeepLinkRoute, preferences.profile.hasCompletedOnboarding, currentRoute) {
        if (pendingDeepLinkRoute != null && preferences.profile.hasCompletedOnboarding &&
            currentRoute != null && currentRoute != MainDestination.Onboarding.route
        ) {
            selectedAnalysisSessionId = null
            if (pendingDeepLinkRoute == MainDestination.QuickWorkout.route) {
                selectedCategoryFilter = null
            }
            navController.navigate(pendingDeepLinkRoute) {
                launchSingleTop = true
            }
            onConsumeDeepLink()
        }
    }

    val topLevelRoutes = remember {
        setOf(
            MainDestination.Home.route,
            MainDestination.Exercises.route,
            MainDestination.History.route,
            MainDestination.Health.route,
            MainDestination.Settings.route
        )
    }

    val bottomItems = listOf(
        Triple(MainDestination.Home.route, R.string.nav_home, Icons.Outlined.Home),
        Triple(MainDestination.Exercises.route, R.string.nav_exercises, Icons.Outlined.FitnessCenter),
        Triple(MainDestination.History.route, R.string.nav_history, Icons.Outlined.History),
        Triple(MainDestination.Health.route, R.string.nav_health, Icons.Outlined.FavoriteBorder),
        Triple(MainDestination.Settings.route, R.string.nav_settings, Icons.Outlined.Settings)
    )

    Scaffold(
        bottomBar = {
            if (currentRoute in topLevelRoutes) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp
                ) {
                    bottomItems.forEach { (route, labelRes, icon) ->
                        NavigationBarItem(
                            selected = currentRoute == route,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(MainDestination.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = null) },
                            label = { Text(stringResource(labelRes)) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            CenteredContent {
                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
                    composable(MainDestination.Onboarding.route) {
                        OnboardingScreen(
                            initialProfile = preferences.profile,
                            initialLanguage = preferences.language,
                            savedDraft = preferences.onboardingDraft,
                            onSaveDraft = container.preferencesRepository::saveOnboardingDraft,
                            onLanguageSelected = { lang ->
                                scope.launch { container.preferencesRepository.setLanguage(lang) }
                            },
                            onCompleteOnboarding = { profile ->
                                container.preferencesRepository.saveProfile(profile.copy(hasCompletedOnboarding = true))
                                navController.navigate(MainDestination.Home.route) {
                                    popUpTo(MainDestination.Onboarding.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(MainDestination.Home.route) {
                        HomeScreen(
                            profile = preferences.profile,
                            isPersian = isPersian,
                            recentExerciseIds = recentExerciseIds,
                            allSessions = sessions,
                            latestInsight = latestInsight,
                            dailyQuote = dailyQuote,
                            onOpenProfileSetup = { navController.navigate(MainDestination.PersonalProfile.route) },
                            onStartQuickWorkout = {
                                navController.navigate(MainDestination.QuickWorkout.route) { launchSingleTop = true }
                            },
                            onBuildMultiWorkout = {
                                workoutViewModel.openSessionBuilder()
                                navController.navigate(MainDestination.Workout.route) { launchSingleTop = true }
                            },
                            onContinueLastWorkout = { last ->
                                val exercises = last.sets
                                    .mapNotNull { ExerciseCatalog.findById(it.exerciseId) }
                                    .distinctBy { it.id }
                                workoutViewModel.openSessionBuilder(exercises)
                                navController.navigate(MainDestination.Workout.route) { launchSingleTop = true }
                            },
                            onSelectCategory = { cat ->
                                selectedCategoryFilter = cat
                                navController.navigate(MainDestination.Exercises.route) { launchSingleTop = true }
                            },
                            onSelectExercise = { ex ->
                                navController.navigate("exercise_setup/${ex.id}") { launchSingleTop = true }
                            },
                            onOpenInsights = {
                                selectedAnalysisSessionId = null
                                navController.navigate(MainDestination.AiInsights.route)
                            }
                        )
                    }

                    listOf(MainDestination.Exercises.route, MainDestination.QuickWorkout.route).forEach { route ->
                        composable(route) {
                            val quickPicker = route == MainDestination.QuickWorkout.route
                            ExercisesScreen(
                                isPersian = isPersian,
                                initialCategoryFilter = if (quickPicker) null else selectedCategoryFilter,
                                onCloseQuickPicker = if (quickPicker) ({ navController.popBackStack() }) else null,
                                onSelectExerciseForDetail = { exercise ->
                                    navController.navigate("exercise_setup/${exercise.id}") { launchSingleTop = true }
                                }
                            )
                        }
                    }

                    composable(MainDestination.ExerciseSetup.route) { entry ->
                        val exercise = entry.arguments?.getString("exercise")?.let(ExerciseCatalog::findById)
                            ?: return@composable
                        ExerciseWorkoutSetup(
                            exercise = exercise,
                            downloadedAsset = downloadedAssetsMap[exercise.id],
                            isPersian = isPersian,
                            onBack = { navController.popBackStack() },
                            onOpenMedia = { navController.navigate("media_library?exercise=${exercise.id}") },
                            onOpenCameraGuide = { navController.navigate("camera_guide/${exercise.id}") },
                            onStartWorkout = { item ->
                                workoutViewModel.startQuickExercise(
                                    exercise = exercise,
                                    customItem = item,
                                    skipCameraGuide = preferences.cameraGuideAcknowledged
                                )
                                navController.navigate(MainDestination.Workout.route) { launchSingleTop = true }
                            }
                        )
                    }

                    composable(MainDestination.History.route) {
                        HistoryScreen(
                            isPersian = isPersian,
                            sessions = sessions,
                            onAnalyzeWorkouts = { sessionId ->
                                selectedAnalysisSessionId = sessionId
                                navController.navigate(MainDestination.AiInsights.route)
                            }
                        )
                    }

                    listOf(MainDestination.Health.route, MainDestination.HealthSettings.route).forEach { route ->
                        composable(route) {
                            HealthScreen(
                                preferences = preferences,
                                preferencesRepository = container.preferencesRepository,
                                workoutDao = container.workoutDao,
                                healthConnectManager = container.healthConnectManager,
                                syncedWorkoutCount = syncedCount,
                                onBack = if (route == MainDestination.HealthSettings.route) ({ navController.popBackStack() }) else null,
                                onOpenInsights = {
                                    selectedAnalysisSessionId = null
                                    navController.navigate(MainDestination.AiInsights.route)
                                }
                            )
                        }
                    }

                    composable(MainDestination.Settings.route) {
                        SettingsHome(preferences) { page ->
                            val route = when (page) {
                                SettingsPage.PROFILE -> MainDestination.PersonalProfile.route
                                SettingsPage.STORAGE -> MainDestination.MediaLibrary.route
                                SettingsPage.HEALTH -> MainDestination.HealthSettings.route
                                else -> "settings/${page.name}"
                            }
                            navController.navigate(route) { launchSingleTop = true }
                        }
                    }

                    composable(MainDestination.SettingsDetail.route) { entry ->
                        val page = SettingsPage.entries.firstOrNull { it.name == entry.arguments?.getString("section") }
                            ?: return@composable
                        SettingsScreen(
                            page = page,
                            onBack = { navController.popBackStack() },
                            preferences = preferences,
                            preferencesRepository = container.preferencesRepository,
                            workoutDao = container.workoutDao,
                            sessions = sessions,
                            exerciseAssetRepository = container.exerciseAssetRepository,
                            localModelRepository = container.localModelRepository,
                            savedInsightRepository = container.savedInsightRepository,
                            secretStore = container.secretStore,
                            onOpenPersonalProfile = { navController.navigate(MainDestination.PersonalProfile.route) },
                            onOpenThemePreferences = { navController.navigate(MainDestination.ThemePreferences.route) },
                            onOpenHealthConnect = { navController.navigate(MainDestination.Health.route) },
                            onOpenMediaLibrary = { navController.navigate(MainDestination.MediaLibrary.route) },
                            onOpenLocalModels = { navController.navigate(MainDestination.LocalModels.route) },
                            onOpenExternalProviders = { navController.navigate(MainDestination.AiProviders.route) },
                            onOpenScheduledAnalysis = { navController.navigate(MainDestination.ScheduledAnalysis.route) },
                            onOpenInsights = {
                                selectedAnalysisSessionId = null
                                navController.navigate(MainDestination.AiInsights.route)
                            }
                        )
                    }

                    composable(MainDestination.PersonalProfile.route) {
                        ProfileScreen(
                            preferences = preferences,
                            preferencesRepository = container.preferencesRepository,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(MainDestination.ThemePreferences.route) {
                        ThemePreferencesScreen(
                            preferences = preferences,
                            preferencesRepository = container.preferencesRepository,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(MainDestination.CameraGuide.route) { entry ->
                        val exercise = entry.arguments?.getString("exercise")?.let(ExerciseCatalog::findById)
                            ?: return@composable
                        CameraPositionGuide(exercise, isPersian) { navController.popBackStack() }
                    }

                    composable(MainDestination.Workout.route) {
                        WorkoutScreen(
                            uiState = workoutUiState,
                            preferences = preferences,
                            isPersian = isPersian,
                            coachExplanationProvider = container.coachExplanationProvider,
                            viewModel = workoutViewModel,
                            onCameraGuideAcknowledged = {
                                scope.launch {
                                    container.preferencesRepository.setCameraGuideAcknowledged(true)
                                }
                            },
                            onOpenCameraGuide = { id ->
                                navController.navigate("camera_guide/$id") { launchSingleTop = true }
                            },
                            onPoseTrackingActiveChanged = { active ->
                                container.localInferenceEngine.setPoseTrackingActive(active)
                            },
                            onSaveAndAnalyze = {
                                selectedAnalysisSessionId = null
                                navController.navigate(MainDestination.AiInsights.route) {
                                    popUpTo(MainDestination.Home.route) { inclusive = false }
                                }
                            },
                            onExitWorkout = { navController.popBackStack() }
                        )
                    }

                    composable(
                        MainDestination.MediaLibraryExercise.route,
                        arguments = listOf(navArgument("exercise") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        })
                    ) { entry ->
                        ExerciseMediaLibraryScreen(
                            isPersian = isPersian,
                            assetRepository = container.exerciseAssetRepository,
                            downloadManager = container.exerciseAssetDownloadManager,
                            preferencesRepository = container.preferencesRepository,
                            wifiOnly = preferences.mediaWifiOnly,
                            exerciseId = entry.arguments?.getString("exercise"),
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(MainDestination.LocalModels.route) {
                        LocalModelManagerScreen(
                            isPersian = isPersian,
                            repository = container.localModelRepository,
                            downloadManager = container.localModelDownloadManager,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(MainDestination.AiProviders.route) {
                        AIProviderSettingsScreen(
                            isPersian = isPersian,
                            preferences = preferences,
                            repository = container.aiProviderRepository,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(MainDestination.AiInsights.route) {
                        AIAnalysisScreen(
                            isPersian = isPersian,
                            preferences = preferences,
                            preferencesRepository = container.preferencesRepository,
                            sessions = sessions,
                            focusSessionId = selectedAnalysisSessionId,
                            coordinator = container.aiAnalysisCoordinator,
                            insightRepository = container.savedInsightRepository,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(MainDestination.ScheduledAnalysis.route) {
                        ScheduledAnalysisScreen(
                            preferences = preferences,
                            preferencesRepository = container.preferencesRepository,
                            scheduler = container.analysisScheduler,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
