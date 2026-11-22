package com.aifitnesscoach.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        WorkoutSessionEntity::class,
        ExerciseSetEntity::class,
        FormObservationEntity::class,
        ExerciseAssetEntity::class,
        InstalledLocalModelEntity::class,
        AIProviderConfigEntity::class,
        SavedInsightEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun exerciseAssetDao(): ExerciseAssetDao
    abstract fun localModelDao(): LocalModelDao
    abstract fun aiProviderDao(): AIProviderDao
    abstract fun savedInsightDao(): SavedInsightDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Non-destructive migration from schema v1 -> v2 preserving all existing
         * workout_sessions, exercise_sets, and form_observations records.
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `exercise_assets` (
                        `exerciseId` TEXT NOT NULL,
                        `sourceId` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `version` TEXT NOT NULL,
                        `localPrimaryPath` TEXT,
                        `localSecondaryPath` TEXT,
                        `mediaType` TEXT NOT NULL,
                        `checksumSha256` TEXT,
                        `bytesDownloaded` INTEGER NOT NULL,
                        `totalBytes` INTEGER NOT NULL,
                        `attribution` TEXT NOT NULL,
                        `licenseName` TEXT NOT NULL,
                        `errorMessage` TEXT,
                        `updatedAtMs` INTEGER NOT NULL,
                        PRIMARY KEY(`exerciseId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `installed_local_models` (
                        `modelId` TEXT NOT NULL,
                        `localPath` TEXT NOT NULL,
                        `fileSizeBytes` INTEGER NOT NULL,
                        `sha256` TEXT NOT NULL,
                        `checksumVerified` INTEGER NOT NULL,
                        `isVerifiedCompatible` INTEGER NOT NULL,
                        `lastTestLoadSuccess` INTEGER,
                        `lastTestInitMs` INTEGER,
                        `lastTestLatencyMs` INTEGER,
                        `lastTestMemoryMb` INTEGER,
                        `lastTestError` TEXT,
                        `status` TEXT NOT NULL,
                        `downloadedBytes` INTEGER NOT NULL,
                        `totalBytes` INTEGER NOT NULL,
                        `updatedAtMs` INTEGER NOT NULL,
                        PRIMARY KEY(`modelId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ai_provider_configs` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `presetType` TEXT NOT NULL,
                        `baseUrl` TEXT NOT NULL,
                        `endpointPath` TEXT NOT NULL,
                        `modelId` TEXT NOT NULL,
                        `customHeadersJson` TEXT NOT NULL,
                        `timeoutSec` INTEGER NOT NULL,
                        `maxTokens` INTEGER NOT NULL,
                        `temperature` REAL NOT NULL,
                        `allowInsecureLocalHttp` INTEGER NOT NULL,
                        `enabled` INTEGER NOT NULL,
                        `createdAtMs` INTEGER NOT NULL,
                        `updatedAtMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `saved_insights` (
                        `id` TEXT NOT NULL,
                        `timestampMs` INTEGER NOT NULL,
                        `analysisType` TEXT NOT NULL,
                        `providerType` TEXT NOT NULL,
                        `modelIdentifier` TEXT NOT NULL,
                        `relatedWorkoutIdsCsv` TEXT NOT NULL,
                        `sourceRangeLabel` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `summaryTitle` TEXT NOT NULL,
                        `bodyText` TEXT NOT NULL,
                        `issuesJson` TEXT NOT NULL,
                        `errorMessage` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_fitness_coach.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
