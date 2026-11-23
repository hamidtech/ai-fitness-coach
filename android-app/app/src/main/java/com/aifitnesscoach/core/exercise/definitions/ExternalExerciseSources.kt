package com.aifitnesscoach.core.exercise.definitions

import android.content.Context
import android.util.Log
import com.aifitnesscoach.core.equipment.ResistanceBandType
import com.aifitnesscoach.core.exercise.CameraSetupGuide
import com.aifitnesscoach.core.exercise.EquipmentCategoryType
import com.aifitnesscoach.core.exercise.ExerciseCategory
import com.aifitnesscoach.core.exercise.ExerciseDefinition
import com.aifitnesscoach.core.exercise.ExerciseMode
import com.aifitnesscoach.core.exercise.ImplementationStatus
import com.aifitnesscoach.core.exercise.MediaAssetRef
import com.aifitnesscoach.core.exercise.RecommendedCameraView
import org.json.JSONArray
import org.json.JSONObject
import java.util.zip.GZIPInputStream

enum class ExerciseSource { BUILT_IN, HASAN, OPEN_EXERCISE_BASE }

data class ExternalPhoto(
    val tile: Int,
    val path: String,
    val bytes: Long,
    val resolution: String
)

data class ExternalExercise(
    val definition: ExerciseDefinition,
    val source: ExerciseSource,
    val equipment: List<String>,
    val regions: List<String>,
    val photo: ExternalPhoto?
)

object ExternalExerciseSources {
    const val OPEN_REVISION = "d2e9058330bc477be1b1e51b9f9df788f963ac04"
    const val HASAN_REVISION = "7455efae41b330c265e7cd4b78dfa848e7ce5ebd"
    const val OPEN_REPOSITORY = "https://github.com/OpenExerciseBase/OpenExerciseBase-Database"
    const val HASAN_REPOSITORY = "https://github.com/hasaneyldrm/exercises-dataset"
    const val OPEN_ATTRIBUTION = "© 2026 Ludwig Boltzmann Institute for Digital Health and Prevention · OpenExerciseBase"
    const val OPEN_LICENSE = "CC BY-NC 4.0"
    const val OPEN_LICENSE_URL = "https://creativecommons.org/licenses/by-nc/4.0/"
    const val ATLAS_PATH = "exercise_sources/open_thumbnails.webp"
    const val TILE_EDGE = 96
    const val ATLAS_COLUMNS = 16

    private data class Snapshot(
        val byId: Map<String, ExternalExercise>,
        val ordered: List<ExerciseDefinition>,
        val availablePhotos: Map<String, ExternalPhoto>
    )

    @Volatile private var snapshot: Snapshot? = null

    val exercises: List<ExerciseDefinition> get() = snapshot?.ordered.orEmpty()

    fun entry(id: String): ExternalExercise? = snapshot?.byId?.get(id)

    fun photo(id: String): ExternalPhoto? = snapshot?.availablePhotos?.get(id)

    @Synchronized
    fun load(context: Context) {
        if (snapshot != null) return
        try {
            val catalog = JSONObject(context.assets.open("exercise_sources/catalog.json.gz").use { file ->
                GZIPInputStream(file).bufferedReader().use { it.readText() }
            })
            check(catalog.getString("openRevision") == OPEN_REVISION)
            check(catalog.getString("hasanRevision") == HASAN_REVISION)
            val rows = catalog.getJSONArray("exercises")
            val entries = List(rows.length()) { parse(rows.getJSONObject(it)) }
            val byId = entries.associateBy { it.definition.id }
            check(entries.size == byId.size)
            val photosByNameAndEquipment = entries.filter { it.photo != null }.groupBy {
                searchKey(it.definition.nameEn) to it.equipment.toSet()
            }
            val photos = buildMap {
                entries.forEach { item ->
                    val photo = item.photo ?: photosByNameAndEquipment[
                        searchKey(item.definition.nameEn) to item.equipment.toSet()
                    ]?.singleOrNull()?.photo
                    if (photo != null) put(item.definition.id, photo)
                }
            }
            snapshot = Snapshot(byId, entries.map { it.definition }, photos)
        } catch (error: Exception) {
            Log.e("ExerciseSources", "Unable to load the bundled catalog", error)
        }
    }

    private fun parse(row: JSONObject): ExternalExercise {
        val id = row.getString("id")
        val source = if (id.startsWith("hasan-")) ExerciseSource.HASAN else ExerciseSource.OPEN_EXERCISE_BASE
        val equipment = row.getJSONArray("equipment").strings().map(::equipmentKey).distinct()
        val muscles = row.getJSONArray("muscles").strings()
        val regions = row.getJSONArray("body").strings()
        val steps = row.getJSONArray("steps").strings()
        val name = row.getString("name").replaceFirstChar { it.titlecase() }
        val type = when {
            "resistance band" in equipment -> EquipmentCategoryType.RESISTANCE_BAND
            "dumbbell" in equipment -> EquipmentCategoryType.DUMBBELL
            "yoga mat" in equipment -> EquipmentCategoryType.MAT_OR_FLOOR
            "wall" in equipment -> EquipmentCategoryType.WALL
            equipment == listOf("body weight") -> EquipmentCategoryType.BODYWEIGHT
            else -> EquipmentCategoryType.OTHER
        }
        val category = when {
            type == EquipmentCategoryType.RESISTANCE_BAND -> ExerciseCategory.RESISTANCE_BAND_TRAINING
            equipment.any { it in loadedEquipment } -> ExerciseCategory.WEIGHTED_TRAINING
            regions.any { searchKey(it) in setOf("waist", "core", "abdominals") } -> ExerciseCategory.CORE_TRAINING
            else -> ExerciseCategory.BODYWEIGHT_TRAINING
        }
        val definition = ExerciseDefinition(
            id = id,
            nameEn = name,
            nameFa = name,
            category = category,
            descriptionEn = "",
            descriptionFa = "",
            targetMusclesEn = muscles,
            targetMusclesFa = muscles,
            equipmentType = type,
            supportedBandTypes = if (type == EquipmentCategoryType.RESISTANCE_BAND) {
                ResistanceBandType.entries
            } else emptyList(),
            exerciseMode = if (row.getBoolean("timed")) ExerciseMode.TIMED_HOLD else ExerciseMode.REPETITIONS,
            cameraGuide = CameraSetupGuide(RecommendedCameraView.SIDE, "", "", "", ""),
            requiredLandmarks = emptyList(),
            analyzerId = null,
            demoMedia = MediaAssetRef("", ""),
            instructionsEn = steps,
            instructionsFa = steps,
            commonMistakesEn = emptyList(),
            commonMistakesFa = emptyList(),
            observableFormChecksEn = emptyList(),
            observableFormChecksFa = emptyList(),
            limitationsEn = emptyList(),
            limitationsFa = emptyList(),
            implementationStatus = ImplementationStatus.MANUAL_ONLY
        )
        val photo = if (row.has("tile")) {
            ExternalPhoto(row.getInt("tile"), row.getString("path"), row.getLong("size"), row.getString("resolution"))
        } else null
        return ExternalExercise(definition, source, equipment, regions, photo)
    }

    private fun JSONArray.strings(): List<String> = List(length()) { getString(it) }

    private val loadedEquipment = setOf(
        "dumbbell", "barbell", "cable machine", "leverage machine", "smith machine",
        "kettlebell", "weighted", "ez barbell", "sled machine", "medicine ball",
        "olympic barbell", "trap bar", "chest press machine", "leg press machine",
        "ankle weights", "weight plates"
    )
}
