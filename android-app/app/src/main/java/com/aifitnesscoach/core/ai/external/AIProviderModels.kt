package com.aifitnesscoach.core.ai.external

enum class ProviderPresetType(
    val displayNameEn: String,
    val displayNameFa: String,
    val defaultBaseUrl: String,
    val defaultEndpointPath: String,
    val defaultModelId: String,
    val isLocalLanDefault: Boolean,
    val requiresApiKeyUsually: Boolean
) {
    OPENAI_CLOUD(
        displayNameEn = "OpenAI-Compatible Cloud",
        displayNameFa = "سرویس ابری سازگار با OpenAI",
        defaultBaseUrl = "https://api.openai.com",
        defaultEndpointPath = "/v1/chat/completions",
        defaultModelId = "gpt-4o-mini",
        isLocalLanDefault = false,
        requiresApiKeyUsually = true
    ),
    OLLAMA_LAN(
        displayNameEn = "Ollama (Local LAN / Home Server)",
        displayNameFa = "Ollama (شبکه محلی / سرور خانگی)",
        defaultBaseUrl = "http://192.168.1.50:11434",
        defaultEndpointPath = "/v1/chat/completions",
        defaultModelId = "llama3.2",
        isLocalLanDefault = true,
        requiresApiKeyUsually = false
    ),
    LM_STUDIO(
        displayNameEn = "LM Studio (Local LAN)",
        displayNameFa = "LM Studio (شبکه محلی)",
        defaultBaseUrl = "http://192.168.1.50:1234",
        defaultEndpointPath = "/v1/chat/completions",
        defaultModelId = "local-model",
        isLocalLanDefault = true,
        requiresApiKeyUsually = false
    ),
    VLLM_SERVER(
        displayNameEn = "vLLM Server",
        displayNameFa = "سرور vLLM",
        defaultBaseUrl = "https://llm.example.com",
        defaultEndpointPath = "/v1/chat/completions",
        defaultModelId = "meta-llama/Llama-3.1-8B-Instruct",
        isLocalLanDefault = false,
        requiresApiKeyUsually = true
    ),
    CUSTOM_COMPATIBLE(
        displayNameEn = "Custom OpenAI-Compatible Endpoint",
        displayNameFa = "سرویس سفارشی سازگار با OpenAI",
        defaultBaseUrl = "https://api.example.com",
        defaultEndpointPath = "/v1/chat/completions",
        defaultModelId = "default-model",
        isLocalLanDefault = false,
        requiresApiKeyUsually = true
    )
}

data class ProviderUrlValidation(
    val valid: Boolean,
    val isLanOrLoopback: Boolean,
    val isHttp: Boolean,
    val normalizedCompletionUrl: String?,
    val normalizedModelsUrl: String?,
    val errorMessage: String? = null
)

data class ProviderConnectionTestResult(
    val success: Boolean,
    val reachable: Boolean,
    val authOk: Boolean,
    val modelCheckOk: Boolean,
    val completionOk: Boolean,
    val latencyMs: Long,
    val sampleResponse: String,
    val errorMessage: String? = null
)
