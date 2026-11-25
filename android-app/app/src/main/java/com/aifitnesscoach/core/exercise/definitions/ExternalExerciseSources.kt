package com.aifitnesscoach.core.exercise.definitions

import android.content.Context
import android.util.Log
import android.util.AtomicFile
import com.aifitnesscoach.core.media.AssetDownloadStatus
import com.aifitnesscoach.core.media.AssetMediaType
import com.aifitnesscoach.core.media.AssetSourceType
import com.aifitnesscoach.core.media.ExerciseMediaManifestEntry
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.concurrent.TimeUnit
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

enum class ExerciseSource { BUILT_IN, HASAN, OPEN_EXERCISE_BASE, OTHER }

data class ExternalPhoto(
    val tile: Int,
    val path: String?,
    val bytes: Long,
    val resolution: String,
    val revision: String,
    val attribution: String
)

data class ExternalExercise(
    val definition: ExerciseDefinition,
    val source: ExerciseSource,
    val equipment: List<String>,
    val regions: List<String>,
    val photo: ExternalPhoto?
)

enum class CatalogRefreshResult { UPDATED, CURRENT, FAILED }

object ExternalExerciseSources {
    private const val FEED_URL =
        "https://raw.githubusercontent.com/hamidtech/ai-fitness-coach/main/catalog-content/pack.zip"
    private val refreshLock = Mutex()
    private val client by lazy {
        OkHttpClient.Builder().followRedirects(false).callTimeout(25, TimeUnit.SECONDS).build()
    }
    private val catalogVersion = MutableStateFlow(0)
    val version = catalogVersion.asStateFlow()
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
        val availablePhotos: Map<String, ExternalPhoto>,
        val media: Map<String, ExerciseMediaManifestEntry>,
        val atlas: ByteArray?,
        val version: Int
    )

    @Volatile private var snapshot: Snapshot? = null

    val exercises: List<ExerciseDefinition> get() = snapshot?.ordered.orEmpty()

    fun entry(id: String): ExternalExercise? = snapshot?.byId?.get(id)

    fun photo(id: String, version: Int? = null): ExternalPhoto? =
        snapshot?.takeIf { version == null || it.version == version }?.availablePhotos?.get(id)

    fun media(id: String): ExerciseMediaManifestEntry? = snapshot?.media?.get(id)

    fun openAtlas(context: Context, version: Int): java.io.InputStream? {
        val current = snapshot ?: return null
        if (current.version != version) return null
        return current.atlas?.inputStream() ?: context.assets.open(ATLAS_PATH)
    }

    private fun diskPack(context: Context) = AtomicFile(File(context.filesDir, "exercise-catalog.pack"))

    @Synchronized
    fun load(context: Context) {
        if (snapshot != null) return
        try {
            val catalog = JSONObject(context.assets.open("exercise_sources/catalog.bin").use { file ->
                GZIPInputStream(file).bufferedReader().use { it.readText() }
            })
            check(catalog.getString("openRevision") == OPEN_REVISION)
            check(catalog.getString("hasanRevision") == HASAN_REVISION)
            val bundled = parseSnapshot(catalog)
            val cached = try {
                diskPack(context).openRead().use { readPack(it.readBounded(MAX_PACK_BYTES), bundled) }
            } catch (_: java.io.FileNotFoundException) {
                null
            } catch (error: Exception) {
                Log.w("ExerciseSources", "Ignoring invalid catalog cache", error)
                null
            }
            publish(cached ?: bundled)
        } catch (error: Exception) {
            Log.e("ExerciseSources", "Unable to load the bundled catalog", error)
        }
    }

    private fun downloadPack(): ByteArray =
        client.newCall(Request.Builder().url(FEED_URL).build()).execute().use {
            check(it.isSuccessful)
            requireNotNull(it.body).byteStream().readBounded(MAX_PACK_BYTES)
        }

    internal suspend fun refresh(
        context: Context,
        download: () -> ByteArray = ::downloadPack
    ): CatalogRefreshResult = withContext(Dispatchers.IO) {
        refreshLock.withLock {
            try {
                load(context)
                val current = requireNotNull(snapshot)
                val bytes = download()
                require(bytes.size <= MAX_PACK_BYTES)
                val pack = ExerciseCatalogPack.read(bytes.inputStream())
                if (pack.version == current.version) return@withLock CatalogRefreshResult.CURRENT
                require(pack.version > current.version)
                val updated = validatePack(pack, current)
                val disk = diskPack(context)
                val output = disk.startWrite()
                try {
                    output.write(bytes)
                    disk.finishWrite(output)
                } catch (error: Exception) {
                    disk.failWrite(output)
                    throw error
                }
                publish(updated)
                CatalogRefreshResult.UPDATED
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                Log.w("ExerciseSources", "Catalog refresh failed", error)
                CatalogRefreshResult.FAILED
            }
        }
    }

    private fun publish(updated: Snapshot) {
        snapshot = updated
        catalogVersion.value = updated.version
    }

    private fun readPack(bytes: ByteArray, current: Snapshot): Snapshot =
        validatePack(ExerciseCatalogPack.read(bytes.inputStream()), current)

    private fun validatePack(pack: ExerciseCatalogPack, current: Snapshot): Snapshot {
        val updated = parseSnapshot(pack.data, pack.version, pack.atlas, pack.tileCount())
        require(updated.byId.keys.containsAll(current.byId.keys)) { "Catalog removed existing exercise IDs" }
        return updated
    }

    private fun parseSnapshot(
        catalog: JSONObject,
        version: Int = 0,
        atlas: ByteArray? = null,
        tileCount: Int = Int.MAX_VALUE
    ): Snapshot {
        val openRevision = catalog.getString("openRevision")
        require(openRevision.matches(Regex("[0-9a-f]{40}")))
        val rows = catalog.getJSONArray("exercises")
        require(rows.length() in 1..25_000)
        val entries = List(rows.length()) { parse(rows.getJSONObject(it), openRevision, tileCount) }
        val byId = entries.associateBy { it.definition.id }
        require(entries.size == byId.size && byId.keys.none { ExerciseCatalog.isBuiltIn(it) })
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
        val media = buildMap {
            val mediaRows = if (catalog.has("media")) catalog.getJSONArray("media") else JSONArray()
            require(mediaRows.length() <= 25_000)
            for (i in 0 until mediaRows.length()) {
                val mediaEntry = parseMedia(mediaRows.getJSONObject(i))
                require(mediaEntry.exerciseId in byId || ExerciseCatalog.isBuiltIn(mediaEntry.exerciseId))
                require(put(mediaEntry.exerciseId, mediaEntry) == null)
            }
        }
        return Snapshot(byId, entries.map { it.definition }, photos, media, atlas, version)
    }

    private fun parseMedia(row: JSONObject): ExerciseMediaManifestEntry {
        val type = AssetMediaType.valueOf(row.getString("type"))
        require(type in setOf(AssetMediaType.PNG_SINGLE, AssetMediaType.WEBP_SINGLE,
            AssetMediaType.JPEG_SINGLE, AssetMediaType.GIF, AssetMediaType.MP4))
        val hash = row.getString("sha256")
        require(hash.matches(Regex("[0-9a-f]{64}")))
        val size = row.getLong("bytes")
        require(size in 1..(if (type == AssetMediaType.MP4) 30_000_000L else 5_000_000L))
        val license = row.getString("license")
        val attribution = row.getString("attribution")
        require(license.isNotBlank() && attribution.isNotBlank())
        val licenseUrl = requireNotNull(row.getString("licenseUrl").toHttpUrlOrNull())
        require(licenseUrl.isHttps && licenseUrl.username.isEmpty() && licenseUrl.password.isEmpty())
        return ExerciseMediaManifestEntry(
            exerciseId = row.getString("exerciseId"),
            sourceType = AssetSourceType.CATALOG_FEED,
            sourceSlug = null,
            mediaType = type,
            primaryUrl = pinnedPublicMediaUrl(row.getString("url")),
            version = hash,
            licenseName = license,
            attribution = attribution,
            licenseUrl = licenseUrl.toString(),
            expectedBytes = size,
            expectedSha256 = hash,
            defaultStatus = AssetDownloadStatus.AVAILABLE
        )
    }

    private fun parse(row: JSONObject, openRevision: String, tileCount: Int): ExternalExercise {
        val id = row.getString("id")
        require(id.matches(Regex("[A-Za-z0-9_-]{1,120}")))
        val source = when {
            id.startsWith("hasan-") -> ExerciseSource.HASAN
            id.startsWith("EX-") -> ExerciseSource.OPEN_EXERCISE_BASE
            else -> ExerciseSource.OTHER
        }
        val equipment = row.getJSONArray("equipment").strings().map(::equipmentKey).distinct()
        val muscles = row.getJSONArray("muscles").strings()
        val regions = row.getJSONArray("body").strings()
        val steps = row.getJSONArray("steps").strings()
        val name = row.getString("name").replaceFirstChar { it.titlecase() }
        require(name.isNotBlank() && name.length <= 200)
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
            nameFa = row.optString("nameFa").takeIf { it.isNotBlank() && it.length <= 200 } ?: name,
            category = category,
            descriptionEn = "",
            descriptionFa = "",
            targetMusclesEn = muscles,
            targetMusclesFa = row.optJSONArray("musclesFa")?.strings() ?: muscles,
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
            instructionsFa = row.optJSONArray("stepsFa")?.strings() ?: steps,
            commonMistakesEn = emptyList(),
            commonMistakesFa = emptyList(),
            observableFormChecksEn = emptyList(),
            observableFormChecksFa = emptyList(),
            limitationsEn = emptyList(),
            limitationsFa = emptyList(),
            implementationStatus = ImplementationStatus.MANUAL_ONLY
        )
        val photo = if (row.has("tile")) {
            val tile = row.getInt("tile")
            require(tile in 0 until tileCount)
            val path = row.optString("path").takeIf(String::isNotBlank)
            val bytes = row.optLong("size")
            val attribution = if (path != null) {
                require(bytes in 1..2_000_000L && source == ExerciseSource.OPEN_EXERCISE_BASE &&
                    path.matches(Regex("images/[A-Za-z0-9_-]+/[A-Za-z0-9_.-]+\\.png")))
                OPEN_ATTRIBUTION
            } else {
                row.getString("tileAttribution").also { require(it.isNotBlank()) }
            }
            ExternalPhoto(tile, path, bytes, row.optString("resolution"), openRevision, attribution)
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
