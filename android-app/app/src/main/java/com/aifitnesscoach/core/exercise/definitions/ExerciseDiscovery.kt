package com.aifitnesscoach.core.exercise.definitions

import com.aifitnesscoach.core.exercise.EquipmentCategoryType
import com.aifitnesscoach.core.exercise.ExerciseDefinition
import java.text.Normalizer
import java.util.Locale

private val separators = Regex("[^\\p{L}\\p{N}]+")
private val marks = Regex("\\p{M}+")

internal fun searchKey(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFKD)
    .replace(marks, "")
    .lowercase(Locale.ROOT)
    .replace('ي', 'ی').replace('ك', 'ک')
    .replace(separators, " ").trim()

internal fun equipmentKey(value: String): String = when (val key = searchKey(value)) {
    "dumbbells" -> "dumbbell"
    "band", "bands", "resistance bands" -> "resistance band"
    "bodyweight", "none", "no equipment" -> "body weight"
    "cable" -> "cable machine"
    else -> key
}

enum class MuscleRegion(val en: String, val fa: String) {
    CORE("Core", "میان‌تنه"),
    CHEST("Chest", "سینه"),
    BACK("Back", "پشت"),
    SHOULDERS("Shoulders", "سرشانه"),
    ARMS("Arms", "بازو و ساعد"),
    HIPS("Hips & glutes", "لگن و باسن"),
    THIGHS("Thighs", "ران"),
    LOWER_LEGS("Calves & feet", "ساق و کف پا"),
    NECK("Neck", "گردن"),
    FULL_BODY("Full body", "کل بدن"),
    CARDIO("Cardio & breathing", "هوازی و تنفس");

    fun label(isPersian: Boolean) = if (isPersian) fa else en
}

private val regionTerms = mapOf(
    MuscleRegion.CORE to listOf("core", "waist", "abdominals", "abs", "abdominis", "oblique"),
    MuscleRegion.CHEST to listOf("chest", "pectoral", "serratus anterior"),
    MuscleRegion.BACK to listOf("back", "lats", "latissimus", "rhomboid", "traps", "trapezius", "spine", "erector"),
    MuscleRegion.SHOULDERS to listOf("shoulder", "delts", "deltoid", "rotator cuff"),
    MuscleRegion.ARMS to listOf("arm", "brachialis", "biceps", "triceps", "wrist"),
    MuscleRegion.HIPS to listOf("hip", "glute", "abductor"),
    MuscleRegion.THIGHS to listOf("thigh", "upper leg", "quads", "quadriceps", "hamstring", "adductor", "legs"),
    MuscleRegion.LOWER_LEGS to listOf("calf", "calves", "gastrocnemius", "soleus", "ankle", "feet", "lower leg"),
    MuscleRegion.NECK to listOf("neck", "levator scapulae", "head"),
    MuscleRegion.FULL_BODY to listOf("full body"),
    MuscleRegion.CARDIO to listOf("cardio", "breathing", "diaphragm")
)

fun exerciseRegions(exercise: ExerciseDefinition, upstream: List<String> = emptyList()): Set<MuscleRegion> {
    val terms = (exercise.targetMusclesEn + upstream).map(::searchKey)
    return regionTerms.filterValues { aliases -> terms.any { term -> aliases.any(term::contains) } }.keys
}

data class DiscoverableExercise(
    val exercise: ExerciseDefinition,
    val source: ExerciseSource,
    val equipment: List<String>,
    val muscleRegions: Set<MuscleRegion>
) {
    private val searchNames = listOf(exercise.nameEn, exercise.nameFa).map(::searchKey).distinct()
    private val searchTerms = (searchNames + equipment + exercise.targetMusclesEn + exercise.targetMusclesFa +
        muscleRegions.flatMap { listOf(it.en, it.fa) } + equipment.map { equipmentLabel(it, true) })
        .joinToString(" ", transform = ::searchKey)
    private val nameWords = searchNames.flatMap { it.split(' ') }.toSet()

    fun matches(query: String, equipmentKey: String?, region: MuscleRegion?): Boolean {
        if (equipmentKey != null && equipmentKey !in equipment) return false
        if (region != null && region !in muscleRegions) return false
        if (query.isEmpty()) return true
        if (query in searchTerms) return true
        return query.split(' ').all { term ->
            term in searchTerms || (term.length >= 4 && nameWords.any { word -> oneEditApart(term, word) })
        }
    }
}

