package com.aifitnesscoach.core.ai.external

import com.aifitnesscoach.core.database.AIProviderConfigEntity
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * OpenAI-compatible REST client using OkHttp with strict URL scheme enforcement:
 * - Public endpoints MUST use HTTPS (default TLS & hostname verification preserved).
 * - Cleartext HTTP is rejected unless BOTH the target host is loopback/private LAN
 *   AND [AIProviderConfigEntity.allowInsecureLocalHttp] is explicitly opted in.
 * - API keys are passed strictly in memory headers and never logged or echoed in errors.
 */
class AIProviderClient(
    private val baseClient: OkHttpClient = OkHttpClient()
) {
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    companion object {
        fun isPrivateOrLoopbackHost(host: String): Boolean {
            val lower = host.trim().lowercase()
            if (lower == "localhost" || lower == "127.0.0.1" || lower == "10.0.2.2" || lower == "::1" || lower.endsWith(".local")) {
                return true
            }
            val ipv4 = Regex("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$").matchEntire(lower)
            if (ipv4 != null) {
                val a = ipv4.groupValues[1].toIntOrNull() ?: return false
                val b = ipv4.groupValues[2].toIntOrNull() ?: return false
                val c = ipv4.groupValues[3].toIntOrNull() ?: return false
                val d = ipv4.groupValues[4].toIntOrNull() ?: return false
                if (listOf(a, b, c, d).any { it !in 0..255 }) return false
                return when {
                    a == 127 -> true
                    a == 10 -> true
                    a == 192 && b == 168 -> true
                    a == 172 && b in 16..31 -> true
                    a == 169 && b == 254 -> true
                    else -> false
                }
            }
            return runCatching {
                val addr = InetAddress.getByName(lower)
                addr.isLoopbackAddress || addr.isSiteLocalAddress || addr.isLinkLocalAddress
            }.getOrDefault(false)
        }

        fun validateEndpointUrl(
            baseUrl: String,
            endpointPath: String,
            allowInsecureLocalHttp: Boolean
        ): ProviderUrlValidation {
            val trimmedBase = baseUrl.trim().trimEnd('/')
            if (trimmedBase.isBlank()) {
                return ProviderUrlValidation(false, false, false, null, null, "Base URL is required.")
            }
            val parsedBase = trimmedBase.toHttpUrlOrNull()
                ?: return ProviderUrlValidation(false, false, false, null, null, "Invalid URL format.")

            val isHttp = parsedBase.scheme.equals("http", ignoreCase = true)
            val isHttps = parsedBase.scheme.equals("https", ignoreCase = true)
            if (!isHttp && !isHttps) {
                return ProviderUrlValidation(false, false, isHttp, null, null, "Only HTTPS (or local LAN HTTP with explicit warning) is allowed.")
            }

            val isLan = isPrivateOrLoopbackHost(parsedBase.host)
            if (isHttp && !isLan) {
                return ProviderUrlValidation(
                    valid = false,
                    isLanOrLoopback = false,
                    isHttp = true,
                    normalizedCompletionUrl = null,
                    normalizedModelsUrl = null,
                    errorMessage = "HTTPS is required for non-local servers."
                )
            }
            if (isHttp && isLan && !allowInsecureLocalHttp) {
                return ProviderUrlValidation(
                    valid = false,
                    isLanOrLoopback = true,
                    isHttp = true,
                    normalizedCompletionUrl = null,
                    normalizedModelsUrl = null,
                    errorMessage = "Cleartext HTTP to a local LAN server requires explicit opt-in after acknowledging the security warning."
                )
            }

            val cleanPath = endpointPath.trim().let {
                if (it.isBlank()) "/v1/chat/completions" else if (it.startsWith("/")) it else "/$it"
            }
            val completionUrl = "$trimmedBase$cleanPath"
            if (completionUrl.toHttpUrlOrNull() == null) {
                return ProviderUrlValidation(false, isLan, isHttp, null, null, "Invalid endpoint path.")
            }

            val modelsUrl = when {
                cleanPath.contains("/chat/completions") ->
                    "$trimmedBase${cleanPath.replace("/chat/completions", "/models")}"
                else -> "$trimmedBase/v1/models"
            }

            return ProviderUrlValidation(
                valid = true,
                isLanOrLoopback = isLan,
                isHttp = isHttp,
                normalizedCompletionUrl = completionUrl,
                normalizedModelsUrl = modelsUrl,
                errorMessage = null
            )
        }

        fun redactSecrets(message: String, apiKey: String?): String {
            if (apiKey.isNullOrBlank()) return message
            return message.replace(apiKey, "[REDACTED]")
        }
    }

    suspend fun testConnection(
        config: AIProviderConfigEntity,
        apiKey: String?
    ): ProviderConnectionTestResult = withContext(Dispatchers.IO) {
        val validation = validateEndpointUrl(config.baseUrl, config.endpointPath, config.allowInsecureLocalHttp)
        if (!validation.valid || validation.normalizedCompletionUrl == null) {
            return@withContext ProviderConnectionTestResult(
                success = false,
                reachable = false,
                authOk = false,
                modelCheckOk = false,
                completionOk = false,
                latencyMs = 0L,
                sampleResponse = "",
                errorMessage = validation.errorMessage
            )
        }

        val client = buildClient(config.timeoutSec)
        val headersMap = parseCustomHeaders(config.customHeadersJson)
        val startMs = System.currentTimeMillis()

        // Step 1: Reachability & model check via /models (some local servers may return 404 for /models)
        var reachable = false
        var authOk = true
        var modelAvailable = true
        val modelsUrl = validation.normalizedModelsUrl
        if (modelsUrl != null) {
            runCatching {
                val req = applyHeaders(Request.Builder().url(modelsUrl).get(), apiKey, headersMap).build()
                client.newCall(req).execute().use { resp ->
                    reachable = true
                    if (resp.code == 401 || resp.code == 403) {
                        authOk = false
                    } else if (resp.isSuccessful) {
                        val bodyStr = resp.body?.string().orEmpty()
                        if (bodyStr.isNotBlank() && config.modelId.isNotBlank()) {
                            // Verify model appears if server returns a models array
                            if (bodyStr.contains("\"data\"") && !bodyStr.contains(config.modelId)) {
                                modelAvailable = false
                            }
                        }
                    }
                }
            }
        }

        if (!authOk) {
            return@withContext ProviderConnectionTestResult(
                success = false,
                reachable = reachable,
                authOk = false,
                modelCheckOk = false,
                completionOk = false,
                latencyMs = System.currentTimeMillis() - startMs,
                sampleResponse = "",
                errorMessage = "Authentication failed (HTTP 401/403). Verify your API key."
            )
        }

        // Step 2: Real minimal completion test
        val completionRes = executeChatCompletion(
            client = client,
            url = validation.normalizedCompletionUrl,
            config = config.copy(maxTokens = minOf(config.maxTokens, 32), temperature = 0f),
            apiKey = apiKey,
            headersMap = headersMap,
            systemPrompt = "You are a connectivity test endpoint.",
            userPrompt = "Reply with OK."
        )
        val latency = System.currentTimeMillis() - startMs

        return@withContext completionRes.fold(
            onSuccess = { reply ->
                ProviderConnectionTestResult(
                    success = true,
                    reachable = true,
                    authOk = true,
                    modelCheckOk = modelAvailable,
                    completionOk = true,
                    latencyMs = latency,
                    sampleResponse = reply.take(160),
                    errorMessage = if (!modelAvailable) {
                        "Completion succeeded, though '${config.modelId}' was not listed in /models."
                    } else null
                )
            },
            onFailure = { err ->
                val safeMsg = redactSecrets(err.localizedMessage ?: "Connection failed", apiKey)
                ProviderConnectionTestResult(
                    success = false,
                    reachable = reachable || safeMsg.contains("HTTP "),
                    authOk = !safeMsg.contains("HTTP 401") && !safeMsg.contains("HTTP 403"),
                    modelCheckOk = modelAvailable,
                    completionOk = false,
                    latencyMs = latency,
                    sampleResponse = "",
                    errorMessage = safeMsg
                )
            }
        )
    }

    suspend fun runChatCompletion(
        config: AIProviderConfigEntity,
        apiKey: String?,
        systemPrompt: String,
        userPrompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val validation = validateEndpointUrl(config.baseUrl, config.endpointPath, config.allowInsecureLocalHttp)
        if (!validation.valid || validation.normalizedCompletionUrl == null) {
            return@withContext Result.failure(IllegalArgumentException(validation.errorMessage ?: "Invalid provider URL"))
        }
        val client = buildClient(config.timeoutSec)
        val headersMap = parseCustomHeaders(config.customHeadersJson)
        executeChatCompletion(
            client = client,
            url = validation.normalizedCompletionUrl,
            config = config,
            apiKey = apiKey,
            headersMap = headersMap,
            systemPrompt = systemPrompt,
            userPrompt = userPrompt
        )
    }

    private fun executeChatCompletion(
        client: OkHttpClient,
        url: String,
        config: AIProviderConfigEntity,
        apiKey: String?,
        headersMap: Map<String, String>,
        systemPrompt: String,
        userPrompt: String
    ): Result<String> {
        return runCatching {
            val payload = JsonObject().apply {
                addProperty("model", config.modelId.ifBlank { "gpt-4o-mini" })
                addProperty("temperature", config.temperature.coerceIn(0f, 1.5f))
                addProperty("max_tokens", config.maxTokens.coerceIn(16, 4096))
                add("messages", JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("role", "system")
                        addProperty("content", systemPrompt)
                    })
                    add(JsonObject().apply {
                        addProperty("role", "user")
                        addProperty("content", userPrompt)
                    })
                })
            }

            val body = gson.toJson(payload).toRequestBody(jsonMediaType)
            val request = applyHeaders(Request.Builder().url(url).post(body), apiKey, headersMap).build()
            client.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val snippet = redactSecrets(rawBody.take(220), apiKey)
                    error("HTTP ${response.code}: ${snippet.ifBlank { response.message }}")
                }
                val parsed = gson.fromJson(rawBody, JsonObject::class.java)
                val choices = parsed?.getAsJsonArray("choices")
                val first = choices?.firstOrNull()?.asJsonObject
                val content = first?.getAsJsonObject("message")?.get("content")?.asString
                    ?: first?.get("text")?.asString
                    ?: error("Provider response did not include choices[0].message.content")
                content.trim()
            }
        }
    }

    private fun buildClient(timeoutSec: Int): OkHttpClient {
        val sec = timeoutSec.coerceIn(5, 180).toLong()
        return baseClient.newBuilder()
            .connectTimeout(minOf(sec, 30L), TimeUnit.SECONDS)
            .readTimeout(sec, TimeUnit.SECONDS)
            .writeTimeout(sec, TimeUnit.SECONDS)
            .build()
    }

    private fun parseCustomHeaders(rawJson: String): Map<String, String> {
        if (rawJson.isBlank()) return emptyMap()
        return runCatching {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson<Map<String, String>>(rawJson, type).orEmpty()
        }.getOrDefault(emptyMap())
    }

    private fun applyHeaders(
        builder: Request.Builder,
        apiKey: String?,
        customHeaders: Map<String, String>
    ): Request.Builder {
        builder.addHeader("Content-Type", "application/json")
        if (!apiKey.isNullOrBlank()) {
            builder.addHeader("Authorization", "Bearer ${apiKey.trim()}")
        }
        customHeaders.forEach { (k, v) ->
            val key = k.trim()
            val value = v.trim()
            if (key.isNotBlank() && value.isNotBlank() && !key.equals("Authorization", ignoreCase = true)) {
                builder.addHeader(key, value)
            }
        }
        return builder
    }
}
