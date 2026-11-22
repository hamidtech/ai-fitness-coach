package com.aifitnesscoach.core.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val clientRecordId: String,
    val titleEn: String,
    val titleFa: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long,
    val totalDurationMs: Long,
    val notes: String = "",
    val healthConnectRecordId: String? = null,
    val syncedToHealthConnect: Boolean = false
)

@Entity(
    tableName = "exercise_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("exerciseId")]
)
data class ExerciseSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: Long,
    val exerciseId: String,
    val categoryName: String,
    val setNumber: Int,
    val targetReps: Int,
    val completedReps: Int,
    val holdDurationMs: Long,
    val restDurationSec: Int,
    // Weight metadata (manually entered)
    val weightValue: Float? = null,
    val weightUnit: String? = null,
    val weightLoadMode: String? = null,
    // Resistance band metadata (manually entered, never inferred from camera/color)
    val bandType: String? = null,
    val bandBrand: String? = null,
    val bandManufacturerLabel: String? = null,
    val bandResistanceLevel: String? = null,
    val bandColor: String? = null,
    val bandCount: Int? = null,
    val bandRangeMin: Float? = null,
    val bandRangeMax: Float? = null,
    val bandRangeUnit: String? = null
)

@Entity(
    tableName = "form_observations",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class FormObservationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: Long,
    val exerciseId: String,
    val severity: String,
    val detailEn: String,
    val detailFa: String,
    val timestampMs: Long
)

data class WorkoutSessionWithDetails(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val sets: List<ExerciseSetEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val observations: List<FormObservationEntity>
)
