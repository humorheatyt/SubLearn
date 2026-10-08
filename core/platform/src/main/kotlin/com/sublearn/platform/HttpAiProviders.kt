package com.sublearn.platform

import com.sublearn.domain.AiProvider
import com.sublearn.domain.AiProviderFactory
import com.sublearn.domain.AiRequest
import com.sublearn.domain.AiResponse
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject

/** Online providers are invoked only after an explicit user action. Request content is never logged. */
class HttpAiProviderFactory : AiProviderFactory {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(75, TimeUnit.SECONDS)
        .build()

    private val providers: Map<String, AiProvider> = listOf(
        GeminiAiProvider(client),
        OpenAiProvider(client),
        AnthropicAiProvider(client),
    ).associateBy(AiProvider::id)

    override fun get(providerId: String): AiProvider = providers[providerId]
        ?: throw IllegalArgumentException("Unknown AI provider: $providerId")

    override fun supportedProviders(): List<Pair<String, String>> = providers.values.map { it.id to it.displayName }
}

private abstract class JsonHttpAiProvider(
    protected val client: OkHttpClient,
) : AiProvider {
    protected val mediaType = "application/json; charset=utf-8".toMediaType()

    protected suspend fun post(url: String, headers: Map<String, String>, json: JSONObject): JSONObject {
        val request = Request.Builder().url(url).post(json.toString().toRequestBody(mediaType)).apply {
            headers.forEach { (name, value) -> header(name, value) }
        }.build()
        val response = client.newCall(request).awaitBody()
        if (response.code !in 200..299) {
            throw IOException("${displayName} request failed (HTTP ${response.code}); check network and API key.")
        }
        return try {
            JSONObject(response.body)
        } catch (_: Exception) {
            throw IOException("${displayName} returned an unreadable response.")
        }
    }

    protected fun model(request: AiRequest, fallback: String): String {
        val value = request.model?.takeIf { it.matches(Regex("[A-Za-z0-9_.:-]{1,128}")) } ?: fallback
        return value
    }

    protected fun requireKey(key: String) {
        if (key.isBlank()) throw IllegalStateException("Add an API key in AI settings before asking for help.")
    }

    protected fun promptBody(request: AiRequest): String = request.prompt.ifBlank { request.currentBlock }
}

private class GeminiAiProvider(client: OkHttpClient) : JsonHttpAiProvider(client) {
    override val id = "gemini"
    override val displayName = "Gemini"

    override suspend fun complete(request: AiRequest, apiKey: String): AiResponse {
        requireKey(apiKey)
        val model = model(request, "gemini-2.0-flash")
        val body = JSONObject().put(
            "contents",
            JSONArray().put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", promptBody(request))))),
        ).put("generationConfig", JSONObject().put("temperature", 0.25).put("maxOutputTokens", 700))
        val result = post(
            "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent",
            mapOf("x-goog-api-key" to apiKey, "Accept" to "application/json"),
            body,
        )
        val candidates = result.optJSONArray("candidates")
        val text = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
            ?.let { parts -> (0 until parts.length()).mapNotNull { parts.optJSONObject(it)?.optString("text")?.takeIf(String::isNotBlank) }.joinToString("\n") }
            .orEmpty()
        if (text.isBlank()) throw IOException("Gemini returned no answer.")
        return AiResponse(text, id, model)
    }
}

private class OpenAiProvider(client: OkHttpClient) : JsonHttpAiProvider(client) {
    override val id = "openai"
    override val displayName = "OpenAI"

    override suspend fun complete(request: AiRequest, apiKey: String): AiResponse {
        requireKey(apiKey)
        val model = model(request, "gpt-4o-mini")
        val body = JSONObject().put("model", model)
            .put("temperature", 0.25)
            .put("max_tokens", 700)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", promptBody(request))))
        val result = post(
            "https://api.openai.com/v1/chat/completions",
            mapOf("Authorization" to "Bearer $apiKey", "Accept" to "application/json"),
            body,
        )
        val text = result.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content").orEmpty()
        if (text.isBlank()) throw IOException("OpenAI returned no answer.")
        return AiResponse(text, id, model)
    }
}

private class AnthropicAiProvider(client: OkHttpClient) : JsonHttpAiProvider(client) {
    override val id = "anthropic"
    override val displayName = "Claude"

    override suspend fun complete(request: AiRequest, apiKey: String): AiResponse {
        requireKey(apiKey)
        val model = model(request, "claude-3-5-haiku-latest")
        val body = JSONObject().put("model", model)
            .put("max_tokens", 700)
            .put("temperature", 0.25)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", promptBody(request))))
        val result = post(
            "https://api.anthropic.com/v1/messages",
            mapOf("x-api-key" to apiKey, "anthropic-version" to "2023-06-01", "Accept" to "application/json"),
            body,
        )
        val blocks = result.optJSONArray("content")
        val text = blocks?.let { content -> (0 until content.length()).mapNotNull { content.optJSONObject(it)?.optString("text")?.takeIf(String::isNotBlank) }.joinToString("\n") }.orEmpty()
        if (text.isBlank()) throw IOException("Claude returned no answer.")
        return AiResponse(text, id, model)
    }
}

private data class HttpResponseBody(val code: Int, val body: String)

private suspend fun Call.awaitBody(): HttpResponseBody = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, error: IOException) {
            if (continuation.isActive) continuation.resumeWith(Result.failure(error))
        }

        override fun onResponse(call: Call, response: Response) {
            val value = response.use { HttpResponseBody(it.code, it.body?.string().orEmpty()) }
            if (continuation.isActive) continuation.resume(value)
        }
    })
}
