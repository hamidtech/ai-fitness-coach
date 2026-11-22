package com.aifitnesscoach.core.exercise

import com.aifitnesscoach.core.pose.PoseFrame
import com.aifitnesscoach.core.pose.TrackingQuality

enum class MovementPhase(val labelEn: String, val labelFa: String) {
    IDLE("Ready", "آماده"),
    STANDING("Standing", "ایستاده"),
    DESCENDING("Descending", "پایین آمدن"),
    BOTTOM("Bottom Position", "پایین‌ترین نقطه"),
    ASCENDING("Ascending", "بالا آمدن"),
    FLEXING("Flexing", "خم کردن"),
    PEAK_FLEXION("Peak Flexion", "اوج انقباض"),
    EXTENDING("Extending", "باز کردن"),
    HOLDING("Holding", "در حال نگهداشتن"),
    PAUSED_TRACKING_LOST("Paused — Tracking Lost", "متوقف — خطای ردیابی")
}

enum class FeedbackSeverity {
    INFO,
    WARNING
}

enum class CoachingMessageKey {
    READY,
    SQUAT_DESCENDING,
    SQUAT_GOOD_DEPTH,
    SQUAT_SHALLOW,
    CURL_FLEXING,
    CURL_TOP,
    CURL_PARTIAL,
    CURL_ELBOW_DRIFT,
    PLANK_HOLDING,
    PLANK_HIP_SAG,
    PLANK_HIP_HIGH,
    TRACKING_LOW_CONFIDENCE,
    TRACKING_PAUSED
}

data class FormObservation(
    val key: CoachingMessageKey,
    val severity: FeedbackSeverity,
    val timestampMs: Long,
    val detailEn: String,
    val detailFa: String
)

data class AnalysisResult(
    val exerciseId: String,
    val phase: MovementPhase,
    val repCount: Int,
    val holdDurationMs: Long,
    val trackingQuality: TrackingQuality,
    val primaryAngleDegrees: Float?,
    val secondaryAngleDegrees: Float?,
    val activeFeedbackKey: CoachingMessageKey?,
    val observations: List<FormObservation>,
    val uncertaintyWarningEn: String? = null,
    val uncertaintyWarningFa: String? = null
)

interface ExerciseAnalyzer {
    val exerciseId: String
    fun reset()
    fun processFrame(frame: PoseFrame): AnalysisResult
}

/**
 * Clean abstraction for future small on-device LLM coaching explanations.
 * Real-time repetition counting and form evaluation remain strictly deterministic
 * and independent of any language model.
 */
interface CoachExplanationProvider {
    fun formatCoachingTip(
        key: CoachingMessageKey,
        isPersian: Boolean
    ): String
}

class DeterministicCoachExplanationProvider : CoachExplanationProvider {
    override fun formatCoachingTip(key: CoachingMessageKey, isPersian: Boolean): String {
        return when (key) {
            CoachingMessageKey.READY ->
                if (isPersian) "وضعیت آماده است. حرکت را شروع کنید."
                else "Position ready. Begin movement."
            CoachingMessageKey.SQUAT_DESCENDING ->
                if (isPersian) "در حال پایین آمدن… سینه را بالا نگه دارید."
                else "Lowering… keep chest lifted."
            CoachingMessageKey.SQUAT_GOOD_DEPTH ->
                if (isPersian) "عمق اسکوات مناسب است. با کنترل بالا بیایید."
                else "Good squat depth. Drive up smoothly."
            CoachingMessageKey.SQUAT_SHALLOW ->
                if (isPersian) "تکرار ناقص — در صورت راحتی کمی پایین‌تر بروید."
                else "Incomplete rep — lower hips slightly deeper if comfortable."
            CoachingMessageKey.CURL_FLEXING ->
                if (isPersian) "در حال خم کردن آرنج با کنترل."
                else "Curling up smoothly."
            CoachingMessageKey.CURL_TOP ->
                if (isPersian) "بالاترین نقطه حرکت. به‌آرامی پایین بیاورید."
                else "Full flexion reached. Lower with control."
            CoachingMessageKey.CURL_PARTIAL ->
                if (isPersian) "تکرار ناقص — دامنه کامل خم و باز شدن آرنج را انجام دهید."
                else "Incomplete curl — aim for full flexion and extension."
            CoachingMessageKey.CURL_ELBOW_DRIFT ->
                if (isPersian) "بازو را ثابت‌تر در کنار تنه نگه دارید."
                else "Keep upper arm steadier near your torso."
            CoachingMessageKey.PLANK_HOLDING ->
                if (isPersian) "در حال نگهداشتن پلانک — خط بدن را ثابت نگه دارید."
                else "Holding plank — keep body line steady."
            CoachingMessageKey.PLANK_HIP_SAG ->
                if (isPersian) "لگن کمی پایین افتاده — با انقباض شکم هم‌راستایی را حفظ کنید."
                else "Hips appear low — gently brace core to align shoulders, hips, ankles."
            CoachingMessageKey.PLANK_HIP_HIGH ->
                if (isPersian) "لگن کمی بالا رفته — لگن را در راستای مستقیم قرار دهید."
                else "Hips appear high — lower hips toward a straight line."
            CoachingMessageKey.TRACKING_LOW_CONFIDENCE ->
                if (isPersian) "اطمینان پایین نقاط بدن — نور یا کادر را تنظیم کنید."
                else "Low landmark confidence — adjust lighting or framing."
            CoachingMessageKey.TRACKING_PAUSED ->
                if (isPersian) "شمارش تا زمان دیده شدن واضح نقاط بدن متوقف شد."
                else "Counting paused until required landmarks are clearly visible."
        }
    }
}
