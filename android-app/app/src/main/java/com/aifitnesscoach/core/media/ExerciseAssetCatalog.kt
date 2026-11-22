package com.aifitnesscoach.core.media

import com.aifitnesscoach.core.exercise.definitions.ExerciseCatalog
import java.io.File

/**
 * Curated offline-capable media manifest for all 39 exercises.
 *
 * Uses verified 1-to-1 RepDB Free Tier v1.0 flat WebP posture images where exact equipment
 * and movement match exist. Unmatched resistance-band exercises are explicitly marked
 * [AssetDownloadStatus.NOT_AVAILABLE] rather than substituting wrong equipment (e.g., dumbbells).
 */
object ExerciseAssetCatalog {

    const val REPDB_BASE_URL = "https://raw.githubusercontent.com/RepDB/exercise-dataset/main/images/flat"
    const val REPDB_ATTRIBUTION = "Exercise data by RepDB (https://repdb.co)"
    const val REPDB_LICENSE = "RepDB Free Tier License v1.0 (free with attribution)"
    const val EXERCISEDB_ATTRIBUTION = "ExerciseDB OSS API (AscendAPI) — Non-commercial attribution required"

    private fun repDbPair(exerciseId: String, slug: String) = ExerciseMediaManifestEntry(
        exerciseId = exerciseId,
        sourceType = AssetSourceType.REPDB_FREE_TIER,
        sourceSlug = slug,
        mediaType = AssetMediaType.WEBP_IMAGE_PAIR,
        primaryUrl = "$REPDB_BASE_URL/$slug-start.webp",
        secondaryUrl = "$REPDB_BASE_URL/$slug-peak.webp",
        licenseName = REPDB_LICENSE,
        attribution = REPDB_ATTRIBUTION,
        defaultStatus = AssetDownloadStatus.AVAILABLE
    )

    private fun repDbSingle(exerciseId: String, slug: String) = ExerciseMediaManifestEntry(
        exerciseId = exerciseId,
        sourceType = AssetSourceType.REPDB_FREE_TIER,
        sourceSlug = slug,
        mediaType = AssetMediaType.WEBP_SINGLE,
        primaryUrl = "$REPDB_BASE_URL/$slug-main.webp",
        secondaryUrl = null,
        licenseName = REPDB_LICENSE,
        attribution = REPDB_ATTRIBUTION,
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
        repDbPair("dumbbell_biceps_curl", "bicep-curl"),
        repDbPair("dumbbell_shoulder_press", "dumbbell-shoulder-press"),
        repDbPair("dumbbell_lateral_raise", "lateral-raise"),
        repDbPair("dumbbell_bent_over_row", "bent-over-db-row"),
        repDbPair("goblet_squat", "db-goblet-squat"),
        repDbPair("dumbbell_triceps_extension", "dumbbell-tricep-extension"),
        repDbPair("dumbbell_romanian_deadlift", "dumbbell-romanian-deadlift"),
        repDbPair("dumbbell_chest_press", "db-bench-press"),

        // Category B — Bodyweight Training (8/8 matched in RepDB)
        repDbPair("bodyweight_squat", "bodyweight-squat"),
        repDbPair("push_up", "push-up"),
        repDbPair("forward_lunge", "lunge"),
        repDbPair("reverse_lunge", "reverse-lunge"),
        repDbPair("glute_bridge", "glute-bridge"),
        repDbPair("standing_calf_raise", "standing-calf-raise"),
        repDbSingle("wall_sit", "wall-sit"),
        repDbPair("superman", "superman"),

        // Category C — Core Training (8/8 matched in RepDB)
        repDbSingle("forearm_plank", "plank"),
        repDbSingle("side_plank", "side-plank"),
        repDbPair("dead_bug", "dead-bug"),
        repDbPair("bird_dog", "bird-dog"),
        repDbPair("abdominal_crunch", "crunches"),
        repDbPair("reverse_crunch", "reverse-crunches"),
        repDbPair("mountain_climber", "mountain-climbers"),
        repDbSingle("hollow_body_hold", "hollow-body-hold"),

        // Category D — Resistance Band Training (8/15 exact matches in RepDB; 7 honest NOT_AVAILABLE)
        repDbPair("band_row", "banded-standing-row"),
        repDbPair("band_squat", "banded-squat"),
        repDbPair("band_good_morning", "banded-good-morning"),
        repDbPair("band_pull_apart", "band-pull-apart"),
        repDbPair("mini_band_lateral_walk", "banded-lateral-walk"),
        repDbPair("mini_band_clamshell", "banded-clamshell"),
        repDbPair("band_pallof_press", "banded-pallof-press"),
        repDbPair("band_shoulder_press", "banded-overhead-press"),

        notAvailableBand("band_biceps_curl"),
        notAvailableBand("band_lateral_raise"),
        notAvailableBand("band_chest_press"),
        notAvailableBand("band_triceps_extension"),
        notAvailableBand("band_glute_kickback"),
        notAvailableBand("band_face_pull"),
        notAvailableBand("band_dead_bug")
    ).associateBy { it.exerciseId }

    fun entryFor(exerciseId: String): ExerciseMediaManifestEntry {
        return entriesByExerciseId[exerciseId] ?: ExerciseMediaManifestEntry(
            exerciseId = exerciseId,
            sourceType = AssetSourceType.NONE,
            sourceSlug = null,
            mediaType = AssetMediaType.NONE,
            primaryUrl = null,
            defaultStatus = AssetDownloadStatus.NOT_AVAILABLE
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
