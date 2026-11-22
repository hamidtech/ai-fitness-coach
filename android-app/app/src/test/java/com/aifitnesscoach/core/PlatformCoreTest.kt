package com.aifitnesscoach.core

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.health.connect.client.records.ExerciseSessionRecord
import com.aifitnesscoach.core.database.AppLanguage
import com.aifitnesscoach.core.database.ExerciseSetEntity
import com.aifitnesscoach.core.database.UserGender
import com.aifitnesscoach.core.database.UserProfile
import com.aifitnesscoach.core.exercise.AnalyzerRegistry
import com.aifitnesscoach.core.exercise.ExerciseCategory
import com.aifitnesscoach.core.exercise.definitions.ExerciseCatalog
import com.aifitnesscoach.core.localization.LocaleManager
import com.aifitnesscoach.core.ui.theme.selectColorScheme
import com.aifitnesscoach.integration.healthconnect.HealthConnectManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformCoreTest {

    @Test
    fun `exercise catalog contains all 39 unique exercises across 4 categories and valid analyzers`() {
        assertEquals(39, ExerciseCatalog.allExercises.size)
        assertEquals(39, ExerciseCatalog.allExercises.map { it.id }.toSet().size)
        assertEquals(8, ExerciseCatalog.byCategory(ExerciseCategory.WEIGHTED_TRAINING).size)
        assertEquals(8, ExerciseCatalog.byCategory(ExerciseCategory.BODYWEIGHT_TRAINING).size)
        assertEquals(8, ExerciseCatalog.byCategory(ExerciseCategory.CORE_TRAINING).size)
        assertEquals(15, ExerciseCatalog.byCategory(ExerciseCategory.RESISTANCE_BAND_TRAINING).size)

        val supportedExercises = ExerciseCatalog.allExercises.filter { it.analyzerId != null }
        assertEquals(4, supportedExercises.size)
        supportedExercises.forEach { def ->
            assertTrue("Expected supported analyzer for ${def.id}", AnalyzerRegistry.isSupported(def.analyzerId))
            assertNotNull(AnalyzerRegistry.createForExercise(def))
        }
    }

    @Test
    fun `localization resolves RTL for Persian and LTR for English`() {
        assertEquals(LayoutDirection.Rtl, LocaleManager.layoutDirectionFor(AppLanguage.FA))
        assertEquals(LayoutDirection.Ltr, LocaleManager.layoutDirectionFor(AppLanguage.EN))
    }

    @Test
    fun `gender adaptive theme produces distinct primary colors per gender`() {
        val maleLight = selectColorScheme(UserGender.MALE, darkTheme = false)
        val femaleLight = selectColorScheme(UserGender.FEMALE, darkTheme = false)
        val neutralLight = selectColorScheme(UserGender.NEUTRAL, darkTheme = false)

        assertNotEquals(maleLight.primary, femaleLight.primary)
        assertNotEquals(femaleLight.primary, neutralLight.primary)
        assertNotEquals(maleLight.primary, neutralLight.primary)
        assertNotEquals(Color.Unspecified, maleLight.primary)
    }

    @Test
    fun `user profile calculates informational BMI from height cm and weight kg`() {
        val profile = UserProfile(
            name = "Sara",
            gender = UserGender.FEMALE,
            age = 28,
            heightCm = 165f,
            weightKg = 60f
        )
        val bmi = profile.bmi
        assertNotNull(bmi)
        assertEquals(22.04f, bmi!!, 0.1f)
    }

    @Test
    fun `health connect requests only permissions for enabled features`() {
        val none = HealthConnectManager.permissionsForFeatures(
            writeWorkouts = false,
            readWorkouts = false,
            readSteps = false,
            readSleep = false,
            readHeartRate = false
        )
        assertTrue(none.isEmpty())

        val stepsOnly = HealthConnectManager.permissionsForFeatures(
            writeWorkouts = false,
            readWorkouts = false,
            readSteps = true,
            readSleep = false,
            readHeartRate = false
        )
        assertEquals(setOf(HealthConnectManager.readStepsPermission), stepsOnly)

        val writeAndSleep = HealthConnectManager.permissionsForFeatures(
            writeWorkouts = true,
            readWorkouts = false,
            readSteps = false,
            readSleep = true,
            readHeartRate = false
        )
        assertEquals(
            setOf(
                HealthConnectManager.writeExercisePermission,
                HealthConnectManager.readSleepPermission
            ),
            writeAndSleep
        )
    }

    @Test
    fun `health connect maps local exercise categories to official workout types`() {
        val baseSet = ExerciseSetEntity(
            sessionId = 1L,
            exerciseId = "bodyweight_squat",
            categoryName = ExerciseCategory.BODYWEIGHT_TRAINING.name,
            setNumber = 1,
            targetReps = 10,
            completedReps = 10,
            holdDurationMs = 0L,
            restDurationSec = 60
        )
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS,
            HealthConnectManager.mapExerciseType(listOf(baseSet))
        )
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
            HealthConnectManager.mapExerciseType(
                listOf(baseSet.copy(exerciseId = "band_biceps_curl", categoryName = ExerciseCategory.RESISTANCE_BAND_TRAINING.name))
            )
        )
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT,
            HealthConnectManager.mapExerciseType(emptyList())
        )
    }
}
