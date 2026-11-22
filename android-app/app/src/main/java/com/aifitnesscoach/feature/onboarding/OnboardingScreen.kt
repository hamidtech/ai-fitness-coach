package com.aifitnesscoach.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aifitnesscoach.R
import com.aifitnesscoach.core.database.AppLanguage
import com.aifitnesscoach.core.database.ExperienceLevel
import com.aifitnesscoach.core.database.FitnessGoal
import com.aifitnesscoach.core.database.UserGender
import com.aifitnesscoach.core.database.UserProfile
import com.aifitnesscoach.core.personalization.ProfileValidationError
import com.aifitnesscoach.core.personalization.ProfileValidationResult
import com.aifitnesscoach.core.ui.LayoutSpacing

@Composable
fun OnboardingScreen(
    initialProfile: UserProfile,
    initialLanguage: AppLanguage,
    savedDraft: String?,
    onSaveDraft: suspend (String) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onCompleteOnboarding: suspend (UserProfile) -> Unit
) {
    var encoded by rememberSaveable { mutableStateOf(savedDraft) }
    val draft = remember(encoded) { OnboardingDraft.restore(encoded, initialProfile) }
    var completed by remember { mutableStateOf<UserProfile?>(null) }
    LaunchedEffect(encoded) { encoded?.let { onSaveDraft(it) } }
    LaunchedEffect(completed) {
        completed?.let { onCompleteOnboarding(it) }
    }

    fun update(value: OnboardingDraft) { encoded = value.encode() }
    fun goTo(step: OnboardingStep) { update(draft.copy(stepIndex = step.ordinal)) }

    val step = draft.step
    val (title, description, icon) = when (step) {
        OnboardingStep.WELCOME -> Triple(R.string.onboarding_welcome_title, R.string.onboarding_welcome_body, Icons.Outlined.FitnessCenter)
        OnboardingStep.LANGUAGE -> Triple(R.string.onboarding_language_title, R.string.onboarding_language_subtitle, Icons.Outlined.Language)
        OnboardingStep.IDENTITY -> Triple(R.string.wizard_identity, R.string.wizard_identity_body, Icons.Outlined.PersonOutline)
        OnboardingStep.METRICS -> Triple(R.string.onboarding_optional_body_metrics, R.string.wizard_metrics_body, Icons.Outlined.Straighten)
        OnboardingStep.GOALS -> Triple(R.string.wizard_goals, R.string.wizard_goals_body, Icons.Outlined.Flag)
        OnboardingStep.PRIVACY -> Triple(R.string.onboarding_privacy_title, R.string.wizard_privacy_body, Icons.Outlined.Lock)
        OnboardingStep.REVIEW -> Triple(R.string.wizard_review, R.string.wizard_review_body, Icons.Outlined.CheckCircle)
    }
    val validation = draft.validate(initialProfile)
    val errors = (validation as? ProfileValidationResult.Invalid)?.errors.orEmpty()
    val error = when {
        step == OnboardingStep.METRICS && errors.any { it != ProfileValidationError.CUSTOM_GOAL_EMPTY } ->
            R.string.profile_validation_error
        step == OnboardingStep.GOALS && ProfileValidationError.CUSTOM_GOAL_EMPTY in errors ->
            R.string.profile_custom_goal_error
        step == OnboardingStep.REVIEW && errors.isNotEmpty() -> R.string.wizard_review_error
        else -> null
    }
    BackHandler(step != OnboardingStep.WELCOME && completed == null) { goTo(step.previous()) }

    Scaffold(
        modifier = Modifier.imePadding(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(Modifier.fillMaxWidth().padding(LayoutSpacing.screen)) {
                    error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
                    Row(horizontalArrangement = Arrangement.spacedBy(LayoutSpacing.small)) {
                        if (step != OnboardingStep.WELCOME) {
                            TextButton(
                                enabled = completed == null,
                                onClick = { goTo(step.previous()) }
                            ) { Text(stringResource(R.string.common_back)) }
                        }
                        Button(
                            modifier = Modifier.weight(1f).sizeIn(minHeight = LayoutSpacing.touchTarget),
                            enabled = completed == null && error == null,
                            onClick = {
                                if (step == OnboardingStep.REVIEW) {
                                    completed = (validation as? ProfileValidationResult.Valid)?.profile
                                } else {
                                    goTo(step.next())
                                }
                            }
                        ) {
                            Text(stringResource(when (step) {
                                OnboardingStep.WELCOME -> R.string.onboarding_get_started
                                OnboardingStep.REVIEW -> R.string.onboarding_finish
                                else -> R.string.onboarding_continue
                            }))
                        }
                    }
                }
            }
        }
    ) { insets ->
        key(step) {
            Column(
                Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState())
                    .padding(LayoutSpacing.screen),
                verticalArrangement = Arrangement.spacedBy(LayoutSpacing.screen)
            ) {
                Text(stringResource(R.string.wizard_progress, step.ordinal + 1, OnboardingStep.entries.size))
                LinearProgressIndicator(
                    progress = { (step.ordinal + 1).toFloat() / OnboardingStep.entries.size },
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    Modifier.fillMaxWidth().sizeIn(minHeight = 114.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(64.dp))
                }
                Text(stringResource(title), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(description), style = MaterialTheme.typography.bodyLarge)
                when (step) {
                    OnboardingStep.WELCOME, OnboardingStep.PRIVACY -> Unit
                    OnboardingStep.LANGUAGE -> WizardChoices(
                        initialLanguage,
                        listOf(
                            AppLanguage.SYSTEM to R.string.lang_system,
                            AppLanguage.EN to R.string.lang_en,
                            AppLanguage.FA to R.string.lang_fa
                        ),
                        onLanguageSelected
                    )
                    OnboardingStep.IDENTITY -> {
                        WizardField(R.string.profile_name_label, draft.name) { update(draft.copy(name = it.take(60))) }
                        Text(stringResource(R.string.profile_gender_optional_label))
                        WizardChoices(
                            draft.gender,
                            listOf(
                                UserGender.NEUTRAL.name to R.string.gender_neutral,
                                UserGender.FEMALE.name to R.string.gender_female,
                                UserGender.MALE.name to R.string.gender_male
                            )
                        ) { update(draft.copy(gender = it)) }
                        TextButton(onClick = {
                            update(draft.copy(name = "", gender = UserGender.NEUTRAL.name, stepIndex = step.next().ordinal))
                        }) { Text(stringResource(R.string.profile_skip)) }
                    }
                    OnboardingStep.METRICS -> {
                        WizardField(R.string.profile_age_optional_label, draft.age, KeyboardType.Number) {
                            update(draft.copy(age = it.numericText().take(3)))
                        }
                        WizardField(R.string.profile_height_optional_label, draft.height, KeyboardType.Decimal) {
                            update(draft.copy(height = it.numericText().take(7)))
                        }
                        WizardField(R.string.profile_weight_optional_label, draft.weight, KeyboardType.Decimal) {
                            update(draft.copy(weight = it.numericText().take(7)))
                        }
                        TextButton(onClick = {
                            update(draft.copy(age = "", height = "", weight = "", stepIndex = step.next().ordinal))
                        }) { Text(stringResource(R.string.profile_skip)) }
                    }
                    OnboardingStep.GOALS -> {
                        Text(stringResource(R.string.onboarding_goal_label))
                        WizardChoices(draft.goal, goalLabels) { update(draft.copy(goal = it)) }
                        if (draft.goal == FitnessGoal.CUSTOM.name) {
                            WizardField(R.string.goal_custom_hint, draft.custom) { update(draft.copy(custom = it.take(80))) }
                        }
                        Text(stringResource(R.string.onboarding_experience_label))
                        WizardChoices(draft.experience, experienceLabels) { update(draft.copy(experience = it)) }
                    }
                    OnboardingStep.REVIEW -> {
                        if (draft.name.isNotBlank()) Text(draft.name, style = MaterialTheme.typography.titleLarge)
                        goalLabels.firstOrNull { it.first == draft.goal }?.second?.let {
                            Text(draft.custom.takeIf { draft.goal == FitnessGoal.CUSTOM.name } ?: stringResource(it))
                        }
                        experienceLabels.firstOrNull { it.first == draft.experience }?.second?.let {
                            Text(stringResource(it))
                        }
                    }
                }
            }
        }
    }
}

