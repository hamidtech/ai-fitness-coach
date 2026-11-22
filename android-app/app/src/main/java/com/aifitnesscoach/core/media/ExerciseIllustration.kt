package com.aifitnesscoach.core.media

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

private data class ExercisePose(
    val head: Int,
    val shoulders: Int,
    val hips: Int,
    val nearArm: List<Int>,
    val farArm: List<Int>,
    val nearLeg: List<Int>,
    val farLeg: List<Int>,
    val props: List<List<Int>> = emptyList(),
    val band: List<Int> = emptyList()
)

private fun limb(vararg points: Int) = points.toList()
private val standingNear = limb(5063, 5579, 5594)
private val standingFar = limb(5063, 4579, 4394)

private val curl = ExercisePose(5021, 5036, 5063, limb(5036, 5758, 6440), limb(5036, 4358, 3640), standingNear, standingFar)
private val press = ExercisePose(5024, 5040, 5065, limb(5040, 6733, 6910), limb(5040, 3333, 3110), standingNear, standingFar)
private val raise = ExercisePose(5022, 5038, 5064, limb(5038, 6839, 8635), limb(5038, 3239, 1435), standingNear, standingFar)
private val squat = ExercisePose(6429, 5943, 3969, limb(5943, 6955, 8252), limb(5943, 6551, 7947), limb(3969, 6669, 5892), limb(3969, 5569, 4692))
private val hinge = ExercisePose(7528, 6539, 4554, limb(6539, 6958, 6875), limb(6539, 6159, 5974), limb(4554, 5274, 4993), limb(4554, 4474, 4093))
private val extension = ExercisePose(5428, 4939, 4965, limb(4939, 5015, 3519), limb(4939, 5415, 3922), standingNear, standingFar)
private val bug = ExercisePose(1874, 2978, 5381, limb(2978, 4864, 6347), limb(2978, 3158, 3041), limb(5381, 5961, 7847), limb(5381, 7677, 9082))

private fun dumbbells(pose: ExercisePose) = pose.copy(
    props = pose.props + listOf(pose.nearArm.last(), pose.farArm.last()).distinct().map { hand ->
        limb(hand - 300, hand + 300)
    }
)

