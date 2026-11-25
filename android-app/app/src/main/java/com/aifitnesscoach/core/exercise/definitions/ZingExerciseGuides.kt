package com.aifitnesscoach.core.exercise.definitions

/**
 * External reference links verified against Zing's public exercise guide pages.
 * Existing catalog IDs retain their original sources, thumbnails, and licenses.
 */
object ZingExerciseGuides {
    const val libraryUrl = "https://www.zing.coach/exercises"
    private val slugs = mapOf(
        "aifc-high-plank" to "high-plank",
        "aifc-child-pose" to "child-pose",
        "aifc-seated-forward-fold" to "seated-forward-fold",
        "aifc-wide-push-up" to "wide-push-ups",
        "EX-downward-dog-01KZ3V3DYND08RZF4CTKT9VFTB" to "downward-facing-dog",
        "EX-cat-cow-spinal-mobility-43SMTHQT8TZHH3AY1ACDWDP3H4" to "cat-cow-stretch",
        "hasan-0259" to "narrow-push-ups",
        "push_up" to "push-ups",
        "dumbbell_shoulder_press" to "dumbbell-overhead-press",
        "dumbbell_bent_over_row" to "dumbbell-bent-over-rows",
        "dumbbell_lateral_raise" to "dumbbell-lateral-raises",
        "dumbbell_biceps_curl" to "dumbbell-bicep-curls",
        "bodyweight_squat" to "squats",
        "glute_bridge" to "glute-bridges",
        "standing_calf_raise" to "calf-raises",
        "forearm_plank" to "plank",
        "mountain_climber" to "mountain-climbers",
        "abdominal_crunch" to "crunches",
        "goblet_squat" to "dumbbell-goblet-squats",
        "reverse_lunge" to "reverse-lunges",
        "dumbbell_romanian_deadlift" to "dumbbell-romanian-deadlifts",
        "bird_dog" to "bird-dogs",
        "reverse_crunch" to "reverse-crunches",
        "band_pull_apart" to "band-pull-aparts",
        "band_biceps_curl" to "band-bicep-curls",
        "hasan-0025" to "barbell-bench-press",
        "hasan-0031" to "barbell-biceps-curls",
        "hasan-0289" to "dumbbell-bench-press",
        "hasan-0017" to "assisted-pull-ups",
        "hasan-1326" to "chin-ups",
        "hasan-0279" to "decline-push-ups",
        "hasan-0283" to "diamond-push-ups",
        "hasan-2292" to "dumbbell-rear-delt-raises",
        "hasan-0406" to "dumbbell-shrugs",
        "hasan-0652" to "pull-ups",
        "EX-burpee-01KZ3V3DYJQ0B83JNMDWZ8R58V" to "burpees",
        "EX-high-knees-01KZ3V3DYQG2ZAZAGXM9YXKZ47" to "high-knees",
        "EX-jump-rope-1P0S66WME2NR9F760HGMWB1WVH" to "jump-rope",
        "EX-jumping-jacks-01KZ3V3DYVRGF8XTCP211A7W96" to "jumping-jacks",
        "EX-lat-pulldown-60Y3WQ802BGA7R7H1MPEMAGSN9" to "lat-pull-downs",
        "EX-russian-twist-01KZ3V3DYQG2ZAZAGXM9YXKZ4A" to "russian-twists",
        "EX-tricep-dips-01KZ3V3DYND08RZF4CTKT9VFTG" to "tricep-dips",
        "EX-v-up-01KZ3V3DYPMJ0VD30AKHABF5P6" to "v-ups",
        "EX-knee-push-ups-01KZ3V3DYR43D2FTV12ZVQHG11" to "knee-push-ups",
        "EX-leg-raises-01KZ3V3DYND08RZF4CTKT9VFT9" to "leg-raises",
        "EX-jump-squat-01KZ3V3DYPMJ0VD30AKHABF5P0" to "squat-jumps",
        "EX-leg-press-machine-7KHB19HHE2XFNZVXR6EPBD3PSA" to "leg-press",
        "EX-low-step-up-418FG27WCJQQT1WK21QW4JDBXS" to "step-ups"
    )

    fun urlFor(exerciseId: String): String? = slugs[exerciseId]?.let { "$libraryUrl/$it" }
}
