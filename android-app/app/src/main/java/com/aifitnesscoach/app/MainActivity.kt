package com.aifitnesscoach.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.foundation.shape.CircleShape
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
import com.aifitnesscoach.core.database.AppFeature
import com.aifitnesscoach.core.database.nextRotationSlot
import com.aifitnesscoach.core.database.UserPreferences
import com.aifitnesscoach.core.exercise.ExerciseCategory
import com.aifitnesscoach.core.exercise.definitions.ExerciseCatalog
import com.aifitnesscoach.core.equipment.WeightEntry
import com.aifitnesscoach.core.equipment.WeightUnit
import com.aifitnesscoach.core.equipment.WeightLoadMode
import com.aifitnesscoach.core.exercise.definitions.ExternalExerciseSources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import com.aifitnesscoach.feature.plan.WeeklyPlan
import com.aifitnesscoach.feature.plan.WeeklyPlanScreen
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
import java.time.LocalDate
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
    data object Plan : MainDestination("plan")
    data object PlanEdit : MainDestination("plan_edit/{day}")
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
            var catalogReady by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) { ExternalExerciseSources.load(applicationContext) }
                catalogReady = true
            }
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
                    if (loadedPrefs == null || !catalogReady) {
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
    val pendingDownloads by container.exerciseAssetDownloadManager.activeDownloads
        .collectAsStateWithLifecycle(initialValue = emptySet())
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
            onWorkoutPersisted = { WorkoutWidgetRefresh.request(appContext) },
            onRotationCompleted = container.preferencesRepository::completeRotationSlot
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

    val layout = preferences.interfacePreferences
    val showPlan = layout.shows(AppFeature.PLAN)
    val showProgress = layout.shows(AppFeature.PROGRESS)
    val showAi = layout.shows(AppFeature.AI)
    val showMedia = layout.shows(AppFeature.MEDIA)
    val showHealth = layout.shows(AppFeature.HEALTH)
    val hiddenRoutes = buildSet {
        if (!showPlan) addAll(listOf(MainDestination.Plan.route, MainDestination.PlanEdit.route))
        if (!showAi) addAll(listOf(MainDestination.AiInsights.route, MainDestination.AiProviders.route,
            MainDestination.LocalModels.route, MainDestination.ScheduledAnalysis.route))
        if (!showMedia) add(MainDestination.MediaLibrary.route)
        if (!showHealth) addAll(listOf(MainDestination.Health.route, MainDestination.HealthSettings.route))
    }
    LaunchedEffect(currentRoute, layout) {
        if (currentRoute in hiddenRoutes) {
            navController.navigate(MainDestination.Home.route) {
                popUpTo(MainDestination.Home.route)
                launchSingleTop = true
            }
        }
    }
    LaunchedEffect(pendingDeepLinkRoute, preferences.profile.hasCompletedOnboarding, currentRoute, layout) {
        if (pendingDeepLinkRoute != null && preferences.profile.hasCompletedOnboarding &&
            currentRoute != null && currentRoute != MainDestination.Onboarding.route
        ) {
            selectedAnalysisSessionId = null
            if (pendingDeepLinkRoute == MainDestination.QuickWorkout.route) {
                selectedCategoryFilter = null
            }
            if (pendingDeepLinkRoute !in hiddenRoutes) navController.navigate(pendingDeepLinkRoute) {
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
            MainDestination.Plan.route,
            MainDestination.AiInsights.route
        )
    }

    fun startToday() {
        if (navController.currentDestination?.route !in topLevelRoutes) return
        val today = LocalDate.now().dayOfWeek.value
        val plans = if (showPlan) preferences.weeklyPlan.mapValues { WeeklyPlan.decode(it.value) } else emptyMap()
        val slot = if (showPlan && preferences.rotationEnabled) {
            nextRotationSlot(plans.filterValues { it.isNotEmpty() }.keys, preferences.lastRotationSlot)
        } else null
        val planned = plans[slot ?: today].orEmpty()
        if (planned.isNotEmpty()) {
            workoutViewModel.openConfiguredPlan(planned)
            workoutViewModel.beginSessionFromPlan(preferences.cameraGuideAcknowledged, rotationSlot = slot)
            navController.navigate(MainDestination.Workout.route) { launchSingleTop = true }
        } else {
            navController.navigate(MainDestination.QuickWorkout.route) { launchSingleTop = true }
        }
    }

    val bottomItems = listOf(
        Triple(MainDestination.Home.route, R.string.nav_home, Icons.Outlined.Home),
        Triple(MainDestination.Exercises.route, R.string.nav_exercises, Icons.Outlined.FitnessCenter),
        Triple(MainDestination.QuickWorkout.route, R.string.nav_start, Icons.Outlined.PlayArrow),
        Triple(MainDestination.History.route, if (showProgress) R.string.nav_progress else R.string.nav_history, Icons.Outlined.History),
        Triple(MainDestination.AiInsights.route, R.string.nav_ai_analysis, Icons.Outlined.AutoAwesome)
    )

    Scaffold(
        bottomBar = {
            if (currentRoute in topLevelRoutes) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp
                ) {
                    bottomItems.forEach { (route, labelRes, icon) ->
                        if (route in hiddenRoutes) {
                            Spacer(Modifier.weight(1f))
                            return@forEach
                        }
                        val isStart = route == MainDestination.QuickWorkout.route
                        NavigationBarItem(
                            selected = currentRoute == route,
                            onClick = {
                                if (isStart) startToday()
                                else {
                                    if (route == MainDestination.AiInsights.route) selectedAnalysisSessionId = null
                                    navController.navigate(route) {
                                        popUpTo(MainDestination.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                if (isStart) {
                                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                                        Icon(icon, contentDescription = null, modifier = Modifier.padding(6.dp))
                                    }
                                } else Icon(icon, contentDescription = null)
                            },
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
                            latestInsight = latestInsight.takeIf { showAi },
                            dailyQuote = dailyQuote,
                            onOpenProfileSetup = { navController.navigate(MainDestination.PersonalProfile.route) },
                            onStartQuickWorkout = ::startToday,
                            onOpenPlan = if (showPlan) ({
                                navController.navigate(MainDestination.Plan.route) { launchSingleTop = true }
                            }) else null,
                            onOpenSettings = { navController.navigate(MainDestination.Settings.route) { launchSingleTop = true } },
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
                                downloadedAssets = downloadedAssetsMap,
                                pendingDownloads = pendingDownloads,
                                onSelectExerciseForDetail = { exercise ->
                                    navController.navigate("exercise_setup/${exercise.id}") { launchSingleTop = true }
                                }
                            )
                        }
                    }

                    composable(MainDestination.ExerciseSetup.route) { entry ->
                        val exercise = entry.arguments?.getString("exercise")?.let(ExerciseCatalog::findById)
                            ?: return@composable
                        val recentLoad = remember(sessions, exercise.id) {
                            sessions.asSequence()
                                .flatMap { session -> session.sets.asSequence().sortedByDescending { it.setNumber } }
                                .filter { it.exerciseId == exercise.id }
                                .mapNotNull { set ->
                                    val weight = set.weightValue?.takeIf { it.isFinite() && it >= 0f }
                                    val unit = WeightUnit.entries.firstOrNull { it.name == set.weightUnit }
                                    val mode = WeightLoadMode.entries.firstOrNull { it.name == set.weightLoadMode }
                                    if (weight == null || unit == null || mode == null) null
                                    else WeightEntry(weight, unit, mode)
                                }.firstOrNull()
                        }
                        ExerciseWorkoutSetup(
                            exercise = exercise,
                            recentLoad = recentLoad,
                            downloadedAsset = downloadedAssetsMap[exercise.id],
                            isPersian = isPersian,
                            onBack = { navController.popBackStack() },
                            onOpenMedia = { navController.navigate("media_library?exercise=${exercise.id}") },
                            downloadPending = exercise.id in pendingDownloads,
                            onDownloadMedia = {
                                container.exerciseAssetDownloadManager.enqueueDownload(exercise.id, preferences.mediaWifiOnly)
                            },
                            onCancelDownload = { container.exerciseAssetDownloadManager.pauseOrCancel(exercise.id) },
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

                    composable(MainDestination.Plan.route) {
                        WeeklyPlanScreen(
                            savedDays = preferences.weeklyPlan,
                            rotationEnabled = preferences.rotationEnabled,
                            lastRotationSlot = preferences.lastRotationSlot,
                            onRotationEnabled = { enabled ->
                                scope.launch { container.preferencesRepository.setRotationEnabled(enabled) }
                            },
                            isPersian = isPersian,
                            onEdit = { day, items ->
                                workoutViewModel.openConfiguredPlan(items)
                                navController.navigate("plan_edit/$day") { launchSingleTop = true }
                            },
                            onStart = { items, slot ->
                                workoutViewModel.openConfiguredPlan(items)
                                workoutViewModel.beginSessionFromPlan(preferences.cameraGuideAcknowledged, rotationSlot = slot)
                                navController.navigate(MainDestination.Workout.route) { launchSingleTop = true }
                            }
                        )
                    }

                    composable(MainDestination.History.route) {
                        HistoryScreen(
                            isPersian = isPersian,
                            sessions = sessions,
                            showAnalytics = showProgress,
                            onAnalyzeWorkouts = if (showAi) ({ sessionId ->
                                selectedAnalysisSessionId = sessionId
                                navController.navigate(MainDestination.AiInsights.route)
                            }) else null
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
                                onOpenInsights = if (showAi) ({
                                    selectedAnalysisSessionId = null
                                    navController.navigate(MainDestination.AiInsights.route)
                                }) else null
                            )
                        }
                    }

                    composable(MainDestination.Settings.route) {
                        SettingsHome(preferences, onBack = { navController.popBackStack() }) { page ->
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

                    listOf(MainDestination.Workout.route, MainDestination.PlanEdit.route).forEach { route ->
                        composable(route) { entry ->
                            val day = entry.arguments?.getString("day")?.toIntOrNull()?.takeIf { it in 1..7 }
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
                            onSaveAndAnalyze = if (showAi) ({
                                selectedAnalysisSessionId = null
                                navController.navigate(MainDestination.AiInsights.route) {
                                    popUpTo(MainDestination.Home.route) { inclusive = false }
                                }
                            }) else null,
                                onSavePlan = day?.let { weekday ->
                                    { items ->
                                        scope.launch {
                                            container.preferencesRepository.savePlanDay(weekday,
                                                if (items.isEmpty()) null else WeeklyPlan.encode(items))
                                            if (navController.currentBackStackEntry == entry) navController.popBackStack()
                                        }
                                    }
                                },
                                onExitWorkout = { navController.popBackStack() }
                            )
                        }
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