fun discoveryEntry(exercise: ExerciseDefinition): DiscoverableExercise {
    val external = ExternalExerciseSources.entry(exercise.id)
    val equipment = external?.equipment ?: listOf(when (exercise.equipmentType) {
        EquipmentCategoryType.DUMBBELL -> "dumbbell"
        EquipmentCategoryType.RESISTANCE_BAND -> "resistance band"
        EquipmentCategoryType.MAT_OR_FLOOR -> "yoga mat"
        EquipmentCategoryType.WALL -> "wall"
        EquipmentCategoryType.BODYWEIGHT -> "body weight"
        EquipmentCategoryType.OTHER -> "other"
    })
    return DiscoverableExercise(exercise, external?.source ?: ExerciseSource.BUILT_IN, equipment,
        exerciseRegions(exercise, external?.regions.orEmpty()))
}

private fun oneEditApart(a: String, b: String): Boolean {
    if (kotlin.math.abs(a.length - b.length) > 1) return false
    val first = a.indices.firstOrNull { it >= b.length || a[it] != b[it] } ?: a.length
    if (a.length == b.length) {
        if (first >= a.lastIndex) return true
        return a.regionMatches(first + 1, b, first + 1, a.length - first - 1) ||
            (a[first] == b[first + 1] && a[first + 1] == b[first] &&
                a.regionMatches(first + 2, b, first + 2, a.length - first - 2))
    }
    val (shorter, longer) = if (a.length < b.length) a to b else b to a
    return shorter.regionMatches(first, longer, first + 1, shorter.length - first)
}

fun equipmentLabel(key: String, isPersian: Boolean): String {
    if (isPersian) equipmentFa[key]?.let { return it }
    return key.replaceFirstChar { it.titlecase() }
}

private val equipmentFa = mapOf(
    "body weight" to "وزن بدن", "dumbbell" to "دمبل", "resistance band" to "کش مقاومتی",
    "yoga mat" to "مت ورزشی", "barbell" to "هالتر", "cable machine" to "سیم‌کش",
    "leverage machine" to "دستگاه اهرمی", "smith machine" to "دستگاه اسمیت",
    "kettlebell" to "کتل‌بل", "weighted" to "وزنهٔ اضافه", "stability ball" to "توپ تعادل",
    "ez barbell" to "هالتر خم", "assisted" to "دستگاه کمکی", "sled machine" to "دستگاه سورتمه",
    "medicine ball" to "مدیسن‌بال", "rope" to "طناب", "roller" to "غلتک",
    "bosu ball" to "توپ بوسو", "olympic barbell" to "هالتر المپیکی", "wheel roller" to "چرخ شکم",
    "upper body ergometer" to "ارگومتر بالاتنه", "skierg machine" to "دستگاه اسکی",
    "hammer" to "چکش", "stationary bike" to "دوچرخه ثابت", "tire" to "تایر",
    "trap bar" to "هالتر شش‌ضلعی", "elliptical machine" to "دستگاه الپتیکال",
    "elliptical" to "دستگاه الپتیکال", "stepmill machine" to "دستگاه پله",
    "chair" to "صندلی", "wall" to "دیوار", "bench" to "نیمکت", "railing" to "نرده",
    "step" to "استپ", "towel" to "حوله", "table" to "میز", "stairs" to "پله",
    "chest press machine" to "دستگاه پرس سینه", "rowing machine" to "دستگاه روئینگ",
    "recumbent bike" to "دوچرخه ثابت پشتی‌دار", "bicycle" to "دوچرخه", "helmet" to "کلاه ایمنی",
    "desk" to "میز کار", "walking poles" to "عصای پیاده‌روی", "ankle weights" to "وزنه مچ پا",
    "leg press machine" to "دستگاه پرس پا", "swimming pool" to "استخر", "jump rope" to "طناب ورزشی",
    "foam roller" to "فوم رولر", "doorway" to "چارچوب در", "cones" to "مخروط تمرینی",
    "weight plates" to "صفحه وزنه", "soft ball" to "توپ نرم", "parallel support" to "تکیه‌گاه موازی",
    "pull up bar" to "میله بارفیکس", "other" to "سایر تجهیزات"
)
