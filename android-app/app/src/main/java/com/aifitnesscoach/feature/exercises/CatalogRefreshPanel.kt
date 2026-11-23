package com.aifitnesscoach.feature.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aifitnesscoach.R
import com.aifitnesscoach.core.exercise.definitions.CatalogRefreshResult
import com.aifitnesscoach.core.exercise.definitions.ExternalExerciseSources
import com.aifitnesscoach.core.ui.LayoutSpacing
import kotlinx.coroutines.launch

@Composable
fun CatalogRefreshPanel() {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val version by ExternalExerciseSources.version.collectAsStateWithLifecycle()
    var refreshing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<CatalogRefreshResult?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(LayoutSpacing.small)) {
        OutlinedButton(
            modifier = Modifier.fillMaxWidth().heightIn(min = LayoutSpacing.touchTarget),
            enabled = !refreshing,
            onClick = {
                refreshing = true
                scope.launch {
                    try {
                        result = ExternalExerciseSources.refresh(context)
                    } finally {
                        refreshing = false
                    }
                }
            }
        ) {
            Text(stringResource(if (refreshing) R.string.catalog_refresh_busy else R.string.catalog_refresh_button))
        }
        val message = when {
            refreshing -> null
            result == CatalogRefreshResult.UPDATED -> stringResource(R.string.catalog_refresh_updated, version)
            result == CatalogRefreshResult.CURRENT -> stringResource(R.string.catalog_refresh_current, version)
            result == CatalogRefreshResult.FAILED -> stringResource(R.string.catalog_refresh_error)
            version > 0 -> stringResource(R.string.catalog_version, version)
            else -> null
        }
        if (message != null) {
            Text(message, Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodySmall)
        }
    }
}
