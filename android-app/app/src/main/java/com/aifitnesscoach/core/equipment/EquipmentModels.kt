package com.aifitnesscoach.core.equipment

enum class WeightUnit(val symbolEn: String, val symbolFa: String) {
    KG("kg", "کیلوگرم"),
    LB("lb", "پوند")
}

enum class WeightLoadMode {
    PER_DUMBBELL,
    TOTAL_LOAD
}

data class WeightEntry(
    val value: Float,
    val unit: WeightUnit = WeightUnit.KG,
    val loadMode: WeightLoadMode = WeightLoadMode.PER_DUMBBELL
)

enum class ResistanceBandType(val labelEn: String, val labelFa: String) {
    LONG_LOOP("Long Loop Resistance Band", "کش حلقه‌ای بلند"),
    MINI_LOOP("Mini Loop Band", "کش مینی لوپ"),
    TUBE_WITH_HANDLES("Tube Resistance Band with Handles", "کش لوله‌ای دسته‌دار"),
    FLAT_THERAPY("Flat Therapy Band", "کش تخت تمرینی")
}

enum class ResistanceLevel(val labelEn: String, val labelFa: String) {
    EXTRA_LIGHT("Extra Light", "خیلی سبک"),
    LIGHT("Light", "سبک"),
    MEDIUM("Medium", "متوسط"),
    HEAVY("Heavy", "سنگین"),
    EXTRA_HEAVY("Extra Heavy", "خیلی سنگین"),
    CUSTOM("Custom", "سفارشی")
}

/**
 * Equipment metadata entered manually by the user.
 * Resistance ranges and levels are strictly metadata and must NEVER be treated as
 * camera-measured applied force or band tension.
 */
data class ResistanceBandMetadata(
    val bandType: ResistanceBandType = ResistanceBandType.LONG_LOOP,
    val brand: String = "",
    val manufacturerLabel: String = "",
    val resistanceLevel: ResistanceLevel = ResistanceLevel.MEDIUM,
    val bandColor: String = "",
    val bandCount: Int = 1,
    val minManufacturerResistance: Float? = null,
    val maxManufacturerResistance: Float? = null,
    val resistanceUnit: WeightUnit = WeightUnit.KG
)
