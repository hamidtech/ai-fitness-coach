package com.aifitnesscoach.core

import androidx.compose.ui.graphics.Color
import com.aifitnesscoach.core.database.AppColorPalette
import com.aifitnesscoach.core.database.ExperienceLevel
import com.aifitnesscoach.core.database.FitnessGoal
import com.aifitnesscoach.core.database.MeasurementUnit
import com.aifitnesscoach.core.database.UserGender
import com.aifitnesscoach.core.database.UserProfile
import com.aifitnesscoach.core.motivation.DailyQuoteSelector
import com.aifitnesscoach.core.motivation.MotivationalQuote
import com.aifitnesscoach.core.motivation.QuoteVerificationStatus
import com.aifitnesscoach.core.motivation.VerifiedQuoteRepository
import com.aifitnesscoach.core.personalization.PersonalizedGreeting
import com.aifitnesscoach.core.personalization.ProfileValidationError
import com.aifitnesscoach.core.personalization.ProfileValidationResult
import com.aifitnesscoach.core.personalization.ProfileValidator
import com.aifitnesscoach.core.personalization.TimeOfDay
import com.aifitnesscoach.core.ui.theme.resolveEffectivePalette
import com.aifitnesscoach.core.ui.theme.selectColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PersonalizationAndMotivationTest {

    @Test
    fun `profile validator edits metrics in metric and imperial units and preserves onboarding`() {
        val existing = UserProfile(
            name = "Sara",
            gender = UserGender.FEMALE,
            age = 27,
            heightCm = 165f,
            weightKg = 60f,
            fitnessGoal = FitnessGoal.GENERAL_FITNESS,
            experienceLevel = ExperienceLevel.BEGINNER,
            hasCompletedOnboarding = true
        )

        val imperialResult = ProfileValidator.validateAndBuildProfile(
            existingProfile = existing,
            nameInput = "  Sara Ahmadi  ",
            gender = UserGender.FEMALE,
            useBirthYear = true,
            ageInput = "",
            birthYearInput = "1996",
            heightInput = "70",
            weightInput = "154",
            measurementUnit = MeasurementUnit.IMPERIAL,
            fitnessGoal = FitnessGoal.STRENGTH,
            customGoalLabel = "",
            experienceLevel = ExperienceLevel.INTERMEDIATE,
            currentYear = 2026
        )

        assertTrue(imperialResult is ProfileValidationResult.Valid)
        val updated = (imperialResult as ProfileValidationResult.Valid).profile
        assertEquals("Sara Ahmadi", updated.name)
        assertEquals(30, updated.age)
        assertEquals(1996, updated.birthYear)
        assertEquals(177.8f, updated.heightCm!!, 0.2f)
        assertEquals(69.85f, updated.weightKg!!, 0.2f)
        assertEquals(FitnessGoal.STRENGTH, updated.fitnessGoal)
        assertEquals(ExperienceLevel.INTERMEDIATE, updated.experienceLevel)
        assertTrue(updated.hasCompletedOnboarding)
    }

    @Test
    fun `profile validator cleanly clears optional fields and rejects invalid ranges`() {
        val existing = UserProfile(
            name = "Reza",
            gender = UserGender.MALE,
            age = 31,
            birthYear = 1995,
            heightCm = 180f,
            weightKg = 80f,
            hasCompletedOnboarding = true
        )

        val clearedResult = ProfileValidator.validateAndBuildProfile(
            existingProfile = existing,
            nameInput = "Reza",
            gender = UserGender.NEUTRAL,
            useBirthYear = false,
            ageInput = "",
            birthYearInput = "",
            heightInput = "",
            weightInput = "",
            measurementUnit = MeasurementUnit.METRIC,
            fitnessGoal = FitnessGoal.MOBILITY,
            customGoalLabel = "",
            experienceLevel = ExperienceLevel.ADVANCED
        )
        assertTrue(clearedResult is ProfileValidationResult.Valid)
        val cleared = (clearedResult as ProfileValidationResult.Valid).profile
        assertNull(cleared.age)
        assertNull(cleared.birthYear)
        assertNull(cleared.heightCm)
        assertNull(cleared.weightKg)
        assertEquals(UserGender.NEUTRAL, cleared.gender)

        val invalidResult = ProfileValidator.validateAndBuildProfile(
            existingProfile = existing,
            nameInput = "Reza",
            gender = UserGender.MALE,
            useBirthYear = false,
            ageInput = "5",
            birthYearInput = "",
            heightInput = "400",
            weightInput = "10",
            measurementUnit = MeasurementUnit.METRIC,
            fitnessGoal = FitnessGoal.CUSTOM,
            customGoalLabel = "   ",
            experienceLevel = ExperienceLevel.BEGINNER
        )
        assertTrue(invalidResult is ProfileValidationResult.Invalid)
        val errors = (invalidResult as ProfileValidationResult.Invalid).errors
        assertTrue(ProfileValidationError.INVALID_AGE in errors)
        assertTrue(ProfileValidationError.INVALID_HEIGHT in errors)
        assertTrue(ProfileValidationError.INVALID_WEIGHT in errors)
        assertTrue(ProfileValidationError.CUSTOM_GOAL_EMPTY in errors)
    }

    @Test
    fun `theme resolution handles auto gender suggestion manual override and light dark schemes`() {
        assertEquals(AppColorPalette.ATHLETIC_COBALT, resolveEffectivePalette(AppColorPalette.AUTO, UserGender.MALE))
        assertEquals(AppColorPalette.VIOLET, resolveEffectivePalette(AppColorPalette.AUTO, UserGender.FEMALE))
        assertEquals(AppColorPalette.TEAL, resolveEffectivePalette(AppColorPalette.AUTO, UserGender.NEUTRAL))

        // Manual override stays pinned even when gender changes
        assertEquals(AppColorPalette.TEAL, resolveEffectivePalette(AppColorPalette.TEAL, UserGender.MALE))
        assertEquals(AppColorPalette.ATHLETIC_COBALT, resolveEffectivePalette(AppColorPalette.ATHLETIC_COBALT, UserGender.FEMALE))
        assertEquals(AppColorPalette.NEUTRAL, resolveEffectivePalette(AppColorPalette.NEUTRAL, UserGender.FEMALE))

        for (palette in AppColorPalette.entries) {
            val light = selectColorScheme(UserGender.MALE, darkTheme = false, palettePreference = palette)
            val dark = selectColorScheme(UserGender.MALE, darkTheme = true, palettePreference = palette)
            assertNotEquals(Color.Unspecified, light.primary)
            assertNotEquals(Color.Unspecified, dark.primary)
            assertNotEquals(light.background, dark.background)
        }
    }

    @Test
    fun `personalized greetings respect time of day bilingual output and blank name fallback`() {
        assertEquals(TimeOfDay.MORNING, PersonalizedGreeting.resolveTimeOfDay(8))
        assertEquals(TimeOfDay.AFTERNOON, PersonalizedGreeting.resolveTimeOfDay(14))
        assertEquals(TimeOfDay.EVENING, PersonalizedGreeting.resolveTimeOfDay(19))
        assertEquals(TimeOfDay.NIGHT, PersonalizedGreeting.resolveTimeOfDay(23))

        val enMorning = PersonalizedGreeting.homeGreeting("Mina", isPersian = false, hour24 = 8)
        val faEvening = PersonalizedGreeting.homeGreeting("سارا", isPersian = true, hour24 = 19)
        assertTrue(enMorning.contains("Mina"))
        assertTrue(enMorning.contains("Good morning", ignoreCase = true))
        assertTrue(faEvening.contains("سارا"))
        assertTrue(faEvening.contains("عصر بخیر"))

        val enBlank = PersonalizedGreeting.homeGreeting("   ", isPersian = false, hour24 = 14)
        val faBlank = PersonalizedGreeting.homeGreeting("", isPersian = true, hour24 = 7)
        assertFalse(enBlank.contains(","))
        assertFalse(enBlank.contains("Athlete", ignoreCase = true))
        assertFalse(faBlank.contains("،"))

        val completionEn = PersonalizedGreeting.workoutCompletionMessage("Ali", setsCompleted = 3, isPersian = false)
        assertTrue(completionEn.contains("Ali"))
        assertTrue(completionEn.contains("3"))
    }

    @Test
    fun `verified quote repository excludes unverified entries and daily selector rotates cleanly`() {
        val builtInRepo = VerifiedQuoteRepository()
        val quotes = builtInRepo.getAllVerifiedQuotes()
        assertEquals(12, quotes.size)
        quotes.forEach { q ->
            assertEquals(QuoteVerificationStatus.VERIFIED, q.verificationStatus)
            assertTrue(q.quoteTextEn.isNotBlank())
            assertTrue(q.author.isNotBlank())
            assertTrue(q.attribution.isNotBlank())
        }

        // Unverified or incomplete quotes are strictly excluded
        val mixedRepo = VerifiedQuoteRepository(
            listOf(
                quotes.first(),
                MotivationalQuote(
                    id = "unverified_1",
                    quoteTextEn = "Unverified sample",
                    author = "Unknown",
                    sourceOrganization = "None",
                    sourceTitle = "None",
                    attribution = "None",
                    verificationStatus = QuoteVerificationStatus.UNVERIFIED
                )
            )
        )
        assertEquals(1, mixedRepo.getAllVerifiedQuotes().size)

        // Same day stays stable across restarts
        val day1 = LocalDate.of(2026, 6, 10)
        val firstPick = DailyQuoteSelector.selectForDate(
            repository = builtInRepo,
            today = day1,
            savedDateIso = null,
            savedQuoteId = null,
            recentIds = emptyList()
        )
        assertNotNull(firstPick.quote)
        assertTrue(firstPick.didChange)

        val restartSameDay = DailyQuoteSelector.selectForDate(
            repository = builtInRepo,
            today = day1,
            savedDateIso = firstPick.dateIso,
            savedQuoteId = firstPick.quote!!.id,
            recentIds = firstPick.updatedRecentIds
        )
        assertEquals(firstPick.quote!!.id, restartSameDay.quote!!.id)
        assertFalse(restartSameDay.didChange)

        // Next day rotates without immediate repeat
        val day2 = day1.plusDays(1)
        val secondPick = DailyQuoteSelector.selectForDate(
            repository = builtInRepo,
            today = day2,
            savedDateIso = firstPick.dateIso,
            savedQuoteId = firstPick.quote!!.id,
            recentIds = firstPick.updatedRecentIds
        )
        assertNotNull(secondPick.quote)
        assertNotEquals(firstPick.quote!!.id, secondPick.quote!!.id)
        assertTrue(secondPick.didChange)

        // Empty verified catalog handles unavailable state gracefully
        val emptyRepo = VerifiedQuoteRepository(emptyList())
        val emptyPick = DailyQuoteSelector.selectForDate(
            repository = emptyRepo,
            today = day1,
            savedDateIso = null,
            savedQuoteId = null,
            recentIds = emptyList()
        )
        assertNull(emptyPick.quote)
        assertFalse(emptyPick.didChange)
    }

    @Test
    fun `structured AI payload includes sanitized athlete name only when present`() {
        // Minimal unit verification on PersonalizedGreeting.sanitizeName contract used in AIAnalysisCoordinator
        assertEquals("Nima", PersonalizedGreeting.sanitizeName("  Nima  "))
        assertNull(PersonalizedGreeting.sanitizeName("   "))
        assertNull(PersonalizedGreeting.sanitizeName(null))
    }
}
