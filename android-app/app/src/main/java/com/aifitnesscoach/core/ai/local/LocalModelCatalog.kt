package com.aifitnesscoach.core.ai.local

import java.io.File
import java.security.MessageDigest

/**
 * Curated official Google LiteRT-LM (.litertlm) model catalog from Hugging Face `litert-community`.
 * Checksums and sizes are pinned from the upstream LFS manifests.
 * No Hugging Face tokens or secrets are embedded; gated models accept a user-provided token stored
 * locally in Android Keystore via SecureCredentialStore.
 */
object LocalModelCatalog {

    val models: List<LocalModelSpec> = listOf(
        LocalModelSpec(
            id = "gemma-3-270m-it-q8",
            displayName = "Gemma 3 270M IT (Q8)",
            familyName = "Gemma 3",
            quantizationLabel = "Q8 · CPU",
            backendHint = "CPU",
            fileName = "gemma3-270m-it-q8.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/gemma-3-270m-it/resolve/main/gemma3-270m-it-q8.litertlm",
            expectedSizeBytes = 304_005_120L,
            sha256 = "757e9119fa5bd667a9774fb470ac4afcd3190a21c677f8e69a5d6bc908abdd63",
            recommendedRamMb = 3072,
            requiresHfToken = true,
            licenseLabel = "Gemma Terms of Use (gated=auto)",
            licenseUrl = "https://huggingface.co/litert-community/gemma-3-270m-it",
            summaryEn = "Compact ~290 MB on-device model suitable for mid-range devices.",
            summaryFa = "مدل سبک حدود ۲۹۰ مگابایت مناسب برای گوشی‌های میان‌رده."
        ),
        LocalModelSpec(
            id = "gemma-3-1b-it-int4",
            displayName = "Gemma 3 1B IT (INT4)",
            familyName = "Gemma 3",
            quantizationLabel = "INT4 · CPU/GPU",
            backendHint = "CPU / GPU",
            fileName = "gemma3-1b-it-int4.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.litertlm",
            expectedSizeBytes = 584_417_280L,
            sha256 = "1325ae366d31950f137c9c357b9fa89448b176d76998180c08ceaca78bba98be",
            recommendedRamMb = 4096,
            requiresHfToken = true,
            licenseLabel = "Gemma Terms of Use (gated=auto)",
            licenseUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT",
            summaryEn = "Balanced ~557 MB instruction model for workout summary and Q&A.",
            summaryFa = "مدل متعادل حدود ۵۵۷ مگابایت برای تحلیل تمرین و پرسش‌وپاسخ آفلاین."
        ),
        LocalModelSpec(
            id = "gemma-4-e2b-it-cpu",
            displayName = "Gemma 4 E2B IT (CPU)",
            familyName = "Gemma 4",
            quantizationLabel = "LiteRT-LM · CPU",
            backendHint = "CPU",
            fileName = "gemma-4-E2B-it.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
            expectedSizeBytes = 2_588_147_712L,
            sha256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c",
            recommendedRamMb = 6144,
            requiresHfToken = false,
            licenseLabel = "Apache 2.0 (Open Download)",
            licenseUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm",
            summaryEn = "Open-download Gemma 4 E2B (~2.41 GB) for higher-RAM devices.",
            summaryFa = "مدل قدرتمند Gemma 4 بدون نیاز به توکن (~۲.۴ گیگابایت) برای دستگاه‌های با رم بالا."
        ),
        LocalModelSpec(
            id = "gemma-4-e2b-it-gpu",
            displayName = "Gemma 4 E2B IT (GPU)",
            familyName = "Gemma 4",
            quantizationLabel = "LiteRT-LM · OpenCL GPU",
            backendHint = "GPU",
            fileName = "gemma-4-E2B-it-gpu.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it-gpu.litertlm",
            expectedSizeBytes = 2_340_870_772L,
            sha256 = "d3cfec6ae2e027a994e51366d44dbf522ec7531a4a35fbbfcd39cc9b927afc1",
            recommendedRamMb = 6144,
            requiresHfToken = false,
            licenseLabel = "Apache 2.0 (Open Download)",
            licenseUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm",
            summaryEn = "GPU-optimized Gemma 4 E2B (~2.18 GB). Falls back cleanly if GPU unsupported.",
            summaryFa = "نسخه بهینه‌شده GPU از Gemma 4 (~۲.۱۸ گیگابایت) با بازگشت خودکار در صورت عدم پشتیبانی."
        )
    )

    fun findById(id: String): LocalModelSpec? = models.firstOrNull { it.id == id }

    fun computeSha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered(64 * 1024).use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun verifySha256(file: File, expectedSha256: String): Boolean {
        if (!file.exists() || expectedSha256.isBlank()) return false
        return computeSha256Hex(file).equals(expectedSha256.trim(), ignoreCase = true)
    }

    fun resolveConfinedModelFile(modelsDir: File, fileName: String): File {
        val sanitized = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        require(sanitized.isNotBlank() && !sanitized.contains("..")) {
            "Invalid model filename"
        }
        val canonicalBase = modelsDir.canonicalFile
        val target = File(canonicalBase, sanitized).canonicalFile
        require(target.path.startsWith(canonicalBase.path + File.separator)) {
            "Model file path escapes local model directory"
        }
        return target
    }
}
