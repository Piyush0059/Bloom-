package com.example.audio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.data.remote.GeminiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class VoiceManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var mediaPlayer: MediaPlayer? = null
    private var playbackTempFile: File? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isPlayingAudio = MutableStateFlow(false)
    val isPlayingAudio: StateFlow<Boolean> = _isPlayingAudio.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _voiceError = MutableStateFlow<String?>(null)
    val voiceError: StateFlow<String?> = _voiceError.asStateFlow()

    private var onSpeechFinalized: ((String) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null

    fun startListening(
        language: String = "en",
        onFinalResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        stopAudioPlayback()

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition is not available on this device.")
            return
        }

        onSpeechFinalized = onFinalResult
        onErrorCallback = onError
        _partialTranscript.value = ""

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {
                    _rmsLevel.value = (rmsdB / 10f).coerceIn(0f, 1f)
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _isListening.value = false
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    val errorMsg = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                        SpeechRecognizer.ERROR_NETWORK -> "Network connection issue"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timed out"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                        SpeechRecognizer.ERROR_SERVER -> "Recognition server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                        else -> "Speech recognition error ($error)"
                    }
                    Log.w("VoiceManager", "Speech recognition error: " + errorMsg)
                    if (error == SpeechRecognizer.ERROR_NO_MATCH && _partialTranscript.value.isNotBlank()) {
                        onSpeechFinalized?.invoke(_partialTranscript.value)
                    } else {
                        onErrorCallback?.invoke(errorMsg)
                    }
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val bestText = matches?.firstOrNull() ?: _partialTranscript.value
                    if (bestText.isNotBlank()) {
                        onSpeechFinalized?.invoke(bestText)
                    } else {
                        onErrorCallback?.invoke("No words recognized.")
                    }
                    _partialTranscript.value = ""
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    if (text.isNotBlank()) _partialTranscript.value = text
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                val targetLang = when (language.lowercase()) {
                    "ko" -> "ko-KR"
                    "hi" -> "hi-IN"
                    "hinglish" -> "en-IN"
                    else -> "en-US"
                }
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, targetLang)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _isListening.value = false
            onError("Unable to initialize speech recognizer: " + (e.message ?: "unknown error"))
        }
    }

    fun stopListeningManually() {
        if (_isListening.value) {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {}
            _isListening.value = false
        }
    }

    fun cancelListening() {
        try {
            speechRecognizer?.cancel()
        } catch (_: Exception) {}
        _isListening.value = false
        _partialTranscript.value = ""
    }

    /**
     * Speak only through the configured, consented Gemini replicated voice.
     * There is deliberately no Android/system TextToSpeech fallback.
     */
    fun speak(
        text: String,
        customVoiceId: String,
        language: String = "en"
    ) {
        stopAudioPlayback()
        _voiceError.value = null
        if (text.isBlank()) return

        val voiceId = customVoiceId.trim()
        if (!isReplicatedVoiceId(voiceId)) {
            _voiceError.value = "Friend's voice is not configured. Add a valid Gemini replicated voice ID in Settings. No fallback voice will be played."
            return
        }

        coroutineScope.launch {
            val result = GeminiService.generateTTSAudio(text, voiceId, language)
            val audioBytes = result.getOrNull()
            if (result.isSuccess && audioBytes != null && audioBytes.isNotEmpty()) {
                _voiceError.value = null
                playAudioBytes(audioBytes)
            } else {
                _isPlayingAudio.value = false
                _voiceError.value = result.exceptionOrNull()?.localizedMessage
                    ?: "Friend's voice could not be generated. Check the replica ID, Gemini API key, and quota."
                Log.w("VoiceManager", "Gemini replicated voice synthesis failed.")
            }
        }
    }

    private fun isReplicatedVoiceId(value: String): Boolean =
        Regex("^(voice_[A-Za-z0-9_-]{6,}|voicekey_[A-Za-z0-9_-]{6,})$").matches(value)

    private fun pcmToWavIfNeeded(bytes: ByteArray): ByteArray {
        val isWav = bytes.size >= 12 &&
            String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF" &&
            String(bytes, 8, 4, Charsets.US_ASCII) == "WAVE"
        if (isWav) return bytes

        // Gemini GenerateContent TTS returns 24 kHz, mono, 16-bit PCM. Wrap
        // those samples so Android MediaPlayer can decode them as WAV.
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt(36 + bytes.size)
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16)
        header.putShort(1.toShort())
        header.putShort(1.toShort())
        header.putInt(24_000)
        header.putInt(48_000)
        header.putShort(2.toShort())
        header.putShort(16.toShort())
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(bytes.size)
        return header.array() + bytes
    }

    private fun playAudioBytes(bytes: ByteArray) {
        stopAudioPlayback()
        var tempFile: File? = null
        var player: MediaPlayer? = null
        try {
            val audioFile = File.createTempFile("bloom_friend_voice_", ".wav", context.cacheDir)
            tempFile = audioFile
            FileOutputStream(audioFile).use { it.write(pcmToWavIfNeeded(bytes)) }
            playbackTempFile = audioFile

            val createdPlayer = MediaPlayer()
            player = createdPlayer
            mediaPlayer = createdPlayer
            createdPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .build()
            )
            createdPlayer.setDataSource(audioFile.absolutePath)
            createdPlayer.setOnPreparedListener {
                if (mediaPlayer !== createdPlayer) {
                    try { createdPlayer.release() } catch (_: Exception) {}
                    audioFile.delete()
                } else {
                    _isPlayingAudio.value = true
                    createdPlayer.start()
                }
            }
            createdPlayer.setOnCompletionListener { finishPlayback(createdPlayer, audioFile) }
            createdPlayer.setOnErrorListener { _, what, extra ->
                Log.w("VoiceManager", "Friend voice playback failed (what=" + what + ", extra=" + extra + ").")
                finishPlayback(createdPlayer, audioFile)
                true
            }
            createdPlayer.prepareAsync()
        } catch (e: Exception) {
            try { player?.release() } catch (_: Exception) {}
            if (mediaPlayer === player) mediaPlayer = null
            if (playbackTempFile === tempFile) playbackTempFile = null
            tempFile?.delete()
            _isPlayingAudio.value = false
            _voiceError.value = "Friend's voice audio could not be played on this device."
            Log.w("VoiceManager", "Error preparing friend voice audio: " + e.javaClass.simpleName)
        }
    }

    private fun finishPlayback(player: MediaPlayer, file: File) {
        if (mediaPlayer === player) {
            mediaPlayer = null
            if (playbackTempFile === file) playbackTempFile = null
            _isPlayingAudio.value = false
            try { player.release() } catch (_: Exception) {}
        } else {
            // The user may have interrupted playback while an async callback
            // was pending. Never revive the old player or overwrite current state.
            try { player.release() } catch (_: Exception) {}
        }
        file.delete()
    }

    /** Immediately stops playback, even while MediaPlayer is still preparing. */
    fun stopAudioPlayback() {
        val player = mediaPlayer
        val tempFile = playbackTempFile
        mediaPlayer = null
        playbackTempFile = null
        try {
            if (player != null) {
                try {
                    if (player.isPlaying) player.stop()
                } catch (_: Exception) {}
                try { player.reset() } catch (_: Exception) {}
                try { player.release() } catch (_: Exception) {}
            }
        } finally {
            tempFile?.delete()
            _isPlayingAudio.value = false
        }
    }

    fun release() {
        stopAudioPlayback()
        cancelListening()
        try { speechRecognizer?.destroy() } catch (_: Exception) {}
        speechRecognizer = null
    }
}
