package com.aifitnesscoach.feature.onboarding

import com.aifitnesscoach.core.database.ExperienceLevel
import com.aifitnesscoach.core.database.FitnessGoal
import com.aifitnesscoach.core.database.MeasurementUnit
import com.aifitnesscoach.core.database.UserGender
import com.aifitnesscoach.core.database.UserProfile
import com.aifitnesscoach.core.personalization.ProfileValidator
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

enum class OnboardingStep {
    WELCOME, LANGUAGE, IDENTITY, METRICS, GOALS, PRIVACY, REVIEW;

    fun next() = entries[(ordinal + 1).coerceAtMost(entries.lastIndex)]
    fun previous() = entries[(ordinal - 1).coerceAtLeast(0)]
}

data class OnboardingDraft(
    @SerializedName("step") val stepIndex: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("gender") val gender: String = UserGender.NEUTRAL.name,
    @SerializedName("age") val age: String = "",
    @SerializedName("height") val height: String = "",
    @SerializedName("weight") val weight: String = "",
    @SerializedName("goal") val goal: String = FitnessGoal.GENERAL_FITNESS.name,
    @SerializedName("custom") val custom: String = "",
    @SerializedName("experience") val experience: String = ExperienceLevel.BEGINNER.name
) {
    val step get() = OnboardingStep.entries[stepIndex.coerceIn(0, OnboardingStep.entries.lastIndex)]

    fun validate(initialProfile: UserProfile) = ProfileValidator.validateAndBuildProfile(
        existingProfile = initialProfile,
        nameInput = name,
        gender = UserGender.entries.firstOrNull { it.name == gender } ?: UserGender.NEUTRAL,
        useBirthYear = initialProfile.birthYear != null && age == initialProfile.age?.toString().orEmpty(),
        ageInput = age,
        birthYearInput = initialProfile.birthYear?.toString().orEmpty(),
        heightInput = height,
        weightInput = weight,
        measurementUnit = MeasurementUnit.METRIC,
        fitnessGoal = FitnessGoal.entries.firstOrNull { it.name == goal } ?: FitnessGoal.GENERAL_FITNESS,
        customGoalLabel = custom,
        experienceLevel = ExperienceLevel.entries.firstOrNull { it.name == experience } ?: ExperienceLevel.BEGINNER
    )

    fun encode(): String = Gson().toJson(this)

    companion object {
        fun restore(encoded: String?, initial: UserProfile): OnboardingDraft {
            val restored = encoded?.let { runCatching { Gson().fromJson(it, OnboardingDraft::class.java) }.getOrNull() }
            return restored ?: OnboardingDraft(
                name = initial.name,
                gender = initial.gender.name,
                age = initial.age?.toString().orEmpty(),
                height = initial.heightCm?.toString().orEmpty(),
                weight = initial.weightKg?.toString().orEmpty(),
                goal = initial.fitnessGoal.name,
                custom = initial.customGoalLabel,
                experience = initial.experienceLevel.name
            )
        }
    }
}
