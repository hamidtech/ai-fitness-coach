package com.aifitnesscoach.core.media

import com.aifitnesscoach.core.exercise.definitions.ExerciseCatalog
import com.aifitnesscoach.core.exercise.definitions.ExternalExerciseSources
import java.io.File

/**
 * Curated offline-capable media manifest for all 39 exercises.
 *
 * Uses verified 1-to-1 RepDB Free Tier v1.0 flat WebP posture images where exact equipment
 * and movement match exist. Unmatched resistance-band exercises are explicitly marked
 * [AssetDownloadStatus.NOT_AVAILABLE] rather than substituting wrong equipment (e.g., dumbbells).
 */
object ExerciseAssetCatalog {

    const val REPDB_BASE_URL = "https://raw.githubusercontent.com/RepDB/exercise-dataset/f12da458a90f353a86c953f204304d5a8bd98275/images/flat"
    const val REPDB_ATTRIBUTION = "Exercise data by RepDB (https://repdb.co)"
    const val REPDB_LICENSE = "RepDB Free Tier License v1.0 (free with attribution)"
    const val EXERCISEDB_ATTRIBUTION = "ExerciseDB OSS API (AscendAPI) — Non-commercial attribution required"

    private fun repDbPair(exerciseId: String, slug: String, bytes: Long) = ExerciseMediaManifestEntry(
        exerciseId = exerciseId,
        sourceType = AssetSourceType.REPDB_FREE_TIER,
        sourceSlug = slug,
        mediaType = AssetMediaType.WEBP_IMAGE_PAIR,
        primaryUrl = "$REPDB_BASE_URL/$slug-start.webp",
        secondaryUrl = "$REPDB_BASE_URL/$slug-peak.webp",
        licenseName = REPDB_LICENSE,
        attribution = REPDB_ATTRIBUTION,
        expectedBytes = bytes,
        resolution = "512 × 512",
        licenseUrl = "https://github.com/RepDB/exercise-dataset/blob/f12da458a90f353a86c953f204304d5a8bd98275/LICENSE-DATA.md",
        defaultStatus = AssetDownloadStatus.AVAILABLE
    )

    private fun repDbSingle(exerciseId: String, slug: String, bytes: Long) = ExerciseMediaManifestEntry(
        exerciseId = exerciseId,
        sourceType = AssetSourceType.REPDB_FREE_TIER,
        sourceSlug = slug,
        mediaType = AssetMediaType.WEBP_SINGLE,
        primaryUrl = "$REPDB_BASE_URL/$slug-main.webp",
        secondaryUrl = null,
        licenseName = REPDB_LICENSE,
        attribution = REPDB_ATTRIBUTION,
        expectedBytes = bytes,
        resolution = "512 × 512",
        licenseUrl = "https://github.com/RepDB/exercise-dataset/blob/f12da458a90f353a86c953f204304d5a8bd98275/LICENSE-DATA.md",
        defaultStatus = AssetDownloadStatus.AVAILABLE
    )

    private fun notAvailableBand(exerciseId: String) = ExerciseMediaManifestEntry(
        exerciseId = exerciseId,
        sourceType = AssetSourceType.NONE,
        sourceSlug = null,
        mediaType = AssetMediaType.NONE,
        primaryUrl = null,
        secondaryUrl = null,
        licenseName = "",
        attribution = "",
        defaultStatus = AssetDownloadStatus.NOT_AVAILABLE,
        unavailableReasonEn = "No verified resistance-band pack asset is available in the free dataset yet. You can import your own MP4/GIF/image file.",
        unavailableReasonFa = "هنوز فایل رایگان تأییدشده با کش مقاومتی برای این حرکت در بسته وجود ندارد. می‌توانید فایل تمرینی محلی (MP4/GIF/تصویر) وارد کنید."
    )