private val illustrations = mapOf(
    "dumbbell_biceps_curl" to dumbbells(curl),
    "dumbbell_shoulder_press" to dumbbells(press),
    "dumbbell_lateral_raise" to dumbbells(raise),
    "dumbbell_bent_over_row" to dumbbells(hinge.copy(nearArm = limb(6539, 4954, 6749), farArm = limb(6539, 4350, 6145))),
    "goblet_squat" to dumbbells(squat.copy(nearArm = limb(5943, 6462, 7144), farArm = limb(5943, 6062, 7144))),
    "dumbbell_triceps_extension" to dumbbells(extension),
    "dumbbell_romanian_deadlift" to dumbbells(hinge),
    "dumbbell_chest_press" to dumbbells(ExercisePose(2071, 3274, 6374, limb(3274, 3855, 3829), limb(3274, 2954, 2929), limb(6374, 8174, 8694), limb(6374, 7273, 7394), props = listOf(limb(1579, 7979), limb(2979, 2994), limb(6879, 6894)))),
    "bodyweight_squat" to squat,
    "push_up" to ExercisePose(8249, 6955, 4459, limb(6955, 6969, 8090), limb(6955, 6068, 7090), limb(4459, 2969, 1689), limb(4459, 2567, 1189)),
    "forward_lunge" to ExercisePose(5926, 5539, 4962, limb(5539, 5155, 4962), limb(5539, 5960, 5363), limb(4962, 7470, 7793), limb(4962, 3664, 1693)),
    "reverse_lunge" to ExercisePose(5327, 5039, 5061, limb(5039, 4255, 4864), limb(5039, 6360, 5265), limb(5061, 2870, 1791), limb(5061, 6276, 8293)),
    "glute_bridge" to ExercisePose(1781, 3082, 5658, limb(3082, 4568, 5781), limb(3082, 4668, 6083), limb(5658, 7262, 8589), limb(5658, 6668, 7789)),
    "standing_calf_raise" to ExercisePose(5220, 5036, 4960, limb(5036, 4854, 4959), limb(5036, 6150, 7052), limb(4960, 5377, 5486, 6390), limb(4960, 4377, 4486, 5290), props = listOf(limb(4692, 7192))),
    "wall_sit" to ExercisePose(4028, 4142, 4168, limb(4142, 5357, 6465), limb(4142, 5162, 6168), limb(4168, 6969, 7093), limb(4168, 6269, 6293), props = listOf(limb(3517, 3594))),
    "superman" to ExercisePose(7154, 6264, 4378, limb(6264, 7660, 9054), limb(6264, 7465, 8759), limb(4378, 2669, 1163), limb(4378, 2869, 1760)),
    "forearm_plank" to ExercisePose(7856, 6864, 4268, limb(6864, 6889, 8589), limb(6864, 6288, 7888), limb(4268, 2876, 1389), limb(4268, 2376, 1088)),
    "side_plank" to ExercisePose(7946, 7054, 4669, limb(7054, 7189, 8989), limb(7054, 6740, 6423), limb(4669, 2877, 1289), limb(4669, 2678, 1385)),
    "dead_bug" to bug,
    "bird_dog" to ExercisePose(7852, 6759, 4459, limb(6759, 7480, 8288), limb(6759, 8058, 9555), limb(4459, 3678, 2287), limb(4459, 2857, 1157)),
    "abdominal_crunch" to ExercisePose(2760, 3669, 5583, limb(3669, 4864, 3370), limb(3669, 4259, 3162), limb(5583, 7158, 8589), limb(5583, 6559, 7789)),
    "reverse_crunch" to ExercisePose(1880, 3181, 5770, limb(3181, 4786, 6089), limb(3181, 4588, 5689), limb(5770, 5048, 6643), limb(5770, 5848, 7347)),
    "mountain_climber" to ExercisePose(7951, 6858, 4363, limb(6858, 7380, 8089), limb(6858, 6780, 7389), limb(4363, 6869, 4985), limb(4363, 2673, 1289)),
    "hollow_body_hold" to ExercisePose(1968, 3073, 5285, limb(3073, 2370, 1059), limb(3073, 2674, 1364), limb(5285, 7176, 9067), limb(5285, 6978, 8669)),
    "band_biceps_curl" to curl.copy(band = limb(3640, 4394, 5594, 6440)),
    "band_shoulder_press" to press.copy(band = limb(3110, 4394, 5594, 6910)),
    "band_lateral_raise" to raise.copy(band = limb(1435, 4994, 8635)),
    "band_row" to curl.copy(nearArm = limb(5036, 4553, 6149), farArm = limb(5036, 4250, 5846), band = limb(6149, 8750, 5846), props = listOf(limb(8729, 8794))),
    "band_chest_press" to curl.copy(nearArm = limb(5036, 6638, 8041), farArm = limb(5036, 6434, 7836), band = limb(8041, 4537, 1639, 4332, 7836), props = listOf(limb(1618, 1694))),
    "band_triceps_extension" to extension.copy(band = limb(3519, 4394)),
    "band_squat" to squat.copy(nearArm = limb(5943, 6557, 6541), farArm = limb(5943, 5957, 6040), band = limb(6541, 5892, 4692, 6040)),
    "band_good_morning" to hinge.copy(nearArm = limb(6539, 6649, 7340), farArm = limb(6539, 6047, 6937), band = limb(6539, 4993, 4093, 6539)),
    "band_pull_apart" to raise.copy(band = limb(1435, 5036, 8635)),
    "mini_band_lateral_walk" to ExercisePose(5527, 5342, 4964, limb(5342, 6755, 6264), limb(5342, 3954, 4564), limb(4964, 6677, 7993), limb(4964, 3477, 2893), band = limb(3085, 7385, 6780, 3280, 3085)),
    "band_glute_kickback" to ExercisePose(6529, 5741, 4761, limb(5741, 6354, 8060), limb(5741, 6752, 7857), limb(4761, 5078, 4993), limb(4761, 2963, 1668), props = listOf(limb(8138, 8194)), band = limb(1668, 3379, 4890, 3375, 1668)),
    "mini_band_clamshell" to ExercisePose(1773, 3180, 5682, limb(3180, 2589, 1580), limb(3180, 4882, 4889), limb(5682, 6660, 8390), limb(5682, 7587, 8389), band = limb(6164, 7066, 7084, 6384, 6164)),
    "band_face_pull" to curl.copy(nearArm = limb(5036, 6238, 5531), farArm = limb(5036, 4540, 5030), band = limb(5531, 8734, 5030), props = listOf(limb(8714, 8794))),
    "band_pallof_press" to curl.copy(nearArm = limb(5036, 6247, 7949), farArm = limb(5036, 6142, 7949), band = limb(1650, 7949), props = listOf(limb(1631, 1694))),
    "band_dead_bug" to bug.copy(band = limb(6347, 7847))
)

val illustratedExerciseIds: Set<String> get() = illustrations.keys

@Composable
fun ExerciseIllustration(exerciseId: String, modifier: Modifier = Modifier) {
    val pose = illustrations[exerciseId] ?: return
    val colors = MaterialTheme.colorScheme
    Canvas(modifier.clip(RoundedCornerShape(12.dp)).background(colors.secondaryContainer)) {
        val scale = size.minDimension * 0.0092f
        val origin = center - Offset(50 * scale, 50 * scale)
        fun at(point: Int) = origin + Offset((point / 100) * scale, (point % 100) * scale)
        fun trace(points: List<Int>, color: androidx.compose.ui.graphics.Color, width: Float) {
            points.zipWithNext().forEach { (start, end) ->
                drawLine(color, at(start), at(end), width * scale, StrokeCap.Round)
            }
        }
        drawCircle(colors.surface.copy(alpha = 0.55f), 40 * scale, center)
        trace(listOf(796, 9396), colors.outlineVariant, 2f)
        pose.props.forEach { trace(it, colors.tertiary, 5f) }
        trace(pose.farArm, colors.secondary, 5f)
        trace(pose.farLeg, colors.secondary, 7f)
        trace(listOf(pose.shoulders, pose.hips), colors.primary, 11f)
        trace(pose.nearLeg, colors.onSecondaryContainer, 8f)
        trace(pose.nearArm, colors.onSecondaryContainer, 6f)
        drawCircle(colors.onSecondaryContainer, 7 * scale, at(pose.head))
        trace(pose.band, colors.tertiary, 3f)
    }
}
