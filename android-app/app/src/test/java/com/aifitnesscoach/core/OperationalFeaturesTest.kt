package com.aifitnesscoach.core

import androidx.work.NetworkType
import com.aifitnesscoach.core.ai.external.AIProviderClient
import com.aifitnesscoach.core.ai.external.ProviderPresetType
import com.aifitnesscoach.core.ai.local.LocalModelCatalog
import com.aifitnesscoach.core.ai.schedule.AnalysisScheduler
import com.aifitnesscoach.core.ai.schedule.ScheduledAnalysisWorker
import com.aifitnesscoach.core.database.AIProviderConfigEntity
import com.aifitnesscoach.core.database.AiExecutionMode
import com.aifitnesscoach.core.database.AnalysisFrequency
import com.aifitnesscoach.core.database.AnalysisRangeScope
import com.aifitnesscoach.core.database.AppDatabase
import com.aifitnesscoach.core.database.ExerciseSetEntity
import com.aifitnesscoach.core.database.FormObservationEntity
import com.aifitnesscoach.core.database.UserPreferences
import com.aifitnesscoach.core.database.WorkoutExportManager
import com.aifitnesscoach.core.database.WorkoutSessionEntity
import com.aifitnesscoach.core.database.WorkoutSessionWithDetails
import com.aifitnesscoach.core.exercise.ExerciseCategory
import com.aifitnesscoach.core.exercise.definitions.ExerciseCatalog
import com.aifitnesscoach.core.media.AssetDownloadStatus
import com.aifitnesscoach.core.media.AssetSourceType
import com.aifitnesscoach.core.media.ExerciseAssetCatalog
import com.aifitnesscoach.core.security.InMemorySecretStore
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.time.ZoneId
import java.time.ZonedDateTime

class OperationalFeaturesTest {

