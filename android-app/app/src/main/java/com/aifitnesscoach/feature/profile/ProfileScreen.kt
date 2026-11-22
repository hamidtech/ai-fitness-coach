package com.aifitnesscoach.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aifitnesscoach.R
import com.aifitnesscoach.core.database.ExperienceLevel
import com.aifitnesscoach.core.database.FitnessGoal
import com.aifitnesscoach.core.database.MeasurementUnit
import com.aifitnesscoach.core.database.UserGender
import com.aifitnesscoach.core.database.UserPreferences
import com.aifitnesscoach.core.database.UserPreferencesRepository
import com.aifitnesscoach.core.personalization.ProfileValidationError
import com.aifitnesscoach.core.personalization.ProfileValidationResult
import com.aifitnesscoach.core.personalization.ProfileValidator
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    preferences: UserPreferences,
    preferencesRepository: UserPreferencesRepository,
    onBack: () -> Unit
) {
    val currentProfile = preferences.profile
    val unit = preferences.measurementUnit
    val scope = rememberCoroutineScope()

    var name by remember(currentProfile.name) { mutableStateOf(currentProfile.name) }
    var gender by remember(currentProfile.gender) { mutableStateOf(currentProfile.gender) }
    var useBirthYear by remember(currentProfile.birthYear) { mutableStateOf(currentProfile.birthYear != null) }
    var ageText by remember(currentProfile.age) { mutableStateOf(currentProfile.age?.toString().orEmpty()) }
    var birthYearText by remember(currentProfile.birthYear) { mutableStateOf(currentProfile.birthYear?.toString().orEmpty()) }
    var heightText by remember(currentProfile.heightCm, unit) {
        mutableStateOf(ProfileValidator.formatHeightForInput(currentProfile.heightCm, unit))
    }
    var weightText by remember(currentProfile.weightKg, unit) {
        mutableStateOf(ProfileValidator.formatWeightForInput(currentProfile.weightKg, unit))
    }
    var fitnessGoal by remember(currentProfile.fitnessGoal) { mutableStateOf(currentProfile.fitnessGoal) }
    var customGoal by remember(currentProfile.customGoalLabel) { mutableStateOf(currentProfile.customGoalLabel) }
    var experienceLevel by remember(currentProfile.experienceLevel) { mutableStateOf(currentProfile.experienceLevel) }

    var validationErrors by remember { mutableStateOf<Set<ProfileValidationError>>(emptySet()) }
    var showSavedConfirmation by remember { mutableStateOf(false) }

    val heightLabel = if (unit == MeasurementUnit.IMPERIAL) {
        stringResource(R.string.profile_height_imperial_label)
    } else {
        stringResource(R.string.profile_height_label)
    }
    val weightLabel = if (unit == MeasurementUnit.IMPERIAL) {
        stringResource(R.string.profile_weight_imperial_label)
    } else {
        stringResource(R.string.profile_weight_label)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.common_back)
                )
            }
            Text(
                text = stringResource(R.string.profile_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.profile_edit_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (showSavedConfirmation) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = stringResource(R.string.profile_saved_success),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Identity Section
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            showSavedConfirmation = false
                        },
                        label = { Text(stringResource(R.string.profile_name_label)) },
                        placeholder = { Text(stringResource(R.string.profile_name_hint)) },
                        trailingIcon = {
                            if (name.isNotEmpty()) {
                                IconButton(onClick = { name = ""; showSavedConfirmation = false }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Clear,
                                        contentDescription = stringResource(R.string.common_clear)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = stringResource(R.string.profile_gender_optional_label),
                        style = MaterialTheme.typography.labelLarge
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            UserGender.MALE to stringResource(R.string.gender_male),
                            UserGender.FEMALE to stringResource(R.string.gender_female),
                            UserGender.NEUTRAL to stringResource(R.string.gender_neutral)
                        ).forEach { (g, label) ->
                            FilterChip(
                                selected = gender == g,
                                onClick = {
                                    gender = g
                                    showSavedConfirmation = false
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            // Age / Birth Year & Body Metrics Section
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.profile_age_birth_section),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !useBirthYear,
                            onClick = {
                                useBirthYear = false
                                validationErrors = validationErrors - ProfileValidationError.INVALID_BIRTH_YEAR
                                showSavedConfirmation = false
                            },
                            label = { Text(stringResource(R.string.profile_mode_age)) }
                        )
                        FilterChip(
                            selected = useBirthYear,
                            onClick = {
                                useBirthYear = true
                                validationErrors = validationErrors - ProfileValidationError.INVALID_AGE
                                showSavedConfirmation = false
                            },
                            label = { Text(stringResource(R.string.profile_mode_birth_year)) }
                        )
                    }

                    if (!useBirthYear) {
                        OutlinedTextField(
                            value = ageText,
                            onValueChange = {
                                ageText = it.filter { ch -> ch.isDigit() }.take(3)
                                validationErrors = validationErrors - ProfileValidationError.INVALID_AGE
                                showSavedConfirmation = false
                            },
                            label = { Text(stringResource(R.string.profile_age_optional_label)) },
                            isError = ProfileValidationError.INVALID_AGE in validationErrors,
                            trailingIcon = {
                                if (ageText.isNotEmpty()) {
                                    IconButton(onClick = {
                                        ageText = ""
                                        validationErrors = validationErrors - ProfileValidationError.INVALID_AGE
                                    }) {
                                        Icon(Icons.Outlined.Clear, contentDescription = stringResource(R.string.common_clear))
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        OutlinedTextField(
                            value = birthYearText,
                            onValueChange = {
                                birthYearText = it.filter { ch -> ch.isDigit() }.take(4)
                                validationErrors = validationErrors - ProfileValidationError.INVALID_BIRTH_YEAR
                                showSavedConfirmation = false
                            },
                            label = { Text(stringResource(R.string.profile_birth_year_optional_label)) },
                            isError = ProfileValidationError.INVALID_BIRTH_YEAR in validationErrors,
                            trailingIcon = {
                                if (birthYearText.isNotEmpty()) {
                                    IconButton(onClick = {
                                        birthYearText = ""
                                        validationErrors = validationErrors - ProfileValidationError.INVALID_BIRTH_YEAR
                                    }) {
                                        Icon(Icons.Outlined.Clear, contentDescription = stringResource(R.string.common_clear))
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    HorizontalDivider()

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = heightText,
                            onValueChange = {
                                heightText = it.filter { ch -> ch.isDigit() || ch == '.' }
                                validationErrors = validationErrors - ProfileValidationError.INVALID_HEIGHT
                                showSavedConfirmation = false
                            },
                            label = { Text(heightLabel) },
                            isError = ProfileValidationError.INVALID_HEIGHT in validationErrors,
                            trailingIcon = {
                                if (heightText.isNotEmpty()) {
                                    IconButton(onClick = {
                                        heightText = ""
                                        validationErrors = validationErrors - ProfileValidationError.INVALID_HEIGHT
                                    }) {
                                        Icon(Icons.Outlined.Clear, contentDescription = stringResource(R.string.common_clear))
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = {
                                weightText = it.filter { ch -> ch.isDigit() || ch == '.' }
                                validationErrors = validationErrors - ProfileValidationError.INVALID_WEIGHT
                                showSavedConfirmation = false
                            },
                            label = { Text(weightLabel) },
                            isError = ProfileValidationError.INVALID_WEIGHT in validationErrors,
                            trailingIcon = {
                                if (weightText.isNotEmpty()) {
                                    IconButton(onClick = {
                                        weightText = ""
                                        validationErrors = validationErrors - ProfileValidationError.INVALID_WEIGHT
                                    }) {
                                        Icon(Icons.Outlined.Clear, contentDescription = stringResource(R.string.common_clear))
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    currentProfile.bmi?.let { bmi ->
                        Text(
                            text = stringResource(R.string.profile_bmi_info, String.format(Locale.US, "%.1f", bmi)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    TextButton(
                        onClick = {
                            ageText = ""
                            birthYearText = ""
                            heightText = ""
                            weightText = ""
                            validationErrors = emptySet()
                            showSavedConfirmation = false
                        }
                    ) {
                        Text(stringResource(R.string.profile_clear_optional_metrics))
                    }
                }
            }

            // Goals & Experience Section
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.onboarding_goal_label), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            FitnessGoal.GENERAL_FITNESS to stringResource(R.string.goal_general_fitness),
                            FitnessGoal.STRENGTH to stringResource(R.string.goal_strength),
                            FitnessGoal.MUSCLE_BUILDING to stringResource(R.string.goal_muscle),
                            FitnessGoal.MOBILITY to stringResource(R.string.goal_mobility),
                            FitnessGoal.CORE_STABILITY to stringResource(R.string.goal_core),
                            FitnessGoal.WEIGHT_MANAGEMENT to stringResource(R.string.goal_weight_mgmt),
                            FitnessGoal.CUSTOM to stringResource(R.string.goal_custom)
                        ).forEach { (g, label) ->
                            FilterChip(
                                selected = fitnessGoal == g,
                                onClick = {
                                    fitnessGoal = g
                                    validationErrors = validationErrors - ProfileValidationError.CUSTOM_GOAL_EMPTY
                                    showSavedConfirmation = false
                                },
                                label = { Text(label) }
                            )
                        }
                    }

                    if (fitnessGoal == FitnessGoal.CUSTOM) {
                        OutlinedTextField(
                            value = customGoal,
                            onValueChange = {
                                customGoal = it
                                validationErrors = validationErrors - ProfileValidationError.CUSTOM_GOAL_EMPTY
                                showSavedConfirmation = false
                            },
                            label = { Text(stringResource(R.string.goal_custom_hint)) },
                            isError = ProfileValidationError.CUSTOM_GOAL_EMPTY in validationErrors,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    HorizontalDivider()

                    Text(stringResource(R.string.onboarding_experience_label), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            ExperienceLevel.BEGINNER to stringResource(R.string.exp_beginner),
                            ExperienceLevel.INTERMEDIATE to stringResource(R.string.exp_intermediate),
                            ExperienceLevel.ADVANCED to stringResource(R.string.exp_advanced)
                        ).forEach { (exp, label) ->
                            FilterChip(
                                selected = experienceLevel == exp,
                                onClick = {
                                    experienceLevel = exp
                                    showSavedConfirmation = false
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            if (validationErrors.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val message = when {
                            ProfileValidationError.CUSTOM_GOAL_EMPTY in validationErrors ->
                                stringResource(R.string.profile_custom_goal_error)
                            unit == MeasurementUnit.IMPERIAL ->
                                stringResource(R.string.profile_validation_error_imperial)
                            else ->
                                stringResource(R.string.profile_validation_error)
                        }
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
                Button(
                    onClick = {
                        when (
                            val res = ProfileValidator.validateAndBuildProfile(
                                existingProfile = currentProfile,
                                nameInput = name,
                                gender = gender,
                                useBirthYear = useBirthYear,
                                ageInput = ageText,
                                birthYearInput = birthYearText,
                                heightInput = heightText,
                                weightInput = weightText,
                                measurementUnit = unit,
                                fitnessGoal = fitnessGoal,
                                customGoalLabel = customGoal,
                                experienceLevel = experienceLevel
                            )
                        ) {
                            is ProfileValidationResult.Invalid -> {
                                validationErrors = res.errors
                                showSavedConfirmation = false
                            }
                            is ProfileValidationResult.Valid -> {
                                validationErrors = emptySet()
                                scope.launch {
                                    preferencesRepository.saveProfile(res.profile)
                                    showSavedConfirmation = true
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1.6f).height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.profile_save))
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
