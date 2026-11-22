package com.aifitnesscoach.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aifitnesscoach.R
import com.aifitnesscoach.core.media.ExerciseIllustration
import com.aifitnesscoach.core.ui.LayoutSpacing
import com.aifitnesscoach.core.database.FitnessGoal
import com.aifitnesscoach.core.database.SavedInsightEntity
import com.aifitnesscoach.core.database.UserProfile
import com.aifitnesscoach.core.database.WorkoutSessionWithDetails
import com.aifitnesscoach.core.exercise.ExerciseCategory
import com.aifitnesscoach.core.exercise.ExerciseDefinition
import com.aifitnesscoach.core.exercise.definitions.ExerciseCatalog
import com.aifitnesscoach.core.motivation.MotivationalQuote
import com.aifitnesscoach.core.personalization.PersonalizedGreeting
import java.text.DateFormat
import java.util.Date

@Composable
fun HomeScreen(
    profile: UserProfile,
    isPersian: Boolean,
    recentExerciseIds: List<String>,
    allSessions: List<WorkoutSessionWithDetails>,
    latestInsight: SavedInsightEntity?,
    dailyQuote: MotivationalQuote?,
    onOpenProfileSetup: () -> Unit,
    onStartQuickWorkout: () -> Unit,
    onBuildMultiWorkout: () -> Unit,
    onContinueLastWorkout: (WorkoutSessionWithDetails) -> Unit,
    onSelectCategory: (ExerciseCategory) -> Unit,
    onSelectExercise: (ExerciseDefinition) -> Unit,
    onOpenInsights: () -> Unit
) {
    val weekAgoMs = System.currentTimeMillis() - 7L * 24L * 60L * 60L * 1000L
    val weeklySessions = allSessions.filter { it.session.startedAtEpochMs >= weekAgoMs }
    val weeklyMinutes = (weeklySessions.sumOf { it.session.totalDurationMs } / 60_000L).toInt()
    val lastSession = allSessions.firstOrNull()
    val recentExercises = recentExerciseIds.mapNotNull { ExerciseCatalog.findById(it) }

    val goalText = when (profile.fitnessGoal) {
        FitnessGoal.GENERAL_FITNESS -> stringResource(R.string.goal_general_fitness)
        FitnessGoal.STRENGTH -> stringResource(R.string.goal_strength)
        FitnessGoal.MUSCLE_BUILDING -> stringResource(R.string.goal_muscle)
        FitnessGoal.MOBILITY -> stringResource(R.string.goal_mobility)
        FitnessGoal.CORE_STABILITY -> stringResource(R.string.goal_core)
        FitnessGoal.WEIGHT_MANAGEMENT -> stringResource(R.string.goal_weight_mgmt)
        FitnessGoal.CUSTOM -> profile.customGoalLabel.ifBlank { stringResource(R.string.goal_custom) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(LayoutSpacing.pagePadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section A — Time-Aware Personalized Greeting Header + Profile Shortcut
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = PersonalizedGreeting.homeGreeting(profile.name, isPersian),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = PersonalizedGreeting.readyPrompt(
                                name = profile.name,
                                isPersian = isPersian,
                                hasWorkouts = allSessions.isNotEmpty()
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f)
                        )
                    }
                    FilledTonalIconButton(
                        onClick = onOpenProfileSetup,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = stringResource(R.string.profile_edit)
                        )
                    }
                }
            }
        }

        // Section B — Primary Workout Actions
        item {
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onStartQuickWorkout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.home_start_workout),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBuildMultiWorkout,
                            modifier = Modifier
                                .weight(1f)
                                .sizeIn(minHeight = 48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.home_build_session))
                        }

                        if (lastSession != null) {
                            FilledTonalButton(
                                onClick = { onContinueLastWorkout(lastSession) },
                                modifier = Modifier
                                    .weight(1f)
                                    .sizeIn(minHeight = 48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.home_continue_previous))
                            }
                        }
                    }
                }
            }
        }

        // Section C — Weekly Overview & Active Goal (Real data only)
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.home_weekly_summary),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = goalText,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.home_weekly_sessions, weeklySessions.size),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.home_weekly_minutes, weeklyMinutes),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        // Section D — Four Exercise Categories
        item {
            Text(
                text = stringResource(R.string.home_categories_title),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val categories = listOf(
                    ExerciseCategory.WEIGHTED_TRAINING to stringResource(R.string.cat_weighted),
                    ExerciseCategory.BODYWEIGHT_TRAINING to stringResource(R.string.cat_bodyweight),
                    ExerciseCategory.CORE_TRAINING to stringResource(R.string.cat_core),
                    ExerciseCategory.RESISTANCE_BAND_TRAINING to stringResource(R.string.cat_resistance_band)
                )
                categories.chunked(2).forEach { rowCats ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowCats.forEach { (cat, label) ->
                            val count = ExerciseCatalog.byCategory(cat).size
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSelectCategory(cat) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Icon(
                                        imageVector = Icons.Outlined.FitnessCenter,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = stringResource(R.string.home_category_count, count),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section E — Quick Access: Recent Exercises & Last Completed Workout
        if (recentExercises.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.home_recent_exercises),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(recentExercises, key = { it.id }) { ex ->
                        Card(
                            modifier = Modifier
                                .width(200.dp)
                                .clickable { onSelectExercise(ex) },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                ExerciseIllustration(ex.id, Modifier.fillMaxWidth().height(88.dp))
                                Spacer(Modifier.height(LayoutSpacing.small))
                                Text(
                                    text = if (isPersian) ex.nameFa else ex.nameEn,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 2
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (isPersian) ex.targetMusclesFa.firstOrNull().orEmpty()
                                    else ex.targetMusclesEn.firstOrNull().orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.home_last_workout),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(8.dp))
            if (lastSession == null) {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.home_no_workouts_yet),
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isPersian) lastSession.session.titleFa else lastSession.session.titleEn,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        val dateStr = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                            .format(Date(lastSession.session.startedAtEpochMs))
                        Text(
                            text = stringResource(
                                R.string.home_last_workout_meta,
                                dateStr,
                                lastSession.sets.size
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Section F — Compact Daily English Motivational Quote
        item {
            DailyMotivationCard(
                quote = dailyQuote,
                athleteName = profile.name,
                isPersian = isPersian
            )
        }

        // Section G — Optional Latest Saved AI Coaching Insight (only shown when real insight exists)
        if (latestInsight != null) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenInsights() }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.home_ai_insight_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            TextButton(onClick = onOpenInsights) {
                                Text(stringResource(R.string.ai_open_insights))
                            }
                        }
                        Text(
                            text = latestInsight.summaryTitle,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = latestInsight.bodyText,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
