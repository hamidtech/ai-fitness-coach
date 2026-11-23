package com.aifitnesscoach.feature.plan

import com.aifitnesscoach.core.equipment.ResistanceBandType
import com.aifitnesscoach.core.equipment.ResistanceLevel
import com.aifitnesscoach.core.equipment.WeightEntry
import com.aifitnesscoach.core.equipment.WeightLoadMode
import com.aifitnesscoach.core.equipment.WeightUnit
import com.aifitnesscoach.core.exercise.definitions.ExerciseCatalog
import com.aifitnesscoach.feature.workout.PlannedWorkoutItem
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser

internal object WeeklyPlan {
    fun encode(items: List<PlannedWorkoutItem>): String = JsonArray().apply {
        items.forEach { item ->
            add(JsonObject().apply {
                addProperty("exercise", item.exercise.id)
                addProperty("sets", item.targetSets)
                addProperty("reps", item.targetReps)
                addProperty("hold", item.targetHoldSec)
                addProperty("rest", item.restDurationSec)
                item.weightEntry?.let { weight ->
                    addProperty("weight", weight.value)
                    addProperty("unit", weight.unit.name)
                    addProperty("load", weight.loadMode.name)
                }
                item.bandMetadata?.let { band ->
                    addProperty("band", band.bandType.name)
                    addProperty("level", band.resistanceLevel.name)
                    addProperty("brand", band.brand)
                    addProperty("color", band.bandColor)
                }
            })
        }
    }.toString()

    fun decode(encoded: String?): List<PlannedWorkoutItem> {
        if (encoded.isNullOrEmpty()) return emptyList()
        val entries = runCatching { JsonParser.parseString(encoded).asJsonArray }.getOrNull() ?: return emptyList()
        return entries.mapNotNull { raw ->
            runCatching { decodeItem(raw.asJsonObject) }.getOrNull()
        }
    }

    private fun decodeItem(row: JsonObject): PlannedWorkoutItem? {
        val exercise = ExerciseCatalog.findById(row.get("exercise").asString) ?: return null
        val initial = PlannedWorkoutItem(exercise = exercise)
        val savedLoad = row.get("weight")?.asFloat?.takeIf { it.isFinite() && it >= 0 }
        return initial.copy(
            targetSets = row.get("sets").asInt.coerceIn(1, 20),
            targetReps = row.get("reps").asInt.coerceIn(1, 100),
            targetHoldSec = row.get("hold").asInt.coerceIn(5, 600),
            restDurationSec = row.get("rest").asInt.coerceIn(0, 600),
            weightEntry = savedLoad?.let {
                WeightEntry(it, WeightUnit.valueOf(row.get("unit").asString),
                    WeightLoadMode.valueOf(row.get("load").asString))
            } ?: initial.weightEntry,
            bandMetadata = initial.bandMetadata?.let { band ->
                band.copy(
                    bandType = row.get("band")?.asString?.let(ResistanceBandType::valueOf) ?: band.bandType,
                    resistanceLevel = row.get("level")?.asString?.let(ResistanceLevel::valueOf) ?: band.resistanceLevel,
                    brand = row.get("brand")?.asString.orEmpty(),
                    bandColor = row.get("color")?.asString.orEmpty()
                )
            }
        )
    }
}