    @Test
    fun `exercise asset catalog covers all 39 exercises with strict equipment accuracy and path confinement`() {
        val allIds = ExerciseCatalog.allExercises.map { it.id }
        assertEquals(39, allIds.size)

        val entries = allIds.map { ExerciseAssetCatalog.entryFor(it) }
        val available = entries.filter { it.defaultStatus == AssetDownloadStatus.AVAILABLE }
        val unavailable = entries.filter { it.defaultStatus == AssetDownloadStatus.NOT_AVAILABLE }

        assertEquals(32, available.size)
        assertEquals(7, unavailable.size)

        available.forEach { entry ->
            assertEquals(AssetSourceType.REPDB_FREE_TIER, entry.sourceType)
            assertTrue(entry.primaryUrl?.startsWith("https://") == true)
            assertTrue(entry.attribution.contains("RepDB"))
        }

        // Verify all 7 unavailable entries belong exclusively to Resistance Band Training
        unavailable.forEach { entry ->
            val def = ExerciseCatalog.findById(entry.exerciseId)
            assertNotNull(def)
            assertEquals(ExerciseCategory.RESISTANCE_BAND_TRAINING, def?.category)
            assertNull(entry.primaryUrl)
        }

        // Verify Goblet Squat maps cleanly to db-goblet-squat
        val goblet = ExerciseAssetCatalog.entryFor("goblet_squat")
        assertEquals("db-goblet-squat", goblet.sourceSlug)

        // Verify path traversal rejection
        val tempDir = Files.createTempDirectory("aifc_media_test").toFile()
        try {
            val safe = ExerciseAssetCatalog.resolveConfinedMediaFile(tempDir, "bodyweight_squat_primary.webp")
            assertTrue(safe.canonicalPath.startsWith(tempDir.canonicalPath))

            var threw = false
            try {
                ExerciseAssetCatalog.resolveConfinedMediaFile(tempDir, "../../etc/passwd")
            } catch (_: IllegalArgumentException) {
                threw = true
            }
            assertTrue("Expected traversal path rejection", threw)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `local model catalog validates SHA-256 and confines model paths`() {
        assertEquals(4, LocalModelCatalog.models.size)

        val tempDir = Files.createTempDirectory("aifc_model_test").toFile()
        try {
            val safeFile = LocalModelCatalog.resolveConfinedModelFile(tempDir, "test-model.litertlm")
            safeFile.writeBytes("litertlm-sample-weights".toByteArray())

            val expectedSha = MessageDigest.getInstance("SHA-256")
                .digest("litertlm-sample-weights".toByteArray())
                .joinToString("") { "%02x".format(it) }

            assertTrue(LocalModelCatalog.verifySha256(safeFile, expectedSha))
            assertFalse(LocalModelCatalog.verifySha256(safeFile, "0".repeat(64)))
            assertFalse(LocalModelCatalog.verifySha256(File(tempDir, "missing.litertlm"), expectedSha))

            var threw = false
            try {
                LocalModelCatalog.resolveConfinedModelFile(tempDir, "../escape.litertlm")
            } catch (_: IllegalArgumentException) {
                threw = true
            }
            assertTrue("Expected traversal rejection in LocalModelCatalog", threw)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `external AI URL validation enforces HTTPS on public hosts and explicit opt-in for LAN HTTP`() {
        val publicHttp = AIProviderClient.validateEndpointUrl(
            baseUrl = "http://api.openai.com",
            endpointPath = "/v1/chat/completions",
            allowInsecureLocalHttp = true
        )
        assertFalse(publicHttp.valid)

        val publicHttps = AIProviderClient.validateEndpointUrl(
            baseUrl = "https://api.openai.com",
            endpointPath = "/v1/chat/completions",
            allowInsecureLocalHttp = false
        )
        assertTrue(publicHttps.valid)
        assertEquals("https://api.openai.com/v1/chat/completions", publicHttps.normalizedCompletionUrl)
        assertEquals("https://api.openai.com/v1/models", publicHttps.normalizedModelsUrl)

        val lanHttpBlockedByDefault = AIProviderClient.validateEndpointUrl(
            baseUrl = "http://192.168.1.50:11434",
            endpointPath = "/v1/chat/completions",
            allowInsecureLocalHttp = false
        )
        assertFalse(lanHttpBlockedByDefault.valid)
        assertTrue(lanHttpBlockedByDefault.isLanOrLoopback)

        val lanHttpAllowedWithOptIn = AIProviderClient.validateEndpointUrl(
            baseUrl = "http://192.168.1.50:11434",
            endpointPath = "/v1/chat/completions",
            allowInsecureLocalHttp = true
        )
        assertTrue(lanHttpAllowedWithOptIn.valid)

        val secret = "sk-secret-value-999"
        val redacted = AIProviderClient.redactSecrets("Auth failed using $secret on server", secret)
        assertFalse(redacted.contains(secret))
        assertTrue(redacted.contains("[REDACTED]"))
    }

    @Test
    fun `AIProviderClient tests connection and handles 401 securely against MockWebServer`() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            val baseUrl = server.url("/").toString().trimEnd('/')
            val config = AIProviderConfigEntity(
                id = "test_provider",
                name = "Local Test",
                presetType = ProviderPresetType.OLLAMA_LAN.name,
                baseUrl = baseUrl,
                endpointPath = "/v1/chat/completions",
                modelId = "llama3.2",
                allowInsecureLocalHttp = true
            )
            val client = AIProviderClient()

            // Success case: /v1/models + /v1/chat/completions
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"object":"list","data":[{"id":"llama3.2"}]}""")
            )
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"choices":[{"message":{"content":"OK"}}]}""")
            )

            val secretKey = "sk-live-secret-1234"
            val okResult = client.testConnection(config, secretKey)
            assertTrue(okResult.success)
            assertTrue(okResult.authOk)
            assertTrue(okResult.modelCheckOk)
            assertTrue(okResult.completionOk)
            assertEquals("OK", okResult.sampleResponse)

            val modelsReq = server.takeRequest()
            assertEquals("Bearer $secretKey", modelsReq.getHeader("Authorization"))
            val completionReq = server.takeRequest()
            assertEquals("Bearer $secretKey", completionReq.getHeader("Authorization"))

            // Auth failure case: must fail cleanly without leaking key
            server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"Unauthorized with $secretKey"}"""))
            val failResult = client.testConnection(config, secretKey)
            assertFalse(failResult.success)
            assertFalse(failResult.authOk)
            assertFalse(failResult.errorMessage.orEmpty().contains(secretKey))
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `workout export JSON contains session metrics and strictly excludes secrets`() {
        val secretStore = InMemorySecretStore()
        val sensitiveKey = "sk-do-not-export-me-xyz"
        secretStore.putSecret("provider_key_1", sensitiveKey)
        secretStore.putSecret("hf_token", "hf_do_not_export_token")

        val session = WorkoutSessionEntity(
            id = 42L,
            clientRecordId = "aifc_42",
            titleEn = "Bodyweight Squat",
            titleFa = "اسکوات وزن بدن",
            startedAtEpochMs = 1_700_000_000_000L,
            endedAtEpochMs = 1_700_000_600_000L,
            totalDurationMs = 600_000L
        )
        val details = WorkoutSessionWithDetails(
            session = session,
            sets = listOf(
                ExerciseSetEntity(
                    id = 1L,
                    sessionId = 42L,
                    exerciseId = "bodyweight_squat",
                    categoryName = ExerciseCategory.BODYWEIGHT_TRAINING.name,
                    setNumber = 1,
                    targetReps = 10,
                    completedReps = 10,
                    holdDurationMs = 0L,
                    restDurationSec = 60
                )
            ),
            observations = listOf(
                FormObservationEntity(
                    id = 1L,
                    sessionId = 42L,
                    exerciseId = "bodyweight_squat",
                    severity = "POSITIVE",
                    detailEn = "Consistent depth",
                    detailFa = "عمق مناسب",
                    timestampMs = 1_700_000_300_000L
                )
            )
        )

        val json = WorkoutExportManager.buildExportJson(
            preferences = UserPreferences(),
            sessions = listOf(details),
            exportedAtMs = 1_700_001_000_000L
        )
        assertTrue(json.contains("Bodyweight Squat"))
        assertTrue(json.contains("Consistent depth"))
        assertFalse(json.contains(sensitiveKey))
        assertFalse(json.contains("hf_do_not_export_token"))
    }

    @Test
    fun `scheduler computes constraints and worker watermark scopes sessions accurately`() {
        val now = ZonedDateTime.of(2026, 6, 1, 20, 0, 0, 0, ZoneId.of("UTC"))
        val localDailyPrefs = UserPreferences(
            scheduledAnalysisEnabled = true,
            scheduleHour = 21,
            scheduleMinute = 30,
            scheduleFrequency = AnalysisFrequency.DAILY,
            scheduleProviderMode = AiExecutionMode.LOCAL_LLM,
            scheduleWifiOnly = true,
            scheduleChargingRequired = true
        )
        val localPolicy = AnalysisScheduler.computePolicy(localDailyPrefs, now)
        assertEquals(1L, localPolicy.repeatIntervalDays)
        assertEquals(90L, localPolicy.initialDelayMinutes)
        assertEquals(NetworkType.NOT_REQUIRED, localPolicy.networkType)
        assertTrue(localPolicy.requiresCharging)

        val externalWeeklyPrefs = localDailyPrefs.copy(
            scheduleFrequency = AnalysisFrequency.WEEKLY,
            scheduleProviderMode = AiExecutionMode.EXTERNAL_API,
            scheduleWifiOnly = true
        )
        val externalPolicy = AnalysisScheduler.computePolicy(externalWeeklyPrefs, now)
        assertEquals(7L, externalPolicy.repeatIntervalDays)
        assertEquals(NetworkType.UNMETERED, externalPolicy.networkType)

        // Watermark scoping
        val baseTime = 1_700_000_000_000L
        val s1 = WorkoutSessionWithDetails(
            session = WorkoutSessionEntity(1L, "r1", "Old", "قدیمی", baseTime - 10_000_000L, baseTime - 9_000_000L, 600_000L),
            sets = emptyList(),
            observations = emptyList()
        )
        val s2 = WorkoutSessionWithDetails(
            session = WorkoutSessionEntity(2L, "r2", "New", "جدید", baseTime + 5_000L, baseTime + 605_000L, 600_000L),
            sets = emptyList(),
            observations = emptyList()
        )
        val sinceWatermark = ScheduledAnalysisWorker.selectSessionsForScope(
            sessions = listOf(s1, s2),
            scope = AnalysisRangeScope.SINCE_LAST_ANALYSIS,
            watermarkMs = baseTime,
            nowMs = baseTime + 1_000_000L
        )
        assertEquals(1, sinceWatermark.size)
        assertEquals(2L, sinceWatermark.first().session.id)
    }

    @Test
    fun `database migration v1 to v2 targets exact version bump and secret store clears cleanly`() {
        assertEquals(1, AppDatabase.MIGRATION_1_2.startVersion)
        assertEquals(2, AppDatabase.MIGRATION_1_2.endVersion)

        val store = InMemorySecretStore()
        store.putSecret("k1", "  my-secret  ")
        assertEquals("my-secret", store.getSecret("k1"))
        assertTrue(store.hasSecret("k1"))
        store.clearAllSecrets()
        assertFalse(store.hasSecret("k1"))
        assertNull(store.getSecret("k1"))
    }
}
