package com.aifitnesscoach.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import com.aifitnesscoach.feature.widget.WidgetActivity

@Dao
interface WorkoutDao {

    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY startedAtEpochMs DESC")
    fun observeAllSessionsWithDetails(): Flow<List<WorkoutSessionWithDetails>>

    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY startedAtEpochMs DESC LIMIT 1")
    fun observeLatestSession(): Flow<WorkoutSessionWithDetails?>

    @Query("SELECT DISTINCT exerciseId FROM exercise_sets ORDER BY id DESC LIMIT 6")
    fun observeRecentExerciseIds(): Flow<List<String>>

    @Query("SELECT * FROM workout_sessions WHERE syncedToHealthConnect = 0")
    suspend fun getUnsyncedSessions(): List<WorkoutSessionEntity>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE syncedToHealthConnect = 0")
    suspend fun getUnsyncedSessionsWithDetails(): List<WorkoutSessionWithDetails>

    @Query("SELECT COUNT(*) FROM workout_sessions WHERE syncedToHealthConnect = 1")
    fun observeSyncedCount(): Flow<Int>

    @Query("""
        SELECT COUNT(*) AS workouts, COALESCE(SUM(totalDurationMs), 0) AS durationMs
        FROM workout_sessions AS s
        WHERE endedAtEpochMs >= :startMs AND endedAtEpochMs < :endMs
        AND EXISTS (SELECT 1 FROM exercise_sets WHERE sessionId = s.id)
        """)
    suspend fun getWidgetActivity(startMs: Long, endMs: Long): WidgetActivity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<ExerciseSetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObservations(observations: List<FormObservationEntity>)

    @Transaction
    suspend fun saveFullWorkout(
        session: WorkoutSessionEntity,
        sets: List<ExerciseSetEntity>,
        observations: List<FormObservationEntity>
    ): Long {
        val sessionId = insertSession(session)
        if (sets.isNotEmpty()) {
            insertSets(sets.map { it.copy(sessionId = sessionId) })
        }
        if (observations.isNotEmpty()) {
            insertObservations(observations.map { it.copy(sessionId = sessionId) })
        }
        return sessionId
    }

    @Query("UPDATE workout_sessions SET syncedToHealthConnect = 1, healthConnectRecordId = :recordId WHERE id = :sessionId")
    suspend fun markSessionSynced(sessionId: Long, recordId: String)

    @Query("DELETE FROM workout_sessions")
    suspend fun deleteAllWorkouts()
}
