package com.example.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonObject

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val tools: List<JsonObject>? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val role: String? = null,
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@Serializable
data class InlineData(
    val mimeType: String,
    val data: String
)

@Serializable
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val thinkingConfig: ThinkingConfig? = null,
    val responseModalities: List<String>? = null,
    val speechConfig: SpeechConfig? = null
)

@Serializable
data class ThinkingConfig(
    val thinkingLevel: String
)

@Serializable
data class SpeechConfig(
    val voiceConfig: VoiceConfig,
    val languageCode: String? = null
)

@Serializable
data class VoiceConfig(
    val prebuiltVoiceConfig: PrebuiltVoiceConfig? = null,
    val voice: String? = null
)

@Serializable
data class PrebuiltVoiceConfig(
    val voiceName: String
)


@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate> = emptyList()
)

@Serializable
data class Candidate(
    val content: Content? = null,
    val finishReason: String? = null
)


@Serializable
data class InteractionRequest(
    val model: String,
    val input: String,
    @SerialName("response_format") val responseFormat: InteractionResponseFormat,
    @SerialName("generation_config") val generationConfig: InteractionGenerationConfig
)

@Serializable
data class InteractionResponseFormat(
    val type: String = "audio"
)

@Serializable
data class InteractionGenerationConfig(
    @SerialName("speech_config") val speechConfig: List<InteractionSpeechConfig>
)

@Serializable
data class InteractionSpeechConfig(
    val voice: String
)

@Serializable
data class InteractionResponse(
    @SerialName("output_audio") val outputAudio: InteractionAudio? = null,
    val steps: List<InteractionStep> = emptyList()
)

@Serializable
data class InteractionAudio(
    val data: String? = null,
    @SerialName("mime_type") val mimeType: String? = null
)

@Serializable
data class InteractionStep(
    val type: String? = null,
    val content: List<InteractionContent> = emptyList()
)

@Serializable
data class InteractionContent(
    val type: String? = null,
    val data: String? = null,
    @SerialName("mime_type") val mimeType: String? = null
)
