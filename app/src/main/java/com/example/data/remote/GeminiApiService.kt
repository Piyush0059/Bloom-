package com.example.data.remote

import com.example.BuildConfig
import com.example.data.model.BloomLanguage
import com.example.data.model.MemoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Header
import java.util.concurrent.TimeUnit

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse

    @POST("v1beta/interactions")
    suspend fun generateInteraction(
        @Header("x-goog-api-key") apiKey: String,
        @Body request: InteractionRequest
    ): InteractionResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = false
        }
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

object GeminiService {
    fun getApiKey(): String {
        val personalKey = GeminiApiKeyStore.getStoredKey()
        return personalKey.ifBlank { BuildConfig.GEMINI_API_KEY.ifBlank { "" } }
    }

    private fun getSystemInstruction(
        language: BloomLanguage,
        retrievedMemories: List<MemoryItem>,
        recentSummaries: List<String>
    ): Content {
        val langDirective = when (language) {
            BloomLanguage.KO -> """
Reply strictly and naturally in Korean (한국어).
- Use proper Korean grammar and natural, respectful, empathetic phrasing (존댓말, 주로 해요체).
- Write primarily in Hangul.
- Cautiously reflect possible emotions using tentative phrasing (e.g. "~하신 마음이 느껴져요", "제가 잘못 짚은 것일 수도 있지만...").
- Keep ordinary conversational responses concise and focused (1-4 sentences).
            """.trimIndent()
            BloomLanguage.HINGLISH -> """
Understand conversational Hindi-English code-switching and reply strictly in natural conversational Hinglish.
- Mix Hindi and English words naturally written in Latin script (e.g., "Main samajh sakta hoon ki aap kitna overwhelmed feel kar rahe hain...").
- Never force formal Hindi or Devanagari script.
- Do not translate everything rigidly; match everyday spoken cadence.
- Keep ordinary conversational responses concise and focused (1-4 sentences).
            """.trimIndent()
            BloomLanguage.EN -> """
Reply strictly in natural, clear, conversational English.
- Avoid overly formal, rigid, or robotic prose.
- Acknowledge and validate emotional experience first before offering solutions.
- Keep ordinary conversational responses concise and focused (1-4 sentences).
            """.trimIndent()
        }

        val memoryContextBuilder = StringBuilder()
        if (retrievedMemories.isNotEmpty() || recentSummaries.isNotEmpty()) {
            memoryContextBuilder.appendLine("\n--- [BLOOM DURABLE CROSS-SESSION MEMORY] ---")
            if (recentSummaries.isNotEmpty()) {
                memoryContextBuilder.appendLine("Recent Session Summaries:")
                recentSummaries.forEach { summary ->
                    memoryContextBuilder.appendLine("• $summary")
                }
            }
            if (retrievedMemories.isNotEmpty()) {
                memoryContextBuilder.appendLine("Confirmed Long-Term User Facts & Context:")
                retrievedMemories.forEach { mem ->
                    memoryContextBuilder.appendLine("• [${mem.category.uppercase()}]: ${mem.content}")
                }
            }
            memoryContextBuilder.appendLine("Rule: Use this persistent context naturally to maintain continuity across sessions without re-asking background details. Do not claim to remember facts that are not present here.")
        }

        val prompt = """
You are Bloom — an attentive, emotionally intelligent, and deeply supportive AI companion focused on personal growth, active listening, and practical guidance.

Core Identity & Safety:
1. You are Bloom, an AI companion. You are NOT a human, and NOT a licensed psychologist, psychiatrist, or medical healthcare provider.
2. Never claim to diagnose mental health conditions (depression, anxiety, bipolar, etc.) or promise instant healing.
3. Interaction Sequence:
   (a) Listen attentively to the user's immediate message.
   (b) Validate the emotional experience before jumping into solutions.
   (c) Tentatively reflect feelings ("It sounds like...", "I might be misreading this, but...").
   (d) Allow the user to correct your interpretation without friction.
   (e) Ask at most ONE gentle, relevant question to help them reflect when appropriate.
   (f) Offer practical guidance only when requested or suitable.
   (g) Check whether your response feels helpful without pressure.
4. Tone & Style:
   - Warm, grounded, attentive, respectful, non-judgmental.
   - Conversational length: usually 1 to 4 sentences for regular turns.
   - Avoid clichés, empty positivity, and repetitive robotic apologies.
   - Never guilt, shame, pressure, or foster emotional exclusivity/dependency.
5. Safety & Crisis Protocol:
   - If the user hints at or discloses self-harm, suicide risk, abuse, or immediate danger: prioritize calm, direct, compassionate human-support guidance.
   - Offer clear, grounding, immediate steps. Urge them to contact trusted people and free emergency services:
     • India: Tele-MANAS (14416 or 1800-89-14416) and 112
     • Korea: Mental Health Crisis Line (1577-0199), Suicide Prevention Line (1393), Emergency (112 or 119)
     • International / US: 988
6. Language Directive:
$langDirective

$memoryContextBuilder
        """.trimIndent()

        return Content(parts = listOf(Part(text = prompt)))
    }

