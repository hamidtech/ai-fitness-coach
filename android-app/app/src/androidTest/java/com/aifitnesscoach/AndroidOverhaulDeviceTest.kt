package com.aifitnesscoach

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import com.aifitnesscoach.core.exercise.definitions.ExerciseCatalog
import com.aifitnesscoach.core.exercise.definitions.ExerciseSource
import com.aifitnesscoach.core.exercise.definitions.ExternalExerciseSources
import com.aifitnesscoach.core.media.AssetMediaType
import com.aifitnesscoach.core.media.ExerciseAssetCatalog
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.room.Room
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.io.File
import java.util.UUID
import androidx.test.platform.app.InstrumentationRegistry
import com.aifitnesscoach.core.database.AppDatabase
import com.aifitnesscoach.core.database.AppLanguage
import com.aifitnesscoach.core.database.AppThemeMode
import com.aifitnesscoach.core.database.ExerciseSetEntity
import com.aifitnesscoach.core.database.UserPreferencesRepository
import com.aifitnesscoach.core.database.UserProfile
import com.aifitnesscoach.core.database.WorkoutSessionEntity
import com.aifitnesscoach.core.motivation.VerifiedQuoteRepository
import com.aifitnesscoach.feature.onboarding.OnboardingDraft
import com.aifitnesscoach.feature.widget.MotivationWorkoutWidget
import com.aifitnesscoach.feature.widget.QuickWorkoutWidget
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AndroidOverhaulDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun profileAndSettingsPersistAndCompletionClearsDraft() = runBlocking {
        withIsolatedPreferences { repository, reopened ->
            repository.saveOnboardingDraft(OnboardingDraft(name = "حمید", stepIndex = 2).encode())
            repository.setLanguage(AppLanguage.FA)
            repository.setThemeMode(AppThemeMode.DARK)
            repository.setMediaWifiOnly(true)
            assertEquals("حمید", OnboardingDraft.restore(reopened.preferencesFlow.first().onboardingDraft, UserProfile()).name)
            reopened.saveProfile(UserProfile(name = "حمید", heightCm = 179f))
            reopened.saveOnboardingDraft("late draft")
            val saved = reopened.preferencesFlow.first()
            assertTrue(saved.profile.hasCompletedOnboarding)
            assertEquals("حمید", saved.profile.name)
            assertEquals(179f, requireNotNull(saved.profile.heightCm), 0f)
            assertNull(saved.onboardingDraft)
            assertEquals(AppLanguage.FA, saved.language)
            assertEquals(AppThemeMode.DARK, saved.themeMode)
            assertTrue(saved.mediaWifiOnly)
        }
    }

    @Test
    fun widgetReadsOnlyPersistedWorkoutsWithSetsInsideTodaysRange() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(instrumentation.targetContext, AppDatabase::class.java).build()
        try {
            val dao = database.workoutDao()
            assertEquals(0, dao.getWidgetActivity(100L, 200_000L).workouts)
            val set = ExerciseSetEntity(
                sessionId = 0L, exerciseId = "bodyweight_squat", categoryName = "BODYWEIGHT_TRAINING",
                setNumber = 1, targetReps = 10, completedReps = 8, holdDurationMs = 0L, restDurationSec = 0
            )
            val session = WorkoutSessionEntity(
                clientRecordId = "today", titleEn = "Squat", titleFa = "اسکوات",
                startedAtEpochMs = 1_000L, endedAtEpochMs = 65_000L, totalDurationMs = 64_000L
            )
            dao.saveFullWorkout(session, listOf(set), emptyList())
            dao.saveFullWorkout(session.copy(clientRecordId = "empty"), emptyList(), emptyList())
            dao.saveFullWorkout(
                session.copy(clientRecordId = "tomorrow", endedAtEpochMs = 200_000L), listOf(set), emptyList()
            )
            val data = dao.getWidgetActivity(100L, 200_000L)
            assertEquals(1, data.workouts)
            assertEquals(64_000L, data.durationMs)
        } finally {
            database.close()
        }
    }

    @Test
    fun bothWidgetsRegisterAndNativePreviewsInflate() {
        val context = instrumentation.targetContext
        listOf(
            QuickWorkoutWidget::class.java to R.layout.fitness_widget,
            MotivationWorkoutWidget::class.java to R.layout.fitness_widget_motivation
        ).forEach { (receiver, layout) ->
            @Suppress("DEPRECATION")
            val info = context.packageManager.getReceiverInfo(
                ComponentName(context, receiver), PackageManager.GET_META_DATA
            )
            assertTrue(info.metaData.getInt(AppWidgetManager.META_DATA_APPWIDGET_PROVIDER) != 0)
            instrumentation.runOnMainSync {
                val view = RemoteViews(context.packageName, layout).apply(context, FrameLayout(context))
                assertNotNull(view.findViewById<android.view.View>(R.id.widget_quick_start))
            }
        }
    }

    @Test
    fun dailyQuoteIsPersistedAndSharedBySeparateRepositoryCallers() = runBlocking {
        withIsolatedPreferences { appRepository, widgetRepository ->
            val today = LocalDate.of(2026, 11, 22)
            val appQuote = appRepository.dailyQuote(today, VerifiedQuoteRepository())
            val widgetQuote = widgetRepository.dailyQuote(today, VerifiedQuoteRepository())
            assertNotNull(appQuote)
            assertEquals(appQuote, widgetQuote)
            assertEquals(appQuote?.id, appRepository.preferencesFlow.first().dailyQuoteId)
        }
    }

    @Test
    fun bothCatalogsAndThumbnailsWorkFromPackagedOfflineAssets() {
        val context = instrumentation.targetContext
        ExternalExerciseSources.load(context)
        val external = ExternalExerciseSources.exercises
        val hasan = external.filter { ExternalExerciseSources.entry(it.id)?.source == ExerciseSource.HASAN }
        val validated = external.filter { ExternalExerciseSources.entry(it.id)?.source == ExerciseSource.OPEN_EXERCISE_BASE }
        assertEquals(1_324, hasan.size)
        assertEquals(191, validated.size)
        assertEquals(1_554, ExerciseCatalog.allExercises.size)
        assertTrue(external.none { it.analyzerId != null })
        val openImage = ExerciseAssetCatalog.entryFor(validated.first().id)
        assertEquals(AssetMediaType.PNG_SINGLE, openImage.mediaType)
        assertTrue(requireNotNull(openImage.primaryUrl).contains(ExternalExerciseSources.OPEN_REVISION))
        assertTrue(hasan.none { ExerciseAssetCatalog.entryFor(it.id).primaryUrl.orEmpty().contains("gymvisual") })
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.assets.open(ExternalExerciseSources.ATLAS_PATH).use {
            BitmapFactory.decodeStream(it, null, options)
        }
        assertEquals(1_536, options.outWidth)
        validated.forEach {
            val photo = requireNotNull(ExternalExerciseSources.photo(it.id))
            assertTrue((photo.tile / 16 + 1) * 96 <= options.outHeight)
        }
    }

    @Test
    fun planDaysPersistIndependentlyAndCanBeCleared() = runBlocking {
        withIsolatedPreferences { write, read ->
            write.savePlanDay(1, "first")
            write.savePlanDay(7, "last")
            assertEquals(mapOf(1 to "first", 7 to "last"), read.preferencesFlow.first().weeklyPlan)
            write.savePlanDay(1, null)
            assertEquals(mapOf(7 to "last"), read.preferencesFlow.first().weeklyPlan)
        }
    }

    private suspend fun withIsolatedPreferences(
        block: suspend (UserPreferencesRepository, UserPreferencesRepository) -> Unit
    ) {
        val context = instrumentation.targetContext
        val file = File(context.cacheDir, "${UUID.randomUUID()}.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val store = PreferenceDataStoreFactory.create(scope = scope) { file }
            block(UserPreferencesRepository(context, store), UserPreferencesRepository(context, store))
        } finally {
            scope.cancel()
            file.delete()
        }
    }
}
