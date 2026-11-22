package com.aifitnesscoach.core.personalization

import com.aifitnesscoach.core.database.ExperienceLevel
import com.aifitnesscoach.core.database.FitnessGoal
import com.aifitnesscoach.core.database.MeasurementUnit
import com.aifitnesscoach.core.database.UserGender
import com.aifitnesscoach.core.database.UserProfile
import java.time.LocalTime
import kotlin.math.roundToInt

enum class TimeOfDay {
    MORNING,
    AFTERNOON,
    EVENING,
    NIGHT
}

object PersonalizedGreeting {

    fun resolveTimeOfDay(hour24: Int = LocalTime.now().hour): TimeOfDay {
        val h = ((hour24 % 24) + 24) % 24
        return when (h) {
            in 5..11 -> TimeOfDay.MORNING
            in 12..16 -> TimeOfDay.AFTERNOON
            in 17..20 -> TimeOfDay.EVENING
            else -> TimeOfDay.NIGHT
        }
    }

    fun sanitizeName(rawName: String?): String? {
        val cleaned = rawName?.trim()?.replace(Regex("\\s+"), " ") ?: return null
        return cleaned.takeIf { it.isNotEmpty() }
    }

    fun homeGreeting(
        name: String?,
        isPersian: Boolean,
        hour24: Int = LocalTime.now().hour
    ): String {
        val cleanName = sanitizeName(name)
        val tod = resolveTimeOfDay(hour24)
        return if (isPersian) {
            when (tod) {
                TimeOfDay.MORNING -> if (cleanName != null) "صبح بخیر، $cleanName" else "صبح بخیر"
                TimeOfDay.AFTERNOON -> if (cleanName != null) "ظهر بخیر، $cleanName" else "ظهر بخیر"
                TimeOfDay.EVENING -> if (cleanName != null) "عصر بخیر، $cleanName" else "عصر بخیر"
                TimeOfDay.NIGHT -> if (cleanName != null) "شب بخیر، $cleanName" else "شب بخیر"
            }
        } else {
            when (tod) {
                TimeOfDay.MORNING -> if (cleanName != null) "Good morning, $cleanName" else "Good morning"
                TimeOfDay.AFTERNOON -> if (cleanName != null) "Good afternoon, $cleanName" else "Good afternoon"
                TimeOfDay.EVENING -> if (cleanName != null) "Good evening, $cleanName" else "Good evening"
                TimeOfDay.NIGHT -> if (cleanName != null) "Late-night training, $cleanName" else "Ready for a late session?"
            }
        }
    }

    fun readyPrompt(
        name: String?,
        isPersian: Boolean,
        hasWorkouts: Boolean
    ): String {
        val cleanName = sanitizeName(name)
        return if (isPersian) {
            if (hasWorkouts) {
                if (cleanName != null) "آماده تمرین امروز هستی، $cleanName؟" else "آماده تمرین امروز هستید؟"
            } else {
                if (cleanName != null) "$cleanName، بیایید اولین تمرین خود را ثبت کنیم." else "بیایید اولین تمرین خود را ثبت کنید."
            }
        } else {
            if (hasWorkouts) {
                if (cleanName != null) "Ready for today's session, $cleanName?" else "Ready for today's session?"
            } else {
                if (cleanName != null) "Let's log your first workout, $cleanName." else "Let's log your first workout."
            }
        }
    }

    fun workoutCompletionMessage(
        name: String?,
        setsCompleted: Int,
        isPersian: Boolean
    ): String {
        val cleanName = sanitizeName(name)
        return if (isPersian) {
            if (cleanName != null) {
                "عالی بود، $cleanName — $setsCompleted ست ثبت شد."
            } else {
                "جلسه تمرین تکمیل شد — $setsCompleted ست ثبت شد."
            }
        } else {
            val noun = if (setsCompleted == 1) "set" else "sets"
            if (cleanName != null) {
                "Nice work, $cleanName — $setsCompleted $noun completed."
            } else {
                "Session complete — $setsCompleted $noun logged."
            }
        }
    }

    fun dailyMotivationIntro(
        name: String?,
        isPersian: Boolean
    ): String {
        val cleanName = sanitizeName(name)
        return if (isPersian) {
            if (cleanName != null) "انگیزه امروز برای $cleanName" else "الهام روزانه ورزشی"
        } else {
            if (cleanName != null) "Daily motivation for $cleanName" else "Daily Athlete Motivation"
        }
    }
}

enum class ProfileValidationError {
    INVALID_AGE,
    INVALID_BIRTH_YEAR,
    INVALID_HEIGHT,
    INVALID_WEIGHT,
    CUSTOM_GOAL_EMPTY
}

sealed class ProfileValidationResult {
    data class Valid(val profile: UserProfile) : ProfileValidationResult()
    data class Invalid(val errors: Set<ProfileValidationError>) : ProfileValidationResult()
}

object ProfileValidator {

    private const val CM_PER_INCH = 2.54f
    private const val LBS_PER_KG = 2.2046226f

    fun cmToInches(cm: Float): Float = cm / CM_PER_INCH
    fun inchesToCm(inches: Float): Float = inches * CM_PER_INCH

