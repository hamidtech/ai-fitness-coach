package com.aifitnesscoach.core.ai.local

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.LogSeverity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

interface LocalLlmRuntime {
    val isPoseTrackingActive: StateFlow<Boolean>
    fun setPoseTrackingActive(active: Boolean)

    suspend fun testModel(modelPath: String, preferGpu: Boolean): LocalModelTestReport

    suspend fun runCompletion(
        modelPath: String,
        systemPrompt: String,
        userPrompt: String,
        preferGpu: Boolean
    ): Result<LocalInferenceCompletion>
}

/**
 * Official Google LiteRT-LM inference wrapper.
 * Enforces single-execution mutex and refuses execution while active camera pose tracking
 * is occupying CPU/GPU memory budgets.
 */
class LiteRtLmInferenceEngine(private val context: Context) : LocalLlmRuntime {

    private val mutex = Mutex()
    private val _isPoseTrackingActive = MutableStateFlow(false)
    override val isPoseTrackingActive: StateFlow<Boolean> = _isPoseTrackingActive.asStateFlow()

    private val cacheDir: File = File(context.cacheDir, "litertlm_cache").apply { mkdirs() }

    override fun setPoseTrackingActive(active: Boolean) {
        _isPoseTrackingActive.value = active
    }

    override suspend fun testModel(modelPath: String, preferGpu: Boolean): LocalModelTestReport {
        val result = runCompletion(
            modelPath = modelPath,
            systemPrompt = "You are a concise fitness summary assistant.",
            userPrompt = "Reply in one short sentence confirming you are ready to review workout logs.",
            preferGpu = preferGpu
        )
        return result.fold(
            onSuccess = { completion ->
                LocalModelTestReport(
                    loadSuccess = true,
                    initTimeMs = completion.initTimeMs,
                    latencyMs = completion.latencyMs,
                    memoryDeltaMb = currentUsedHeapMb(),
                    backendUsed = completion.backendUsed,
                    sampleOutput = completion.text.take(240),
                    errorMessage = null
                )
            },
            onFailure = { err ->
                LocalModelTestReport(
                    loadSuccess = false,
                    initTimeMs = 0L,
                    latencyMs = 0L,
                    memoryDeltaMb = currentUsedHeapMb(),
                    backendUsed = if (preferGpu) "GPU->CPU" else "CPU",
                    sampleOutput = "",
                    errorMessage = err.localizedMessage ?: err::class.java.simpleName
                )
            }
        )
    }

    override suspend fun runCompletion(
        modelPath: String,
        systemPrompt: String,
        userPrompt: String,
        preferGpu: Boolean
    ): Result<LocalInferenceCompletion> = withContext(Dispatchers.IO) {
        if (_isPoseTrackingActive.value) {
            return@withContext Result.failure(
                IllegalStateException("Pause or finish live camera pose tracking before running local LLM inference.")
            )
        }
        val modelFile = File(modelPath)
        if (!modelFile.exists() || modelFile.length() == 0L) {
            return@withContext Result.failure(
                IllegalStateException("Model file not found on device.")
            )
        }

        mutex.withLock {
            runCatching {
                Engine.setNativeMinLogSeverity(LogSeverity.ERROR)
                val backendsToTry = if (preferGpu) {
                    listOf("GPU" to Backend.GPU(), "CPU" to Backend.CPU())
                } else {
                    listOf("CPU" to Backend.CPU())
                }

                var lastError: Throwable? = null
                for ((backendLabel, backend) in backendsToTry) {
                    val attempt = runCatching {
                        val initStart = System.currentTimeMillis()
                        val config = EngineConfig(
                            modelPath = modelFile.absolutePath,
                            backend = backend,
                            cacheDir = cacheDir.absolutePath
                        )
                        Engine(config).use { engine ->
                            engine.initialize()
                            val initMs = System.currentTimeMillis() - initStart
                            val genStart = System.currentTimeMillis()
                            val conversationConfig = ConversationConfig(
                                systemInstruction = Contents.of(systemPrompt)
                            )
                            val builder = StringBuilder()
                            engine.createConversation(conversationConfig).use { conversation ->
                                conversation.sendMessageAsync(userPrompt).collect { chunk ->
                                    builder.append(chunk.toString())
                                }
                            }
                            val genMs = System.currentTimeMillis() - genStart
                            val outputText = builder.toString().trim()
                            require(outputText.isNotEmpty()) { "Model returned empty output." }
                            LocalInferenceCompletion(
                                text = outputText,
                                initTimeMs = initMs,
                                latencyMs = genMs,
                                backendUsed = backendLabel
                            )
                        }
                    }
                    if (attempt.isSuccess) return@runCatching attempt.getOrThrow()
                    lastError = attempt.exceptionOrNull()
                }
                throw lastError ?: IllegalStateException("Unable to initialize LiteRT-LM engine.")
            }
        }
    }

    private fun currentUsedHeapMb(): Long {
        val rt = Runtime.getRuntime()
        return ((rt.totalMemory() - rt.freeMemory()) / (1024L * 1024L)).coerceAtLeast(0L)
    }
}
