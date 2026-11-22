package com.aifitnesscoach.core

import com.aifitnesscoach.app.MainDestination
import com.aifitnesscoach.app.routeForLaunchExtra
import com.aifitnesscoach.app.startDestination
import com.aifitnesscoach.core.camera.CameraPermissionState
import com.aifitnesscoach.core.camera.WorkoutCameraState
import com.aifitnesscoach.core.camera.cameraPermissionState
import com.aifitnesscoach.core.camera.workoutCameraState
import com.aifitnesscoach.core.database.ExerciseAssetEntity
import com.aifitnesscoach.core.database.UserProfile
import com.aifitnesscoach.core.exercise.definitions.ExerciseCatalog
import com.aifitnesscoach.core.media.AssetDownloadStatus
import com.aifitnesscoach.core.media.AssetMediaType
import com.aifitnesscoach.core.media.ExerciseAssetCatalog
import com.aifitnesscoach.core.media.illustratedExerciseIds
import com.aifitnesscoach.core.media.resolveEffectiveStatus
import com.aifitnesscoach.core.personalization.ProfileValidationResult
import com.aifitnesscoach.feature.onboarding.OnboardingDraft
import com.aifitnesscoach.feature.onboarding.OnboardingStep
import com.aifitnesscoach.feature.widget.WidgetActivity
import com.aifitnesscoach.feature.widget.WidgetDay
import com.aifitnesscoach.feature.widget.WorkoutWidgetRoutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class AndroidOverhaulTest {
    @Test
    fun onboardingRestoresStepAndFields() {
        val draft = OnboardingDraft(OnboardingStep.GOALS.ordinal, name = "حمید", weight = "101.4")
        val restored = OnboardingDraft.restore(draft.encode(), UserProfile())
        assertEquals(draft, restored)
        assertEquals(OnboardingStep.METRICS, restored.step.previous())
        assertEquals(OnboardingStep.PRIVACY, restored.step.next())
        assertEquals(OnboardingStep.WELCOME, OnboardingStep.WELCOME.previous())
        assertEquals(OnboardingStep.REVIEW, OnboardingStep.REVIEW.next())
    }

    @Test
    fun emptyOptionalProfileIsValidAndCompletedProfilesGoHome() {
        assertTrue(OnboardingDraft().validate(UserProfile()) is ProfileValidationResult.Valid)
        assertTrue(OnboardingDraft(height = "invalid").validate(UserProfile()) is ProfileValidationResult.Invalid)
        assertEquals(MainDestination.Onboarding.route, startDestination(UserProfile()))
        assertEquals(MainDestination.Home.route, startDestination(UserProfile(hasCompletedOnboarding = true)))
    }

    @Test
    fun exercisesHaveOfflineArtAndVerifiedDownloadMetadata() {
        assertEquals(ExerciseCatalog.allExercises.map { it.id }.toSet(), illustratedExerciseIds)
        val curl = ExerciseAssetCatalog.entryFor("band_biceps_curl")
        assertEquals("banded-standing-curl", curl.sourceSlug)
        assertEquals(22346L, curl.expectedBytes)
        ExerciseAssetCatalog.entriesByExerciseId.values.filter { it.primaryUrl != null }.forEach {
            assertTrue(requireNotNull(it.expectedBytes) > 0)
            assertTrue(requireNotNull(it.licenseUrl).startsWith("https://"))
        }
    }

    @Test
    fun incompletePairsFallBackAndDownloadFailuresRemainRetryable() {
        val manifest = ExerciseAssetCatalog.entryFor("bodyweight_squat")
        val downloaded = ExerciseAssetEntity(
            exerciseId = manifest.exerciseId,
            sourceId = manifest.sourceType.name,
            status = AssetDownloadStatus.DOWNLOADED.name,
            version = manifest.version,
            localPrimaryPath = "start.webp",
            localSecondaryPath = "peak.webp",
            mediaType = AssetMediaType.WEBP_IMAGE_PAIR.name
        )
        assertEquals(AssetDownloadStatus.AVAILABLE, resolveEffectiveStatus(manifest, downloaded) { it == "start.webp" })
        assertEquals(AssetDownloadStatus.DOWNLOADED, resolveEffectiveStatus(manifest, downloaded) { true })
        val failed = downloaded.copy(status = AssetDownloadStatus.FAILED.name)
        assertEquals(AssetDownloadStatus.FAILED, resolveEffectiveStatus(manifest, failed))
    }

    @Test
    fun cameraPermissionAndStartupHaveIndependentStates() {
        assertEquals(CameraPermissionState.REQUIRED, cameraPermissionState(false, false, false))
        assertEquals(CameraPermissionState.DENIED, cameraPermissionState(false, true, true))
        assertEquals(CameraPermissionState.SETTINGS_REQUIRED, cameraPermissionState(false, true, false))
        assertEquals(CameraPermissionState.GRANTED, cameraPermissionState(true, true, false))
        assertEquals(WorkoutCameraState.INITIALIZING_CAMERA, workoutCameraState(false, true, true, null))
        assertEquals(WorkoutCameraState.INITIALIZING_POSE, workoutCameraState(true, true, false, null))
        assertEquals(WorkoutCameraState.WORKOUT_READY, workoutCameraState(true, true, true, null))
        assertEquals(WorkoutCameraState.CAMERA_READY, workoutCameraState(true, false, false, null))
        assertEquals(
            WorkoutCameraState.PREVIEW_ERROR,
            workoutCameraState(false, true, false, WorkoutCameraState.PREVIEW_ERROR)
        )
        assertEquals(
            WorkoutCameraState.POSE_ERROR,
            workoutCameraState(true, true, false, WorkoutCameraState.POSE_ERROR)
        )
    }

    @Test
    fun widgetDayHandlesDstAndDeepLinksAreAllowlisted() {
        val spring = WidgetDay.of(LocalDate.of(2026, 3, 8), ZoneId.of("America/New_York"))
        assertEquals(23L * 60 * 60 * 1000, spring.endMs - spring.startMs)
        assertEquals(0L, WidgetActivity(0, 0).activeMinutes)
        assertEquals(2L, WidgetActivity(1, 61000).activeMinutes)
        assertEquals(MainDestination.Exercises.route, routeForLaunchExtra(WorkoutWidgetRoutes.QUICK_START))
        assertEquals(MainDestination.History.route, routeForLaunchExtra(WorkoutWidgetRoutes.ACTIVITY))
        assertEquals(MainDestination.Home.route, routeForLaunchExtra(WorkoutWidgetRoutes.MOTIVATION))
        assertNull(routeForLaunchExtra("workout"))
    }
}
