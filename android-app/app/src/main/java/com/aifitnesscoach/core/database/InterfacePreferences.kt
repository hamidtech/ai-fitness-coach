package com.aifitnesscoach.core.database

enum class InterfaceMode { SIMPLE, ADVANCED }

enum class AppFeature { PLAN, PROGRESS, AI, HEALTH, MEDIA }

data class InterfacePreferences(
    val mode: InterfaceMode = InterfaceMode.SIMPLE,
    val overrides: Set<String> = emptySet()
) {
    fun shows(feature: AppFeature): Boolean {
        val prefix = "${mode.name}:${feature.name}="
        return overrides.firstOrNull { it.startsWith(prefix) }?.substringAfter('=')?.toBooleanStrictOrNull()
            ?: (mode == InterfaceMode.ADVANCED)
    }

    fun withFeature(feature: AppFeature, enabled: Boolean): Set<String> {
        val prefix = "${mode.name}:${feature.name}="
        return overrides.filterNot { it.startsWith(prefix) }.toSet() + "$prefix$enabled"
    }
}

internal fun nextRotationSlot(savedSlots: Set<Int>, after: Int): Int? =
    savedSlots.filter { it in 1..7 }.sorted().let { slots ->
        slots.firstOrNull { it > after } ?: slots.firstOrNull()
    }
