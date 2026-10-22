package com.example.ui.viewmodel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.VoiceManager
import com.example.data.model.BloomLanguage
import com.example.data.model.CrisisResource
import com.example.data.model.Message
import com.example.data.model.OrbState
import com.example.data.model.Session
import com.example.data.model.UserProfile
import com.example.data.remote.Content
import com.example.data.remote.GeminiService
import com.example.data.remote.Part
import com.example.data.repository.AuraRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuraViewModel(
    private val repository: AuraRepository,
    val voiceManager: VoiceManager
) : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Auth & User State
    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isGuestMode = MutableStateFlow(auth.currentUser == null)
    val isGuestMode: StateFlow<Boolean> = _isGuestMode.asStateFlow()

    private val _userProfile = MutableStateFlow(
        UserProfile(
            id = auth.currentUser?.uid ?: "guest_user",
            userId = auth.currentUser?.uid ?: "guest_user",
            displayName = auth.currentUser?.displayName ?: "Guest Companion",
            voicePreference = "None",
            preferredLanguage = "en",
            cloudHistoryEnabled = auth.currentUser != null,
            onboarded = false
        )
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    // Active Session & Messages
    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()

    private val _activeSession = MutableStateFlow<Session?>(null)
    val activeSession: StateFlow<Session?> = _activeSession.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    // Conversation State
    private val _orbState = MutableStateFlow(OrbState.IDLE)
    val orbState: StateFlow<OrbState> = _orbState.asStateFlow()

    private val _isVoiceMode = MutableStateFlow(false)
    val isVoiceMode: StateFlow<Boolean> = _isVoiceMode.asStateFlow()

    private val _isDeepThinking = MutableStateFlow(false)
    val isDeepThinking: StateFlow<Boolean> = _isDeepThinking.asStateFlow()

    private val _textInput = MutableStateFlow("")
    val textInput: StateFlow<String> = _textInput.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private fun isUsableFriendVoiceId(value: String?): Boolean =
        value != null && Regex("^(voice_[A-Za-z0-9_-]{6,}|voicekey_[A-Za-z0-9_-]{6,})$").matches(value.trim())

    private val _showFeedbackPrompt = MutableStateFlow(false)
    val showFeedbackPrompt: StateFlow<Boolean> = _showFeedbackPrompt.asStateFlow()

    private val _lastAssistantMessageId = MutableStateFlow<String?>(null)
    val lastAssistantMessageId: StateFlow<String?> = _lastAssistantMessageId.asStateFlow()

    private val _isHelpNowVisible = MutableStateFlow(false)
    val isHelpNowVisible: StateFlow<Boolean> = _isHelpNowVisible.asStateFlow()

    private val _groundedCrisisResults = MutableStateFlow<String?>(null)
    val groundedCrisisResults: StateFlow<String?> = _groundedCrisisResults.asStateFlow()

    private val _isSearchingGroundedCrisis = MutableStateFlow(false)
    val isSearchingGroundedCrisis: StateFlow<Boolean> = _isSearchingGroundedCrisis.asStateFlow()

    // Default India & Global Helplines
    val defaultCrisisResources = listOf(
        CrisisResource(
            name = "Tele-MANAS (Govt of India)",
            phone = "14416",
            description = "National Tele Mental Health Programme of India. Free 24/7 psychological support in 20+ languages.",
            region = "India"
        ),
        CrisisResource(
            name = "Tele-MANAS Toll-Free",
            phone = "18008914416",
            description = "Alternate toll-free national line for emotional crisis and psychiatric support.",
            region = "India"
        ),
        CrisisResource(
            name = "Mental Health Crisis Line (정신건강위기상담)",
            phone = "15770199",
            description = "24/7 mental health crisis consultation service (South Korea).",
            region = "Korea"
        ),
        CrisisResource(
            name = "Suicide Prevention Line (자살예방상담전화)",
            phone = "109",
            description = "24/7 national emergency suicide prevention hotline (South Korea).",
            region = "Korea"
        ),
        CrisisResource(
            name = "Emergency Services (India)",
            phone = "112",
            description = "Unified national emergency number for immediate police, medical, or fire assistance.",
            region = "India"
        ),
        CrisisResource(
            name = "KIRAN Helpline",
            phone = "18005990019",
            description = "Mental health rehabilitation helpline by Ministry of Social Justice & Empowerment.",
            region = "India"
        ),
        CrisisResource(
            name = "988 Suicide & Crisis Lifeline",
            phone = "988",
            description = "Free, confidential 24/7 support for people in suicidal crisis or emotional distress (US/Canada).",
            region = "International"
        )
    )

    init {
        // Observe auth state changes
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user
            _isGuestMode.value = (user == null)
            loadUserAndSessions()
        }
        loadUserAndSessions()
        viewModelScope.launch {
            voiceManager.voiceError.collect { error ->
                if (!error.isNullOrBlank()) _statusMessage.value = error
            }
        }
    }

    private fun loadUserAndSessions() {
        val uid = _currentUser.value?.uid ?: "guest_user"
        viewModelScope.launch {
            repository.observeUserProfile(uid).collect { profile ->
                if (profile != null) {
                    _userProfile.value = profile
                    _isVoiceMode.value = profile.voicePreference == "Friend" &&
                        isUsableFriendVoiceId(profile.customVoiceId)
                }
            }
        }
        viewModelScope.launch {
            repository.observeSessions(uid).collect { sessionList ->
                _sessions.value = sessionList
                if (_activeSession.value == null && sessionList.isNotEmpty()) {
                    selectSession(sessionList.first())
                } else if (_activeSession.value == null && sessionList.isEmpty()) {
                    startNewSession("First Conversation")
                }
            }
        }
    }

    fun selectSession(session: Session) {
        _activeSession.value = session
        val uid = _currentUser.value?.uid ?: "guest_user"
        viewModelScope.launch {
            repository.observeMessages(uid, session.id).collect { msgList ->
                _messages.value = msgList
                val lastAssistant = msgList.lastOrNull { it.role == "assistant" }
                _lastAssistantMessageId.value = lastAssistant?.id
            }
        }
    }

    fun startNewSession(title: String = "Supportive Session") {
        val uid = _currentUser.value?.uid ?: "guest_user"
        val voice = _userProfile.value.voicePreference
        val lang = _userProfile.value.preferredLanguage
        val customVoiceId = _userProfile.value.customVoiceId

        viewModelScope.launch {
            val result = repository.createSession(uid, title, voice, lang, customVoiceId)
            if (result.isSuccess) {
                val newSessionId = result.getOrNull()
                if (newSessionId != null) {
                    val session = Session(
                        id = newSessionId,
                        userId = uid,
                        title = title,
                        voicePreference = voice,
                        customVoiceId = customVoiceId,
                        language = lang
                    )
                    _activeSession.value = session
                    selectSession(session)
                    // Introduce Bloom warmly
                    sendWelcomeGreeting(session)
                }
            }
        }
    }

    private fun sendWelcomeGreeting(session: Session) {
        val uid = _currentUser.value?.uid ?: "guest_user"
        val greeting = when (_userProfile.value.preferredLanguage) {
            "ko" -> "안녕하세요. 저는 Bloom입니다. 당신의 마음에 귀 기울이고 함께하기 위해 여기 있어요. 오늘 마음이 어떠신가요?"
            "hinglish" -> "Hi, I am Bloom. Main yahan bina kisi judgement ke aapko sunne ke liye hoon. Aaj aap kaisa feel kar rahe hain?"
            else -> "Hello. I'm Bloom — an emotional support companion here to listen, understand, and be with you. How are you feeling today?"
        }

        viewModelScope.launch {
            repository.addMessage(uid, session.id, "assistant", greeting, if (_isVoiceMode.value) "voice" else "text")
            if (_isVoiceMode.value && isUsableFriendVoiceId(_userProfile.value.customVoiceId)) {
                voiceManager.speak(greeting, _userProfile.value.customVoiceId, _userProfile.value.preferredLanguage)
            }
        }
    }

    fun setTextInput(text: String) {
        _textInput.value = text
    }

    fun setVoiceMode(enabled: Boolean) {
        if (enabled && !isUsableFriendVoiceId(_userProfile.value.customVoiceId)) {
            _isVoiceMode.value = false
            _statusMessage.value = "Friend's voice is not configured. Add a valid Gemini replicated voice ID in Settings; Bloom will remain text-only."
            return
        }
        if (enabled && _userProfile.value.voicePreference != "Friend") {
            val updated = _userProfile.value.copy(voicePreference = "Friend")
            _userProfile.value = updated
            viewModelScope.launch { repository.saveUserProfile(updated) }
        }
        _isVoiceMode.value = enabled
        if (!enabled) {
            voiceManager.stopListeningManually()
            voiceManager.stopAudioPlayback()
            _orbState.value = OrbState.IDLE
        } else {
            _statusMessage.value = "Only the configured friend voice will be used."
        }
    }

    fun toggleDeepThinking() {
        _isDeepThinking.value = !_isDeepThinking.value
    }

    fun setHelpNowVisible(visible: Boolean) {
        _isHelpNowVisible.value = visible
    }

    fun completeOnboarding(voicePreference: String, language: String, stayGuest: Boolean) {
        val selectedVoice = if (voicePreference == "Friend") "Friend" else "None"
        val updated = _userProfile.value.copy(
            voicePreference = selectedVoice,
            preferredLanguage = language,
            onboarded = true,
            cloudHistoryEnabled = !stayGuest
        )
        _userProfile.value = updated
        _isVoiceMode.value = selectedVoice == "Friend" && isUsableFriendVoiceId(updated.customVoiceId)
        if (selectedVoice == "Friend" && !_isVoiceMode.value) {
            _statusMessage.value = "Friend's voice is not configured yet. Add the verified replica ID in Settings; no other voice will be used."
        }
        viewModelScope.launch {
            repository.saveUserProfile(updated)
            if (_activeSession.value == null) {
                startNewSession("Reflective Dialogue")
            }
        }
    }

    // --- Sending Messages & Turn-Taking ---

    fun onUserSendMessage(text: String, modality: String = "text") {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        val currentSess = _activeSession.value ?: return
        val uid = _currentUser.value?.uid ?: "guest_user"

        // Stop any active audio immediately
        voiceManager.stopAudioPlayback()
        _textInput.value = ""
        _showFeedbackPrompt.value = false

        viewModelScope.launch {
            // Save user message
            repository.addMessage(uid, currentSess.id, "user", trimmed, modality)

            // 1. Safety & Crisis Analysis
            if (isPotentialCrisis(trimmed)) {
                handleCrisisFlow(currentSess, trimmed, modality)
                return@launch
            }

            // 2. Normal Empathetic Turn
            _orbState.value = OrbState.PROCESSING
            _statusMessage.value = if (_isDeepThinking.value) "Reflecting deeply on emotional nuances..." else "Bloom is listening and reflecting..."

            val history = _messages.value.takeLast(10).map { msg ->
                Content(
                    role = if (msg.role == "user") "user" else "model",
                    parts = listOf(Part(text = msg.text))
                )
            } + Content(role = "user", parts = listOf(Part(text = trimmed)))

            val recentSummaries = _sessions.value
                .filter { it.id != currentSess.id }
                .mapNotNull { it.summary.ifBlank { null } }
                .take(5)

            val result = GeminiService.generateSupportResponse(
                contents = history,
                isDeepThinking = _isDeepThinking.value,
                language = BloomLanguage.fromCode(_userProfile.value.preferredLanguage),
                recentSummaries = recentSummaries
            )

            if (result.isSuccess) {
                val responseText = result.getOrNull() ?: ""
                _orbState.value = if (_isVoiceMode.value) OrbState.SPEAKING else OrbState.IDLE
                _statusMessage.value = null

                val assistantMsgId = repository.addMessage(uid, currentSess.id, "assistant", responseText, modality).getOrNull()
                _lastAssistantMessageId.value = assistantMsgId

                // Vocalize response if in voice mode and not text only
                if (_isVoiceMode.value && isUsableFriendVoiceId(_userProfile.value.customVoiceId)) {
                    voiceManager.speak(responseText, _userProfile.value.customVoiceId, _userProfile.value.preferredLanguage)
                }

                // Show supportive feedback check every few turns
                if (_messages.value.size >= 4) {
                    _showFeedbackPrompt.value = true
                }
            } else {
                _orbState.value = OrbState.IDLE
                val err = result.exceptionOrNull()?.localizedMessage ?: "Connection error"
                _statusMessage.value = "Bloom could not complete turn: $err"
                val fallbackMsg = "I'm right here with you. It seems there was a network pause, but please take your time and continue when you're ready."
                repository.addMessage(uid, currentSess.id, "assistant", fallbackMsg, modality)
            }
        }
    }

    private fun isPotentialCrisis(text: String): Boolean {
        val lower = text.lowercase()
        val crisisTriggers = listOf(
            "suicide", "kill myself", "end my life", "want to die", "hurt myself",
            "self harm", "cutting myself", "hang myself", "take all the pills",
            "no reason to live", "better off dead", "khudkushi", "jaan lena", "marna chahta",
            "abused", "being beaten", "assaulted"
        )
        return crisisTriggers.any { lower.contains(it) }
    }

    private suspend fun handleCrisisFlow(session: Session, userText: String, modality: String) {
        val uid = _currentUser.value?.uid ?: "guest_user"
        _orbState.value = OrbState.CRISIS
        _isHelpNowVisible.value = true

        val crisisMessage = when (_userProfile.value.preferredLanguage) {
            "ko" -> """
지금 많이 지치고 힘드신 마음 깊이 전해집니다. 당신의 안전과 생명은 그 무엇보다 소중합니다. 이 고통을 혼자 견디지 마세요.

지금 바로 안전하고 따뜻한 전문 도움을 요청해 주세요:
• 정신건강 위기상담전화: 1577-0199 (24시간)
• 자살예방 상담전화: 109 (24시간)
• 긴급 신고: 112 또는 119

지금 안전한 곳으로 이동하시고, 믿을 수 있는 분이나 전문 기관에 도움을 청해 주세요. Bloom이 항상 곁에 있겠습니다.
            """.trimIndent()
            "hinglish" -> """
Main aapki baat dhyan se sun raha hoon, aur aapki safety sabse zaroori hai. Please akele mat rahiye.

Abhi in free & confidential helplines par call karein:
• Tele-MANAS: 14416 ya 1800-89-14416
• National Emergency: 112

Please kisi trusted insaan ke paas jayiye. Aap akele nahi hain.
            """.trimIndent()
            else -> """
I hear how deeply overwhelmed you feel, and your safety and life matter immensely. You don't have to carry this pain alone.

Please connect with compassionate, immediate help right now:
• Tele-MANAS (India): 14416 or 1800-89-14416 (24/7 Free)
• Emergency: 112
• US & Canada Crisis Line: 988

Please move to a safe space and reach out to someone you trust or a professional immediately.
            """.trimIndent()
        }

        repository.addMessage(uid, session.id, "crisis_alert", crisisMessage, modality)

        if (_isVoiceMode.value && isUsableFriendVoiceId(_userProfile.value.customVoiceId)) {
            voiceManager.speak(crisisMessage, _userProfile.value.customVoiceId, _userProfile.value.preferredLanguage)
        }
    }

    fun startVoiceTurn() {
        voiceManager.stopAudioPlayback()
        _orbState.value = OrbState.LISTENING
        _statusMessage.value = "Listening to you... Speak naturally"

        voiceManager.startListening(
            language = _userProfile.value.preferredLanguage,
            onFinalResult = { transcribed ->
                _orbState.value = OrbState.PROCESSING
                onUserSendMessage(transcribed, "voice")
            },
            onError = { errMsg ->
                _orbState.value = OrbState.IDLE
                _statusMessage.value = errMsg
            }
        )
    }

    fun stopVoiceTurnManually() {
        voiceManager.stopListeningManually()
    }

    fun interruptPlayback() {
        voiceManager.stopAudioPlayback()
        voiceManager.cancelListening()
        _orbState.value = OrbState.INTERRUPTED
        viewModelScope.launch {
            kotlinx.coroutines.delay(600)
            _orbState.value = OrbState.IDLE
        }
    }

    fun replayLastAssistantAudio() {
        val lastMsg = _messages.value.lastOrNull { it.role == "assistant" || it.role == "crisis_alert" }
        if (lastMsg != null) {
            voiceManager.speak(lastMsg.text, _userProfile.value.customVoiceId, _userProfile.value.preferredLanguage)
        }
    }

    // --- Support Effectiveness Feedback ---

    fun rateInteraction(rating: String) {
        val msgId = _lastAssistantMessageId.value ?: return
        val sess = _activeSession.value ?: return
        val uid = _currentUser.value?.uid ?: "guest_user"

        viewModelScope.launch {
            repository.rateMessage(uid, sess.id, msgId, rating)
            _showFeedbackPrompt.value = false

            if (rating == "change_approach") {
                val adjustment = "Thank you for telling me. I will adjust right away. Would you prefer I simply listen without giving suggestions, or would you like to unpack a specific feeling together?"
                repository.addMessage(uid, sess.id, "assistant", adjustment, if (_isVoiceMode.value) "voice" else "text")
                if (_isVoiceMode.value && isUsableFriendVoiceId(_userProfile.value.customVoiceId)) {
                    voiceManager.speak(adjustment, _userProfile.value.customVoiceId, _userProfile.value.preferredLanguage)
                }
            } else if (rating == "not_yet") {
                val gentle = "I understand. Take all the time you need; there's no rush to fix anything today."
                repository.addMessage(uid, sess.id, "assistant", gentle, if (_isVoiceMode.value) "voice" else "text")
            }
        }
    }

    // --- Search Grounding for Crisis Resources ---

    fun searchCrisisHelplines(query: String) {
        _isSearchingGroundedCrisis.value = true
        _groundedCrisisResults.value = null
        viewModelScope.launch {
            val result = GeminiService.searchGroundedCrisisResources(query)
            _isSearchingGroundedCrisis.value = false
            _groundedCrisisResults.value = result.getOrNull() ?: "Unable to fetch live crisis grounding information at this time."
        }
    }

    // --- Session History Management ---

    fun renameSession(sessionId: String, newTitle: String) {
        val uid = _currentUser.value?.uid ?: "guest_user"
        viewModelScope.launch {
            repository.renameSession(uid, sessionId, newTitle)
        }
    }

    fun deleteSession(sessionId: String) {
        val uid = _currentUser.value?.uid ?: "guest_user"
        viewModelScope.launch {
            repository.deleteSession(uid, sessionId)
            if (_activeSession.value?.id == sessionId) {
                _activeSession.value = null
                _messages.value = emptyList()
            }
        }
    }

    fun clearAllHistory() {
        val uid = _currentUser.value?.uid ?: "guest_user"
        viewModelScope.launch {
            repository.clearAllSessions(uid)
            _activeSession.value = null
            _messages.value = emptyList()
            startNewSession("New Conversation")
        }
    }

    fun getExportableTranscript(): String {
        val sess = _activeSession.value ?: return ""
        val builder = StringBuilder()
        builder.appendLine("# Bloom Conversation Transcript: ${sess.title}")
        builder.appendLine("Date: ${sess.createdAt?.toDate() ?: "Recent"}")
        builder.appendLine("Voice: ${sess.voicePreference} | Language: ${sess.language}")
        builder.appendLine()
        _messages.value.forEach { msg ->
            val speaker = if (msg.role == "user") "You" else "Bloom"
            builder.appendLine("[$speaker (${msg.modality})]: ${msg.text}")
            builder.appendLine()
        }
        return builder.toString()
    }

    // --- Authentication (Google Sign-In via Credential Manager) ---

    fun signInWithGoogle(
        context: Context,
        activity: Activity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val credentialManager = CredentialManager.create(context)
        // google-services.json is intentionally not committed. Resolve the generated
        // Firebase web client ID dynamically so builds without private Firebase config
        // still compile and can use guest mode.
        val clientIdResource = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        if (clientIdResource == 0) {
            onError("Google Sign-In is not configured for this build. Add your Firebase google-services.json, or continue in guest mode.")
            return
        }
        val clientId = context.getString(clientIdResource)
        if (clientId.isBlank()) {
            onError("Google Sign-In client ID is blank. Check your Firebase configuration.")
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

        viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    _currentUser.value = authResult.user
                    _isGuestMode.value = false
                    onSuccess()
                } else {
                    onError("Unexpected credential type")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w("Auth", "Google Sign-In cancelled: ${e.message}", e)
                onError("Sign-in was cancelled.")
            } catch (e: Exception) {
                Log.e("Auth", "Google Sign-In failed", e)
                onError(e.localizedMessage ?: "Sign-in failed.")
            }
        }
    }

    fun signOutUser(context: Context) {
        auth.signOut()
        _currentUser.value = null
        _isGuestMode.value = true
        val credentialManager = CredentialManager.create(context)
        viewModelScope.launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e("Auth", "Error clearing credential state", e)
            }
            loadUserAndSessions()
        }
    }

    fun updatePreferences(voice: String, language: String, cloudHistory: Boolean) {
        val selectedVoice = if (voice == "Friend") "Friend" else "None"
        val updated = _userProfile.value.copy(
            voicePreference = selectedVoice,
            preferredLanguage = language,
            cloudHistoryEnabled = cloudHistory
        )
        _userProfile.value = updated
        _isVoiceMode.value = selectedVoice == "Friend" && isUsableFriendVoiceId(updated.customVoiceId)
        if (selectedVoice == "None") {
            voiceManager.stopAudioPlayback()
        } else if (!_isVoiceMode.value) {
            _statusMessage.value = "Add a valid Gemini replicated voice ID in Settings before enabling spoken replies."
        }
        viewModelScope.launch {
            repository.saveUserProfile(updated)
        }
    }

    fun updateCustomVoiceId(value: String): String {
        val voiceId = value.trim()
        if (!isUsableFriendVoiceId(voiceId)) {
            return "Invalid voice ID. Use the voice_… or voicekey_… ID returned by Google AI Studio Voice Replication."
        }
        val updated = _userProfile.value.copy(customVoiceId = voiceId, voicePreference = "Friend")
        _userProfile.value = updated
        _isVoiceMode.value = true
        _statusMessage.value = "Friend's voice ID saved. Bloom will only use this voice."
        viewModelScope.launch {
            val result = repository.saveUserProfile(updated)
            if (result.isFailure) {
                _statusMessage.value = "Voice ID is active for this session, but profile save failed. Sign in and check Firebase to persist it."
            }
        }
        return "Friend's voice ID saved. No generic/system voice will be used."
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
    }
}

class AuraViewModelFactory(
    private val repository: AuraRepository,
    private val voiceManager: VoiceManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AuraViewModel(repository, voiceManager) as T
    }
}