private fun String.numericText() = map { ch ->
    ch.digitToIntOrNull()?.digitToChar() ?: if (ch == '٫' || ch == ',') '.' else ch
}.joinToString("")

private val goalLabels = listOf(
    FitnessGoal.GENERAL_FITNESS.name to R.string.goal_general_fitness,
    FitnessGoal.STRENGTH.name to R.string.goal_strength,
    FitnessGoal.MUSCLE_BUILDING.name to R.string.goal_muscle,
    FitnessGoal.MOBILITY.name to R.string.goal_mobility,
    FitnessGoal.CORE_STABILITY.name to R.string.goal_core,
    FitnessGoal.WEIGHT_MANAGEMENT.name to R.string.goal_weight_mgmt,
    FitnessGoal.CUSTOM.name to R.string.goal_custom
)

private val experienceLabels = listOf(
    ExperienceLevel.BEGINNER.name to R.string.exp_beginner,
    ExperienceLevel.INTERMEDIATE.name to R.string.exp_intermediate,
    ExperienceLevel.ADVANCED.name to R.string.exp_advanced
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> WizardChoices(selected: T, choices: List<Pair<T, Int>>, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(LayoutSpacing.small)) {
        choices.forEach { (value, label) ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                label = { Text(stringResource(label)) },
                modifier = Modifier.sizeIn(minHeight = LayoutSpacing.touchTarget)
            )
        }
    }
}

@Composable
private fun WizardField(label: Int, value: String, keyboardType: KeyboardType = KeyboardType.Text, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true
    )
}