    fun kgToLbs(kg: Float): Float = kg * LBS_PER_KG
    fun lbsToKg(lbs: Float): Float = lbs / LBS_PER_KG

    fun formatHeightForInput(heightCm: Float?, unit: MeasurementUnit): String {
        if (heightCm == null) return ""
        return when (unit) {
            MeasurementUnit.METRIC -> heightCm.roundToInt().toString()
            MeasurementUnit.IMPERIAL -> cmToInches(heightCm).roundToInt().toString()
        }
    }

    fun formatWeightForInput(weightKg: Float?, unit: MeasurementUnit): String {
        if (weightKg == null) return ""
        return when (unit) {
            MeasurementUnit.METRIC -> formatCleanDecimal(weightKg)
            MeasurementUnit.IMPERIAL -> formatCleanDecimal(kgToLbs(weightKg))
        }
    }

    private fun formatCleanDecimal(value: Float): String {
        val roundedTenth = (value * 10f).roundToInt() / 10f
        return if (roundedTenth % 1f == 0f) {
            roundedTenth.toInt().toString()
        } else {
            roundedTenth.toString()
        }
    }

    fun validateAndBuildProfile(
        existingProfile: UserProfile,
        nameInput: String,
        gender: UserGender,
        useBirthYear: Boolean,
        ageInput: String,
        birthYearInput: String,
        heightInput: String,
        weightInput: String,
        measurementUnit: MeasurementUnit,
        fitnessGoal: FitnessGoal,
        customGoalLabel: String,
        experienceLevel: ExperienceLevel,
        currentYear: Int = java.time.Year.now().value
    ): ProfileValidationResult {
        val errors = linkedSetOf<ProfileValidationError>()

        var resolvedAge: Int? = null
        var resolvedBirthYear: Int? = null

        if (useBirthYear) {
            val trimmedBy = birthYearInput.trim()
            if (trimmedBy.isNotEmpty()) {
                val parsedBy = trimmedBy.toIntOrNull()
                val minYear = currentYear - 110
                val maxYear = currentYear - 10
                if (parsedBy == null || parsedBy !in minYear..maxYear) {
                    errors.add(ProfileValidationError.INVALID_BIRTH_YEAR)
                } else {
                    resolvedBirthYear = parsedBy
                    resolvedAge = (currentYear - parsedBy).coerceIn(10, 110)
                }
            }
        } else {
            val trimmedAge = ageInput.trim()
            if (trimmedAge.isNotEmpty()) {
                val parsedAge = trimmedAge.toIntOrNull()
                if (parsedAge == null || parsedAge !in 10..110) {
                    errors.add(ProfileValidationError.INVALID_AGE)
                } else {
                    resolvedAge = parsedAge
                    resolvedBirthYear = null
                }
            }
        }

        val resolvedHeightCm: Float? = heightInput.trim().let { raw ->
            if (raw.isEmpty()) {
                null
            } else {
                val numeric = raw.toFloatOrNull()
                if (numeric == null) {
                    errors.add(ProfileValidationError.INVALID_HEIGHT)
                    null
                } else {
                    val inCm = when (measurementUnit) {
                        MeasurementUnit.METRIC -> numeric
                        MeasurementUnit.IMPERIAL -> inchesToCm(numeric)
                    }
                    if (inCm !in 90f..250f) {
                        errors.add(ProfileValidationError.INVALID_HEIGHT)
                        null
                    } else {
                        inCm
                    }
                }
            }
        }

        val resolvedWeightKg: Float? = weightInput.trim().let { raw ->
            if (raw.isEmpty()) {
                null
            } else {
                val numeric = raw.toFloatOrNull()
                if (numeric == null) {
                    errors.add(ProfileValidationError.INVALID_WEIGHT)
                    null
                } else {
                    val inKg = when (measurementUnit) {
                        MeasurementUnit.METRIC -> numeric
                        MeasurementUnit.IMPERIAL -> lbsToKg(numeric)
                    }
                    if (inKg !in 25f..300f) {
                        errors.add(ProfileValidationError.INVALID_WEIGHT)
                        null
                    } else {
                        inKg
                    }
                }
            }
        }

        val cleanedCustomGoal = if (fitnessGoal == FitnessGoal.CUSTOM) customGoalLabel.trim() else ""
        if (fitnessGoal == FitnessGoal.CUSTOM && cleanedCustomGoal.isEmpty()) {
            errors.add(ProfileValidationError.CUSTOM_GOAL_EMPTY)
        }

        if (errors.isNotEmpty()) {
            return ProfileValidationResult.Invalid(errors)
        }

        return ProfileValidationResult.Valid(
            existingProfile.copy(
                name = nameInput.trim().replace(Regex("\\s+"), " "),
                gender = gender,
                age = resolvedAge,
                birthYear = resolvedBirthYear,
                heightCm = resolvedHeightCm,
                weightKg = resolvedWeightKg,
                fitnessGoal = fitnessGoal,
                customGoalLabel = cleanedCustomGoal,
                experienceLevel = experienceLevel,
                hasCompletedOnboarding = true
            )
        )
    }
}