    suspend fun generateSupportResponse(
        contents: List<Content>,
        isDeepThinking: Boolean,
        language: BloomLanguage,
        retrievedMemories: List<MemoryItem> = emptyList(),
        recentSummaries: List<String> = emptyList()
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("GEMINI_API_KEY is not configured in Secrets panel."))
        }

        val model = if (isDeepThinking) "gemini-3.1-pro-preview" else "gemini-3.8-flash"
        val thinkingConfig = if (isDeepThinking) ThinkingConfig(thinkingLevel = "high") else null

        val request = GenerateContentRequest(
            contents = contents,
            systemInstruction = getSystemInstruction(language, retrievedMemories, recentSummaries),
            generationConfig = GenerationConfig(
                temperature = if (isDeepThinking) 0.4f else 0.7f,
                thinkingConfig = thinkingConfig
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(model, apiKey, request)
            val candidate = response.candidates.firstOrNull()
            val text = candidate?.content?.parts?.mapNotNull { it.text }?.joinToString("\n")
            if (text.isNullOrBlank()) {
                Result.failure(IllegalStateException("Bloom received an empty response from model."))
            } else {
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateTTSAudio(
        text: String,
        customVoiceId: String,
        language: String = "en"
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("GEMINI_API_KEY is not configured."))
        }

        val voiceId = customVoiceId.trim()
        if (!voiceId.matches(Regex("^(voice_[A-Za-z0-9_-]{6,}|voicekey_[A-Za-z0-9_-]{6,})$"))) {
            return@withContext Result.failure(
                IllegalStateException("Friend's voice is not configured. Add a valid Gemini replicated voice ID after the voice owner completes the consent process.")
            )
        }

        val request = InteractionRequest(
            model = "gemini-3.8-flash-tts",
            input = text.take(1800),
            responseFormat = InteractionResponseFormat(type = "audio"),
            generationConfig = InteractionGenerationConfig(
                speechConfig = listOf(InteractionSpeechConfig(voice = voiceId))
            )
        )

        try {
            val response = RetrofitClient.service.generateInteraction(apiKey, request)
            val audioBase64 = response.outputAudio?.data
                ?: response.steps.asSequence()
                    .filter { it.type == "model_output" }
                    .flatMap { it.content.asSequence() }
                    .firstOrNull { it.type == "audio" && !it.data.isNullOrBlank() }
                    ?.data
            if (audioBase64.isNullOrBlank()) {
                Result.failure(IllegalStateException("Gemini returned no audio for the configured friend voice."))
            } else {
                val bytes = android.util.Base64.decode(audioBase64, android.util.Base64.DEFAULT)
                if (bytes.isEmpty()) {
                    Result.failure(IllegalStateException("Gemini returned an empty audio payload."))
                } else {
                    Result.success(bytes)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchGroundedCrisisResources(query: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("GEMINI_API_KEY is not configured."))
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = "Find up-to-date official crisis helplines, suicide prevention hotlines, mental health support organizations, and contact numbers for: $query. Provide the organization name, phone numbers, hours, and whether it is free/confidential in short, clear text.")))
            ),
            tools = listOf(
                buildJsonObject {
                    put("googleSearch", buildJsonObject {})
                }
            )
        )

        try {
            val response = RetrofitClient.service.generateContent("gemini-3.8-flash", apiKey, request)
            val text = response.candidates.firstOrNull()?.content?.parts?.mapNotNull { it.text }?.joinToString("\n")
            if (text.isNullOrBlank()) {
                Result.failure(IllegalStateException("No search grounding results found."))
            } else {
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun transcribeAudio(base64Audio: String, mimeType: String = "audio/wav"): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("GEMINI_API_KEY is not configured."))
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(inlineData = InlineData(mimeType = mimeType, data = base64Audio)),
                        Part(text = "Transcribe the speech in this audio accurately. Output only the verbatim transcribed text without commentary.")
                    )
                )
            )
        )

        try {
            val response = RetrofitClient.service.generateContent("gemini-3.8-flash", apiKey, request)
            val text = response.candidates.firstOrNull()?.content?.parts?.mapNotNull { it.text }?.joinToString(" ")
            if (text.isNullOrBlank()) {
                Result.failure(IllegalStateException("Could not transcribe audio."))
            } else {
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
