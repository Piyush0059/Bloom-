package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class BloomLanguage(val code: String, val displayName: String, val speechLocale: String) {
    KO("ko", "한국어 (Korean)", "ko-KR"),
    EN("en", "English", "en-US"),
    HINGLISH("hinglish", "Hinglish", "en-IN");

    companion object {
        fun fromCode(code: String?): BloomLanguage {
            return when (code?.lowercase()?.trim()) {
                "ko", "korean" -> KO
                "hinglish" -> HINGLISH
                "en", "english" -> EN
                else -> EN
            }
        }

        fun isValidCode(code: String?): Boolean {
            val normalized = code?.lowercase()?.trim()
            return normalized == "ko" || normalized == "en" || normalized == "hinglish"
        }
    }
}

enum class LanguageStatus {
    INITIALIZING,
    REQUIRED,
    READY,
    ERROR
}

@IgnoreExtraProperties
data class UserProfile(
    val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val voicePreference: String = "Aoede",
    val customVoiceId: String = "Aoede",
    val preferredLanguage: String = "en",
    val cloudHistoryEnabled: Boolean = true,
    val persistentMemoryEnabled: Boolean = true,
    val onboarded: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Session(
    val id: String = "",
    val userId: String = "",
    val title: String = "Bloom Session",
    val language: String = "en",
    val voicePreference: String = "Aoede",
    val customVoiceId: String = "Aoede",
    val messageCount: Long = 0L,
    val summary: String = "",
    val confirmedFeelings: List<String> = emptyList(),
    val agreedNextSteps: List<String> = emptyList(),
    val lastMessageSnippet: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Message(
    val id: String = "",
    val sessionId: String = "",
    val userId: String = "",
    val role: String = "user", // "user", "assistant", "crisis_alert"
    val text: String = "",
    val modality: String = "text", // "text", "voice"
    val helpfulnessRating: String = "", // "helpful", "little", "not_yet", "change_approach"
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MemoryItem(
    val id: String = "",
    val userId: String = "",
    val category: String = "preference", // "preference", "goal", "background", "instruction"
    val content: String = "",
    val sourceConversationId: String = "",
    val userConfirmed: Boolean = true,
    val status: String = "active", // "active", "archived", "deleted"
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class OrbState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    INTERRUPTED,
    CRISIS
}

data class CrisisResource(
    val name: String,
    val phone: String,
    val description: String,
    val region: String,
    val hours: String = "24/7 Free & Confidential"
)
