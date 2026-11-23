package com.aifitnesscoach.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.aifitnesscoach.R
import com.aifitnesscoach.core.database.AppFeature
import com.aifitnesscoach.core.database.InterfaceMode
import com.aifitnesscoach.core.database.InterfacePreferences
import com.aifitnesscoach.core.ui.LayoutSpacing

@Composable
internal fun InterfaceSettings(
    layout: InterfacePreferences,
    onMode: (InterfaceMode) -> Unit,
    onToggle: (AppFeature, Boolean) -> Unit
) {
    Text(stringResource(R.string.feature_layout_help))
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(LayoutSpacing.small)) {
        InterfaceMode.entries.forEach { mode ->
            val label = if (mode == InterfaceMode.SIMPLE) R.string.feature_simple else R.string.feature_advanced
            Row(
                Modifier.fillMaxWidth().heightIn(min = LayoutSpacing.touchTarget)
                    .selectable(layout.mode == mode, role = Role.RadioButton, onClick = { onMode(mode) }),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LayoutSpacing.small)
            ) {
                RadioButton(selected = layout.mode == mode, onClick = null)
                Text(stringResource(label), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
    Text(stringResource(R.string.feature_switch_help), style = MaterialTheme.typography.bodyMedium)
    AppFeature.entries.forEach { feature ->
        val label = when (feature) {
            AppFeature.PLAN -> R.string.nav_plan
            AppFeature.PROGRESS -> R.string.nav_progress
            AppFeature.AI -> R.string.settings_section_d_ai
            AppFeature.HEALTH -> R.string.nav_health
            AppFeature.MEDIA -> R.string.media_library_title
        }
        val checked = layout.shows(feature)
        Row(
            Modifier.fillMaxWidth().heightIn(min = LayoutSpacing.touchTarget)
                .toggleable(checked, role = Role.Switch, onValueChange = { onToggle(feature, it) }),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LayoutSpacing.small)
        ) {
            Text(stringResource(label), Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}