    val entriesByExerciseId: Map<String, ExerciseMediaManifestEntry> = listOf(
        // Category A — Weighted Training (8/8 matched in RepDB)
        repDbPair("dumbbell_biceps_curl", "bicep-curl", 27804L),
        repDbPair("dumbbell_shoulder_press", "dumbbell-shoulder-press", 24932L),
        repDbPair("dumbbell_lateral_raise", "lateral-raise", 27592L),
        repDbPair("dumbbell_bent_over_row", "bent-over-db-row", 32774L),
        repDbPair("goblet_squat", "db-goblet-squat", 24116L),
        repDbPair("dumbbell_triceps_extension", "dumbbell-tricep-extension", 18382L),
        repDbPair("dumbbell_romanian_deadlift", "dumbbell-romanian-deadlift", 24078L),
        repDbPair("dumbbell_chest_press", "db-bench-press", 27742L),

        // Category B — Bodyweight Training (8/8 matched in RepDB)
        repDbPair("bodyweight_squat", "bodyweight-squat", 27050L),
        repDbPair("push_up", "push-up", 29834L),
        repDbPair("forward_lunge", "lunge", 24248L),
        repDbPair("reverse_lunge", "reverse-lunge", 29616L),
        repDbPair("glute_bridge", "glute-bridge", 24136L),
        repDbPair("standing_calf_raise", "standing-calf-raise", 47034L),
        repDbSingle("wall_sit", "wall-sit", 13132L),
        repDbPair("superman", "superman", 20716L),

        // Category C — Core Training (8/8 matched in RepDB)
        repDbSingle("forearm_plank", "plank", 15364L),
        repDbSingle("side_plank", "side-plank", 14396L),
        repDbPair("dead_bug", "dead-bug", 28284L),
        repDbPair("bird_dog", "bird-dog", 20782L),
        repDbPair("abdominal_crunch", "crunches", 24602L),
        repDbPair("reverse_crunch", "reverse-crunches", 29566L),
        repDbPair("mountain_climber", "mountain-climbers", 28520L),
        repDbSingle("hollow_body_hold", "hollow-body-hold", 15534L),

        // Category D — Resistance Band Training (9/15 exact matches in RepDB)
        repDbPair("band_row", "banded-standing-row", 27264L),
        repDbPair("band_squat", "banded-squat", 34746L),
        repDbPair("band_good_morning", "banded-good-morning", 35230L),
        repDbPair("band_pull_apart", "band-pull-apart", 24974L),
        repDbPair("mini_band_lateral_walk", "banded-lateral-walk", 29070L),
        repDbPair("mini_band_clamshell", "banded-clamshell", 27844L),
        repDbPair("band_pallof_press", "banded-pallof-press", 25512L),
        repDbPair("band_shoulder_press", "banded-overhead-press", 25512L),

        repDbPair("band_biceps_curl", "banded-standing-curl", 22346L),
        notAvailableBand("band_lateral_raise"),
        notAvailableBand("band_chest_press"),
        notAvailableBand("band_triceps_extension"),
        notAvailableBand("band_glute_kickback"),
        notAvailableBand("band_face_pull"),
        notAvailableBand("band_dead_bug")
    ).associateBy { it.exerciseId }

    fun entryFor(exerciseId: String): ExerciseMediaManifestEntry {
        ExternalExerciseSources.media(exerciseId)?.let { return it }
        entriesByExerciseId[exerciseId]?.let { return it }
        val upstream = ExternalExerciseSources.entry(exerciseId)
        val photo = ExternalExerciseSources.photo(exerciseId)
        if (photo?.path != null) return ExerciseMediaManifestEntry(
            exerciseId = exerciseId,
            sourceType = AssetSourceType.OPEN_EXERCISE_BASE,
            sourceSlug = photo.path,
            mediaType = AssetMediaType.PNG_SINGLE,
            primaryUrl = "https://raw.githubusercontent.com/OpenExerciseBase/OpenExerciseBase-Database/" +
                "${photo.revision}/${photo.path}",
            version = photo.revision,
            licenseName = ExternalExerciseSources.OPEN_LICENSE,
            attribution = ExternalExerciseSources.OPEN_ATTRIBUTION,
            licenseUrl = ExternalExerciseSources.OPEN_LICENSE_URL,
            resolution = photo.resolution,
            expectedBytes = photo.bytes,
            defaultStatus = AssetDownloadStatus.AVAILABLE
        )
        return ExerciseMediaManifestEntry(
            exerciseId = exerciseId,
            sourceType = AssetSourceType.NONE,
            sourceSlug = null,
            mediaType = AssetMediaType.NONE,
            primaryUrl = null,
            defaultStatus = AssetDownloadStatus.NOT_AVAILABLE,
            unavailableReasonEn = upstream?.let { "Licensed online media is unavailable. You can import a personal photo, GIF or MP4." },
            unavailableReasonFa = upstream?.let { "رسانهٔ دارای مجوز در دسترس نیست. می‌توانید عکس، گیف یا ویدیوی شخصی وارد کنید." }
        )
    }

    fun allDownloadableExerciseIds(): List<String> = ExerciseCatalog.allExercises
        .map { it.id }
        .filter { entryFor(it).defaultStatus == AssetDownloadStatus.AVAILABLE }

    /**
     * Resolves a target path strictly confined under [baseDir] to prevent path-traversal attacks.
     */
    fun resolveConfinedMediaFile(baseDir: File, relativeFileName: String): File {
        val sanitized = relativeFileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        require(sanitized.isNotBlank() && !sanitized.contains("..")) {
            "Invalid media file name"
        }
        val canonicalBase = baseDir.canonicalFile
        val target = File(canonicalBase, sanitized).canonicalFile
        require(target.path.startsWith(canonicalBase.path + File.separator) || target == canonicalBase) {
            "Path escapes media directory"
        }
        return target
    }
}
