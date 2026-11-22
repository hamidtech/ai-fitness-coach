package com.aifitnesscoach.feature.onboarding

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aifitnesscoach.R
import com.aifitnesscoach.core.database.AppLanguage
import com.aifitnesscoach.core.database.ExperienceLevel
import com.aifitnesscoach.core.database.FitnessGoal
import com.aifitnesscoach.core.database.UserGender
import com.aifitnesscoach.core.database.UserProfile

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    initialProfile: UserProfile,
    initialLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onCompleteOnboarding: (UserProfile) -> Unit
) {
    var step by remember { mutableIntStateOf(0) } // 0=Welcome, 1=Capabilities, 2=Privacy, 3=Language, 4=Profile+Goals

    var name by remember(initialProfile.name) { mutableStateOf(initialProfile.name) }
    var gender by remember(initialProfile.gender) { mutableStateOf(initialProfile.gender) }
    var ageText by remember(initialProfile.age) { mutableStateOf(initialProfile.age?.toString().orEmpty()) }
    var heightText by remember(initialProfile.heightCm) { mutableStateOf(initialProfile.heightCm?.toInt()?.toString().orEmpty()) }
    var weightText by remember(initialProfile.weightKg) { mutableStateOf(initialProfile.weightKg?.toInt()?.toString().orEmpty()) }
    var fitnessGoal by remember(initialProfile.fitnessGoal) { mutableStateOf(initialProfile.fitnessGoal) }
    var customGoal by remember(initialProfile.customGoalLabel) { mutableStateOf(initialProfile.customGoalLabel) }
    var experienceLevel by remember(initialProfile.experienceLevel) { mutableStateOf(initialProfile.experienceLevel) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val totalSteps = 5
    val nameRequiredMsg = stringResource(R.string.onboarding_name_required)
    val rangeErrorMsg = stringResource(R.string.profile_validation_error)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LinearProgressIndicator(
            progress = { (step + 1).toFloat() / totalSteps.toFloat() },
            modifier = Modifier.fillMaxWidth()
        )

        when (step) {
            0 -> {
                Spacer(Modifier.height(16.dp))
                Icon(
                    imageVector = Icons.Outlined.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(72.dp)
                        .align(Alignment.CenterHorizontally)
                )
                Text(
                    text = stringResource(R.string.onboarding_welcome_title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.onboarding_welcome_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f, fill = true))
                Button(
                    onClick = { step = 1 },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.onboarding_get_started))
                }
            }

            1 -> {
                Text(
                    text = stringResource(R.string.onboarding_capabilities_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.cat_weighted), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.cat_bodyweight), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.cat_core), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.cat_resistance_band), style = MaterialTheme.typography.titleMedium)
                    }
                }
                Text(
                    text = stringResource(R.string.onboarding_capabilities_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { step = 0 }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.common_back))
                    }
                    Button(onClick = { step = 2 }, modifier = Modifier.weight(2f).height(52.dp)) {
                        Text(stringResource(R.string.onboarding_continue))
                    }
                }
            }

            2 -> {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = stringResource(R.string.onboarding_privacy_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_privacy_body),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { step = 1 }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.common_back))
                    }
                    Button(onClick = { step = 3 }, modifier = Modifier.weight(2f).height(52.dp)) {
                        Text(stringResource(R.string.onboarding_continue))
                    }
                }
            }

            3 -> {
                Icon(
                    imageVector = Icons.Outlined.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(52.dp)
                )
                Text(
                    text = stringResource(R.string.onboarding_language_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.onboarding_language_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        AppLanguage.EN to stringResource(R.string.lang_en),
                        AppLanguage.FA to stringResource(R.string.lang_fa),
                        AppLanguage.SYSTEM to stringResource(R.string.lang_system)
                    ).forEach { (lang, label) ->
                        val selected = initialLanguage == lang
                        if (selected) {
                            Button(
                                onClick = { onLanguageSelected(lang) },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(label)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onLanguageSelected(lang) },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { step = 2 }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.common_back))
                    }
                    Button(onClick = { step = 4 }, modifier = Modifier.weight(2f).height(52.dp)) {
                        Text(stringResource(R.string.onboarding_continue))
                    }
                }
            }

            else -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = stringResource(R.string.onboarding_profile_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = stringResource(R.string.onboarding_profile_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        validationError = null
                    },
                    label = { Text(stringResource(R.string.onboarding_name_required_label)) },
                    placeholder = { Text(stringResource(R.string.profile_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(stringResource(R.string.profile_gender_label), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        UserGender.MALE to stringResource(R.string.gender_male),
                        UserGender.FEMALE to stringResource(R.string.gender_female),
                        UserGender.NEUTRAL to stringResource(R.string.gender_neutral)
                    ).forEach { (g, label) ->
                        FilterChip(
                            selected = gender == g,
                            onClick = { gender = g },
                            label = { Text(label) }
                        )
                    }
                }

                Text(stringResource(R.string.onboarding_optional_body_metrics), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ageText,
                        onValueChange = { ageText = it.filter { ch -> ch.isDigit() } },
                        label = { Text(stringResource(R.string.profile_age_optional_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = heightText,
                        onValueChange = { heightText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text(stringResource(R.string.profile_height_optional_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text(stringResource(R.string.profile_weight_optional_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(stringResource(R.string.onboarding_goal_label), style = MaterialTheme.typography.labelLarge)
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
                            onClick = { fitnessGoal = g },
                            label = { Text(label) }
                        )
                    }
                }

                if (fitnessGoal == FitnessGoal.CUSTOM) {
                    OutlinedTextField(
                        value = customGoal,
                        onValueChange = { customGoal = it },
                        label = { Text(stringResource(R.string.goal_custom_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text(stringResource(R.string.onboarding_experience_label), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        ExperienceLevel.BEGINNER to stringResource(R.string.exp_beginner),
                        ExperienceLevel.INTERMEDIATE to stringResource(R.string.exp_intermediate),
                        ExperienceLevel.ADVANCED to stringResource(R.string.exp_advanced)
                    ).forEach { (exp, label) ->
                        FilterChip(
                            selected = experienceLevel == exp,
                            onClick = { experienceLevel = exp },
                            label = { Text(label) }
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_permissions_later_note),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                validationError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { step = 3 }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.common_back))
                    }
                    Button(
                        onClick = {
                            val trimmedName = name.trim()
                            if (trimmedName.isEmpty()) {
                                validationError = nameRequiredMsg
                                return@Button
                            }
                            val age = ageText.toIntOrNull()
                            val height = heightText.toFloatOrNull()
                            val weight = weightText.toFloatOrNull()
                            val ageOk = age == null || age in 10..110
                            val heightOk = height == null || height in 90f..250f
                            val weightOk = weight == null || weight in 25f..300f
                            if (!ageOk || !heightOk || !weightOk) {
                                validationError = rangeErrorMsg
                                return@Button
                            }
                            validationError = null
                            onCompleteOnboarding(
                                UserProfile(
                                    name = trimmedName,
                                    gender = gender,
                                    age = age,
                                    heightCm = height,
                                    weightKg = weight,
                                    fitnessGoal = fitnessGoal,
                                    customGoalLabel = if (fitnessGoal == FitnessGoal.CUSTOM) customGoal.trim() else "",
                                    experienceLevel = experienceLevel,
                                    hasCompletedOnboarding = true
                                )
                            )
                        },
                        modifier = Modifier.weight(2f).height(52.dp)
                    ) {
                        Text(stringResource(R.string.onboarding_finish))
                    }
                }

                TextButton(
                    onClick = {
                        onCompleteOnboarding(
                            initialProfile.copy(
                                name = name.trim(),
                                gender = gender,
                                hasCompletedOnboarding = true
                            )
                        )
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(stringResource(R.string.profile_skip))
                }
            }
        }
    }
}
