package com.aifitnesscoach.core.ai.analysis

import com.aifitnesscoach.core.database.SavedInsightDao
import com.aifitnesscoach.core.database.SavedInsightEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow

class SavedInsightRepository(private val dao: SavedInsightDao) {

    private val gson = Gson()

    fun observeAllInsights(): Flow<List<SavedInsightEntity>> = dao.observeAll()

    fun observeLatestInsight(): Flow<SavedInsightEntity?> = dao.observeLatest()

    suspend fun getById(id: String): SavedInsightEntity? = dao.getById(id)

    suspend fun save(entity: SavedInsightEntity) {
        dao.upsert(entity)
    }

    suspend fun deleteById(id: String) {
        dao.deleteById(id)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }

    fun encodeIssues(issues: List<InsightIssueItem>): String = gson.toJson(issues)

    fun decodeIssues(json: String): List<InsightIssueItem> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val type = object : TypeToken<List<InsightIssueItem>>() {}.type
            gson.fromJson<List<InsightIssueItem>>(json, type).orEmpty()
        }.getOrDefault(emptyList())
    }
}
