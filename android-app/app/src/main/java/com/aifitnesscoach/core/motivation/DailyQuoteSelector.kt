package com.aifitnesscoach.core.motivation

import java.time.LocalDate

data class DailyQuoteSelection(
    val dateIso: String,
    val quote: MotivationalQuote?,
    val updatedRecentIds: List<String>,
    val didChange: Boolean
)

/**
 * Deterministic per-day selector for English motivational quotes:
 * - Same quote remains stable across app restarts for a given local calendar day [today].
 * - Rotates across days, avoiding repeats from [recentIds] until the catalog is exhausted.
 * - Handles empty or single-quote catalogs safely without crashes or placeholder fabrications.
 */
object DailyQuoteSelector {

    fun selectForDate(
        repository: VerifiedQuoteRepository,
        today: LocalDate = LocalDate.now(),
        savedDateIso: String?,
        savedQuoteId: String?,
        recentIds: List<String>
    ): DailyQuoteSelection {
        val todayIso = today.toString()
        val quotes = repository.getAllVerifiedQuotes()
        if (quotes.isEmpty()) {
            return DailyQuoteSelection(
                dateIso = todayIso,
                quote = null,
                updatedRecentIds = emptyList(),
                didChange = false
            )
        }

        // Same day: keep saved verified quote stable across restarts
        if (savedDateIso == todayIso && !savedQuoteId.isNullOrBlank()) {
            val existing = quotes.firstOrNull { it.id == savedQuoteId }
            if (existing != null) {
                return DailyQuoteSelection(
                    dateIso = todayIso,
                    quote = existing,
                    updatedRecentIds = recentIds,
                    didChange = false
                )
            }
        }

        val recentWindow = recentIds.takeLast((quotes.size - 1).coerceAtLeast(0)).toSet()
        val nonImmediateRepeatPool = quotes.filterNot { it.id == savedQuoteId }
        val unvisitedPool = quotes.filterNot { it.id in recentWindow || it.id == savedQuoteId }

        val candidates = when {
            unvisitedPool.isNotEmpty() -> unvisitedPool
            nonImmediateRepeatPool.isNotEmpty() -> nonImmediateRepeatPool
            else -> quotes
        }

        // Deterministic index based on epoch day so rotation is reproducible across runs
        val index = Math.floorMod(today.toEpochDay(), candidates.size.toLong()).toInt()
        val chosen = candidates[index]

        val nextHistory = (recentIds + chosen.id)
            .distinctLast(maxSize = quotes.size.coerceAtLeast(1))

        return DailyQuoteSelection(
            dateIso = todayIso,
            quote = chosen,
            updatedRecentIds = nextHistory,
            didChange = true
        )
    }

    private fun List<String>.distinctLast(maxSize: Int): List<String> {
        val result = ArrayList<String>(size)
        for (item in this) {
            result.remove(item)
            result.add(item)
        }
        return if (result.size > maxSize) result.takeLast(maxSize) else result
    }
}
