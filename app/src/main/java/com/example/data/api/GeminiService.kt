package com.example.data.api

import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class Part(val text: String)

@JsonClass(generateAdapter = true)
data class Content(val parts: List<Part>)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(val contents: List<Content>)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(val candidates: List<Candidate>?)

@JsonClass(generateAdapter = true)
data class Candidate(val content: Content?)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

// ================= OpenAI Compatible Protocol =================

@JsonClass(generateAdapter = true)
data class OpenAiChatMessage(
    val role: String,
    val content: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiChatMessage>,
    val stream: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class OpenAiChatChoice(
    val index: Int? = 0,
    val message: OpenAiChatMessage? = null
)

@JsonClass(generateAdapter = true)
data class OpenAiChatResponse(
    val id: String? = null,
    val choices: List<OpenAiChatChoice>? = null,
    val error: OpenAiError? = null
)

@JsonClass(generateAdapter = true)
data class OpenAiError(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null
)

interface OpenAiApiService {
    @POST
    suspend fun chatCompletions(
        @Url fullUrl: String,
        @Header("Authorization") authorization: String,
        @Body request: OpenAiChatRequest
    ): OpenAiChatResponse
}

object RetrofitClient {
    const val DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshiFactory = MoshiConverterFactory.create()
    private val serviceMap = mutableMapOf<String, GeminiApiService>()

    fun getGeminiService(baseUrl: String = DEFAULT_BASE_URL): GeminiApiService {
        val sanitized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return synchronized(serviceMap) {
            serviceMap.getOrPut(sanitized) {
                Retrofit.Builder()
                    .baseUrl(sanitized)
                    .client(okHttpClient)
                    .addConverterFactory(moshiFactory)
                    .build()
                    .create(GeminiApiService::class.java)
            }
        }
    }

    val geminiService: GeminiApiService
        get() = getGeminiService(DEFAULT_BASE_URL)

    val openAiService: OpenAiApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .client(okHttpClient)
            .addConverterFactory(moshiFactory)
            .build()
            .create(OpenAiApiService::class.java)
    }

    fun normalizeOpenAiUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim().removeSuffix("/")
        return if (trimmed.endsWith("/chat/completions")) {
            trimmed
        } else if (trimmed.endsWith("/v1") || trimmed.endsWith("/v4")) {
            "$trimmed/chat/completions"
        } else if (trimmed.contains("/v1/") || trimmed.contains("/v4/")) {
            "$trimmed/chat/completions"
        } else {
            "$trimmed/v1/chat/completions"
        }
    }
}
