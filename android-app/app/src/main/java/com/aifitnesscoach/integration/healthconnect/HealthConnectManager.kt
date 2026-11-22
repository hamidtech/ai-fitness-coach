package com.aifitnesscoach.integration.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.aifitnesscoach.core.database.ExerciseSetEntity
import com.aifitnesscoach.core.database.WorkoutSessionEntity
import com.aifitnesscoach.core.exercise.ExerciseCategory
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

enum class HealthConnectAvailability {
    AVAILABLE,
    NOT_INSTALLED,
    NOT_SUPPORTED
}

data class HealthDashboardMetrics(
    val availability: HealthConnectAvailability = HealthConnectAvailability.NOT_SUPPORTED,
    val grantedPermissions: Set<String> = emptySet(),
    val todaySteps: Long? = null,
    val latestSleepMinutes: Long? = null,
    val latestHeartRateAvgBpm: Long? = null,
    val recentImportedSessionTitles: List<String> = emptyList(),
    val lastError: String? = null
)

/**
 * Optional AndroidX Health Connect integration.
 * Uses official Jetpack APIs with stable clientRecordIds for duplicate protection.
 * All workout and AI features remain completely functional if Health Connect is unavailable
 * or restricted under Google Play Personal Apps rules.
 */
class HealthConnectManager(private val context: Context) {

    companion object {
        val writeExercisePermission: String = HealthPermission.getWritePermission(ExerciseSessionRecord::class)
        val readExercisePermission: String = HealthPermission.getReadPermission(ExerciseSessionRecord::class)
        val readStepsPermission: String = HealthPermission.getReadPermission(StepsRecord::class)
        val readSleepPermission: String = HealthPermission.getReadPermission(SleepSessionRecord::class)
        val readHeartRatePermission: String = HealthPermission.getReadPermission(HeartRateRecord::class)
        const val readBackgroundPermission: String = "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"

        fun permissionsForFeatures(
            writeWorkouts: Boolean,
            readWorkouts: Boolean,
            readSteps: Boolean,
            readSleep: Boolean,
            readHeartRate: Boolean
        ): Set<String> = buildSet {
            if (writeWorkouts) add(writeExercisePermission)
            if (readWorkouts) add(readExercisePermission)
            if (readSteps) add(readStepsPermission)
            if (readSleep) add(readSleepPermission)
            if (readHeartRate) add(readHeartRatePermission)
        }

        /**
         * Maps local exercise categories to official Health Connect ExerciseSessionRecord constants.
         * Resistance band & weighted sessions map to EXERCISE_TYPE_STRENGTH_TRAINING;
         * bodyweight & core map to EXERCISE_TYPE_CALISTHENICS; mixed maps to STRENGTH_TRAINING.
         */
        fun mapExerciseType(sets: List<ExerciseSetEntity>): Int {
            val categories = sets.map { it.categoryName }.toSet()
            return when {
                categories.contains(ExerciseCategory.WEIGHTED_TRAINING.name) ||
                    categories.contains(ExerciseCategory.RESISTANCE_BAND_TRAINING.name) ->
                    ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
                categories.contains(ExerciseCategory.BODYWEIGHT_TRAINING.name) ||
                    categories.contains(ExerciseCategory.CORE_TRAINING.name) ->
                    ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS
                else -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            }
        }
    }

