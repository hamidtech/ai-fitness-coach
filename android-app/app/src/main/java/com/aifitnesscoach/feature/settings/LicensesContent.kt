package com.aifitnesscoach.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aifitnesscoach.R
import com.aifitnesscoach.core.exercise.definitions.ExternalExerciseSources
import com.aifitnesscoach.core.media.ExerciseAssetCatalog
import com.aifitnesscoach.core.ui.ControlFlow
import com.aifitnesscoach.core.ui.LayoutSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class ThirdPartyNotice(
    val name: String,
    @StringRes val summary: Int,
    val source: String,
    val document: String? = null,
    val attribution: String? = null
)

private val notices = listOf(
    ThirdPartyNotice("Exercises Dataset · MIT", R.string.license_hasan,
        "${ExternalExerciseSources.HASAN_REPOSITORY}/tree/${ExternalExerciseSources.HASAN_REVISION}",
        "hasan-license.txt", "© 2026 Hasan Emir Yıldırım"),
    ThirdPartyNotice("OpenExerciseBase · CC BY-NC 4.0", R.string.license_oeb,
        "${ExternalExerciseSources.OPEN_REPOSITORY}/tree/${ExternalExerciseSources.OPEN_REVISION}",
        "open-license.txt", ExternalExerciseSources.OPEN_ATTRIBUTION),
    ThirdPartyNotice("RepDB · Free Tier 1.0", R.string.license_repdb,
        "https://github.com/RepDB/exercise-dataset/blob/f12da458a90f353a86c953f204304d5a8bd98275/LICENSE-DATA.md",
        "repdb-license.txt", ExerciseAssetCatalog.REPDB_ATTRIBUTION),
    ThirdPartyNotice("Gym visual", R.string.license_gym_visual,
        "https://github.com/hasaneyldrm/exercises-dataset/blob/${ExternalExerciseSources.HASAN_REVISION}/NOTICE.md",
        "hasan-notice.txt"),
    ThirdPartyNotice("Google MediaPipe", R.string.license_mediapipe,
        "https://github.com/google-ai-edge/mediapipe/blob/master/LICENSE", "apache-2.0.txt"),
    ThirdPartyNotice("AndroidX · Apache 2.0", R.string.license_androidx,
        "https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt", "apache-2.0.txt"),
    ThirdPartyNotice("Kotlin & coroutines · Apache 2.0", R.string.license_runtime,
        "https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt", "apache-2.0.txt"),
    ThirdPartyNotice("Coil · Apache 2.0", R.string.license_coil,
        "https://github.com/coil-kt/coil/blob/2.7.0/LICENSE.txt", "apache-2.0.txt"),
    ThirdPartyNotice("OkHttp / Okio · Apache 2.0", R.string.license_okhttp,
        "https://github.com/square/okhttp/blob/parent-4.12.0/LICENSE.txt", "apache-2.0.txt"),
    ThirdPartyNotice("Gson · Apache 2.0", R.string.license_gson,
        "https://github.com/google/gson/blob/main/LICENSE", "apache-2.0.txt"),
    ThirdPartyNotice("LiteRT-LM · Apache 2.0", R.string.license_litert,
        "https://github.com/google-ai-edge/LiteRT-LM/blob/main/LICENSE", "apache-2.0.txt"),
    ThirdPartyNotice("OpenGym", R.string.license_opengym, "https://github.com/DuarteSantos8/openGym")
)

@Composable
internal fun LicensesContent() {
    val context = LocalContext.current.applicationContext
    val uri = LocalUriHandler.current
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }

    notices.forEach { notice ->
        val isExpanded = notice.name == expanded
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(LayoutSpacing.screen),
                verticalArrangement = Arrangement.spacedBy(LayoutSpacing.small)) {
                Text(notice.name, style = MaterialTheme.typography.titleMedium)
                notice.attribution?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
                Text(stringResource(notice.summary), style = MaterialTheme.typography.bodyMedium)
                ControlFlow {
                    TextButton(onClick = { runCatching { uri.openUri(notice.source) } }) {
                        Text(stringResource(R.string.license_source))
                    }
                    if (notice.document != null) {
                        TextButton(onClick = { expanded = if (isExpanded) null else notice.name }) {
                            Text(stringResource(if (isExpanded) R.string.license_hide else R.string.license_read))
                        }
                    }
                }
                if (isExpanded && notice.document != null) {
                    val terms by produceState<String?>(null, notice.name) {
                        value = withContext(Dispatchers.IO) {
                            runCatching {
                                context.assets.open("exercise_sources/${notice.document}")
                                    .bufferedReader().use { it.readText() }
                            }.getOrNull()
                        }
                    }
                    terms?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }

    Text(stringResource(R.string.license_app_art_title), style = MaterialTheme.typography.titleMedium)
    Text(stringResource(R.string.license_app_art), style = MaterialTheme.typography.bodyMedium)
}
