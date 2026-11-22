package com.aifitnesscoach.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseAssetDao {
    @Query("SELECT * FROM exercise_assets")
    fun observeAll(): Flow<List<ExerciseAssetEntity>>

    @Query("SELECT * FROM exercise_assets WHERE exerciseId = :exerciseId LIMIT 1")
    fun observeById(exerciseId: String): Flow<ExerciseAssetEntity?>

    @Query("SELECT * FROM exercise_assets WHERE exerciseId = :exerciseId LIMIT 1")
    suspend fun getById(exerciseId: String): ExerciseAssetEntity?

    @Query("SELECT * FROM exercise_assets")
    suspend fun getAll(): List<ExerciseAssetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ExerciseAssetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ExerciseAssetEntity>)

    @Query("DELETE FROM exercise_assets WHERE exerciseId = :exerciseId")
    suspend fun deleteById(exerciseId: String)

    @Query("DELETE FROM exercise_assets")
    suspend fun deleteAll()
}

@Dao
interface LocalModelDao {
    @Query("SELECT * FROM installed_local_models ORDER BY updatedAtMs DESC")
    fun observeAll(): Flow<List<InstalledLocalModelEntity>>

    @Query("SELECT * FROM installed_local_models WHERE modelId = :modelId LIMIT 1")
    suspend fun getById(modelId: String): InstalledLocalModelEntity?

    @Query("SELECT * FROM installed_local_models")
    suspend fun getAll(): List<InstalledLocalModelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: InstalledLocalModelEntity)

    @Query("DELETE FROM installed_local_models WHERE modelId = :modelId")
    suspend fun deleteById(modelId: String)

    @Query("DELETE FROM installed_local_models")
    suspend fun deleteAll()
}

@Dao
interface AIProviderDao {
    @Query("SELECT * FROM ai_provider_configs ORDER BY createdAtMs ASC")
    fun observeAll(): Flow<List<AIProviderConfigEntity>>

    @Query("SELECT * FROM ai_provider_configs WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AIProviderConfigEntity?

    @Query("SELECT * FROM ai_provider_configs")
    suspend fun getAll(): List<AIProviderConfigEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AIProviderConfigEntity)

    @Query("DELETE FROM ai_provider_configs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM ai_provider_configs")
    suspend fun deleteAll()
}

@Dao
interface SavedInsightDao {
    @Query("SELECT * FROM saved_insights ORDER BY timestampMs DESC")
    fun observeAll(): Flow<List<SavedInsightEntity>>

    @Query("SELECT * FROM saved_insights ORDER BY timestampMs DESC LIMIT 1")
    fun observeLatest(): Flow<SavedInsightEntity?>

    @Query("SELECT * FROM saved_insights WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SavedInsightEntity?

    @Query("SELECT * FROM saved_insights ORDER BY timestampMs DESC LIMIT 20")
    suspend fun getRecent(): List<SavedInsightEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SavedInsightEntity)

    @Query("DELETE FROM saved_insights WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM saved_insights")
    suspend fun deleteAll()
}
