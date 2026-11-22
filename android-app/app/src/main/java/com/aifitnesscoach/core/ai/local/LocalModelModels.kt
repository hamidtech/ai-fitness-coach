package com.aifitnesscoach.core.ai.local

import com.aifitnesscoach.core.database.InstalledLocalModelEntity

enum class LocalModelDownloadStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    PAUSED,
    VERIFYING,
    DOWNLOADED,
    TEST_PASSED,
    TEST_FAILED,
    FAILED
}

data class LocalModelSpec(
    val id: String,
    val displayName: String,
    val familyName: String,
    val quantizationLabel: String,
    val backendHint: String,
    val fileName: String,
    val downloadUrl: String,
    val expectedSizeBytes: Long,
    val sha256: String,
    val recommendedRamMb: Int,
    val requiresHfToken: Boolean,
    val licenseLabel: String,
    val licenseUrl: String,
    val summaryEn: String,
    val summaryFa: String
) {
    val requiredFreeStorageBytes: Long get() = (expectedSizeBytes * 1.15).toLong()
}

data class LocalModelTestReport(
    val loadSuccess: Boolean,
    val initTimeMs: Long,
    val latencyMs: Long,
    val memoryDeltaMb: Long,
    val backendUsed: String,
    val sampleOutput: String,
    val errorMessage: String? = null
)

data class LocalInferenceCompletion(
    val text: String,
    val initTimeMs: Long,
    val latencyMs: Long,
    val backendUsed: String
)

data class DeviceResourceSummary(
    val availableStorageBytes: Long,
    val totalRamMb: Int,
    val availableRamMb: Int,
    val supportedAbis: List<String>
)

data class LocalModelUiState(
    val spec: LocalModelSpec,
    val entity: InstalledLocalModelEntity?,
    val status: LocalModelDownloadStatus,
    val isActiveSelection: Boolean,
    val hasEnoughStorage: Boolean,
    val hasRecommendedRam: Boolean
)