    fun checkAvailability(): HealthConnectAvailability {
        return when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.NOT_INSTALLED
            else -> HealthConnectAvailability.NOT_SUPPORTED
        }
    }

    fun getClientOrNull(): HealthConnectClient? {
        return if (checkAvailability() == HealthConnectAvailability.AVAILABLE) {
            runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()
        } else {
            null
        }
    }

    suspend fun getGrantedPermissions(): Set<String> {
        val client = getClientOrNull() ?: return emptySet()
        return runCatching {
            client.permissionController.getGrantedPermissions()
        }.getOrDefault(emptySet())
    }

    suspend fun canReadHealthDataInBackground(): Boolean {
        val client = getClientOrNull() ?: return false
        val featureSupported = runCatching {
            client.features.getFeatureStatus(
                androidx.health.connect.client.HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND
            ) == androidx.health.connect.client.HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
        }.getOrDefault(false)
        if (!featureSupported) return false
        return getGrantedPermissions().contains(readBackgroundPermission)
    }

    fun mapExerciseType(sets: List<ExerciseSetEntity>): Int = Companion.mapExerciseType(sets)

    suspend fun writeWorkoutSession(
        session: WorkoutSessionEntity,
        sets: List<ExerciseSetEntity>
    ): Result<String> {
        val client = getClientOrNull()
            ?: return Result.failure(IllegalStateException("Health Connect unavailable"))

        val granted = getGrantedPermissions()
        if (!granted.contains(writeExercisePermission)) {
            return Result.failure(SecurityException("WRITE_EXERCISE permission not granted"))
        }

        val startInstant = Instant.ofEpochMilli(session.startedAtEpochMs)
        val endInstant = Instant.ofEpochMilli(
            maxOf(session.endedAtEpochMs, session.startedAtEpochMs + 1000L)
        )
        val zoneOffset = ZoneId.systemDefault().rules.getOffset(startInstant)

        val record = ExerciseSessionRecord(
            startTime = startInstant,
            startZoneOffset = zoneOffset,
            endTime = endInstant,
            endZoneOffset = zoneOffset,
            exerciseType = mapExerciseType(sets),
            title = session.titleEn,
            notes = session.notes.ifBlank { null },
            metadata = Metadata.activelyRecorded(
                device = Device(type = Device.TYPE_PHONE),
                clientRecordId = session.clientRecordId,
                clientRecordVersion = 1L
            )
        )

        return runCatching {
            val response = client.insertRecords(listOf(record))
            response.recordIdsList.firstOrNull() ?: session.clientRecordId
        }
    }

    suspend fun readDashboardMetrics(
        readWorkouts: Boolean,
        readSteps: Boolean,
        readSleep: Boolean,
        readHeartRate: Boolean
    ): HealthDashboardMetrics {
        val availability = checkAvailability()
        if (availability != HealthConnectAvailability.AVAILABLE) {
            return HealthDashboardMetrics(availability = availability)
        }
        val client = getClientOrNull() ?: return HealthDashboardMetrics(availability = availability)
        val granted = getGrantedPermissions()
        val now = Instant.now()
        val startOfDay = ZonedDateTime.now().toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val last24h = now.minus(Duration.ofHours(24))
        val last7d = now.minus(Duration.ofDays(7))

        var steps: Long? = null
        var sleepMinutes: Long? = null
        var avgHr: Long? = null
        val importedTitles = mutableListOf<String>()
        var lastError: String? = null

        if (readSteps && granted.contains(readStepsPermission)) {
            runCatching {
                val agg = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                    )
                )
                steps = agg[StepsRecord.COUNT_TOTAL]
            }.onFailure { lastError = it.localizedMessage }
        }

        if (readSleep && granted.contains(readSleepPermission)) {
            runCatching {
                val res = client.readRecords(
                    ReadRecordsRequest(
                        recordType = SleepSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(last24h, now)
                    )
                )
                val totalMin = res.records.sumOf {
                    Duration.between(it.startTime, it.endTime).toMinutes()
                }
                if (res.records.isNotEmpty()) sleepMinutes = totalMin
            }.onFailure { lastError = it.localizedMessage }
        }

        if (readHeartRate && granted.contains(readHeartRatePermission)) {
            runCatching {
                val res = client.readRecords(
                    ReadRecordsRequest(
                        recordType = HeartRateRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(last24h, now)
                    )
                )
                val samples = res.records.flatMap { it.samples }.map { it.beatsPerMinute }
                if (samples.isNotEmpty()) {
                    avgHr = samples.average().toLong()
                }
            }.onFailure { lastError = it.localizedMessage }
        }

        if (readWorkouts && granted.contains(readExercisePermission)) {
            runCatching {
                val res = client.readRecords(
                    ReadRecordsRequest(
                        recordType = ExerciseSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(last7d, now)
                    )
                )
                res.records.takeLast(5).forEach { rec ->
                    importedTitles.add(rec.title ?: "Exercise Session")
                }
            }.onFailure { lastError = it.localizedMessage }
        }

        return HealthDashboardMetrics(
            availability = availability,
            grantedPermissions = granted,
            todaySteps = steps,
            latestSleepMinutes = sleepMinutes,
            latestHeartRateAvgBpm = avgHr,
            recentImportedSessionTitles = importedTitles,
            lastError = lastError
        )
    }
}
