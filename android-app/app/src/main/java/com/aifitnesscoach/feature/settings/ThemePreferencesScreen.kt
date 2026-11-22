package com.aifitnesscoach.feature.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aifitnesscoach.R
import com.aifitnesscoach.core.database.AppColorPalette
import com.aifitnesscoach.core.database.AppThemeMode
import com.aifitnesscoach.core.database.UserPreferences
import com.aifitnesscoach.core.database.UserPreferencesRepository
import com.aifitnesscoach.core.ui.theme.palettePreviewPrimaryColor
import com.aifitnesscoach.core.ui.theme.resolveEffectivePalette
import kotlinx.coroutines.launch

@Composable
fun ThemePreferencesScreen(
    preferences: UserPreferences,
    preferencesRepository: UserPreferencesRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val darkPreview = when (preferences.themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val resolvedAutoPalette = resolveEffectivePalette(AppColorPalette.AUTO, preferences.profile.gender)

    Column(modifier = Modifier.fillMaxSize()) {
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
                text = stringResource(R.string.theme_screen_title),
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Appearance Mode
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.settings_theme),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.theme_appearance_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            AppThemeMode.SYSTEM to stringResource(R.string.theme_system),
                            AppThemeMode.LIGHT to stringResource(R.string.theme_light),
                            AppThemeMode.DARK to stringResource(R.string.theme_dark)
                        ).forEach { (mode, label) ->
                            FilterChip(
                                selected = preferences.themeMode == mode,
                                onClick = { scope.launch { preferencesRepository.setThemeMode(mode) } },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            // Color Palette Selection
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.theme_palette_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.theme_palette_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val paletteOptions = listOf(
                        Triple(
                            AppColorPalette.AUTO,
                            stringResource(R.string.palette_auto),
                            stringResource(R.string.palette_auto_desc, paletteNameFor(resolvedAutoPalette))
                        ),
                        Triple(
                            AppColorPalette.ATHLETIC_COBALT,
                            stringResource(R.string.palette_cobalt),
                            stringResource(R.string.palette_cobalt_desc)
                        ),
                        Triple(
                            AppColorPalette.TEAL,
                            stringResource(R.string.palette_teal),
                            stringResource(R.string.palette_teal_desc)
                        ),
                        Triple(
                            AppColorPalette.VIOLET,
                            stringResource(R.string.palette_violet),
                            stringResource(R.string.palette_violet_desc)
                        ),
                        Triple(
                            AppColorPalette.NEUTRAL,
                            stringResource(R.string.palette_neutral),
                            stringResource(R.string.palette_neutral_desc)
                        )
                    )

                    paletteOptions.forEach { (palette, title, desc) ->
                        val selected = preferences.colorPalette == palette && !preferences.useDynamicColor
                        val swatchColor = palettePreviewPrimaryColor(
                            palette = palette,
                            gender = preferences.profile.gender,
                            darkTheme = darkPreview
                        )
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        preferencesRepository.setUseDynamicColor(false)
                                        preferencesRepository.setColorPalette(palette)
                                    }
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                        .border(
                                            width = if (selected) 2.dp else 1.dp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                            shape = CircleShape
                                        )
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = stringResource(R.string.common_selected),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Optional Android 12+ Dynamic Color
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.theme_dynamic_color_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (supportsDynamicColor) {
                                    stringResource(R.string.theme_dynamic_color_desc)
                                } else {
                                    stringResource(R.string.theme_dynamic_color_unsupported)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = preferences.useDynamicColor && supportsDynamicColor,
                            enabled = supportsDynamicColor,
                            onCheckedChange = { enabled ->
                                scope.launch { preferencesRepository.setUseDynamicColor(enabled) }
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun paletteNameFor(palette: AppColorPalette): String = when (palette) {
    AppColorPalette.AUTO -> stringResource(R.string.palette_auto)
    AppColorPalette.ATHLETIC_COBALT -> stringResource(R.string.palette_cobalt)
    AppColorPalette.TEAL -> stringResource(R.string.palette_teal)
    AppColorPalette.VIOLET -> stringResource(R.string.palette_violet)
    AppColorPalette.NEUTRAL -> stringResource(R.string.palette_neutral)
}
