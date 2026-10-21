package com.example.audio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.remote.GeminiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class VoiceManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var ttsEngine: TextToSpeech? = null
    private var isTtsInitialized = false
    private var mediaPlayer: MediaPlayer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isPlayingAudio = MutableStateFlow(false)
    val isPlayingAudio: StateFlow<Boolean> = _isPlayingAudio.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private var onSpeechFinalized: ((String) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null

    init {
        initTts()
    }

    private fun initTts() {
        ttsEngine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
                ttsEngine?.language = Locale.ENGLISH
                ttsEngine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isPlayingAudio.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isPlayingAudio.value = false
                    }

                    override fun onError(utteranceId: String?) {
                        _isPlayingAudio.value = false
                    }
                })
            }
        }
    }

    fun startListening(
        language: String = "en",
        onFinalResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        // Immediate interruption: stop any ongoing audio playback first
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
                    Log.w("VoiceManager", "Speech error: $errorMsg")
                    // If no match was found but user entered partial text, preserve it
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
                    if (text.isNotBlank()) {
                        _partialTranscript.value = text
                    }
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
            onError("Unable to initialize speech recognizer: ${e.message}")
        }
    }

    fun stopListeningManually() {
        if (_isListening.value) {
            speechRecognizer?.stopListening()
            _isListening.value = false
        }
    }

    fun cancelListening() {
        speechRecognizer?.cancel()
        _isListening.value = false
        _partialTranscript.value = ""
    }

    /**
     * Speaks the assistant text using Gemini TTS (model gemini-3.8-flash-tts),
     * with transparent fallback to Android on-device TextToSpeech.
     */
    fun speak(
        text: String,
        voiceName: String = "Aoede",
        language: String = "en"
    ) {
        // Immediate interruption: stop existing playback first
        stopAudioPlayback()

        if (text.isBlank()) return

        coroutineScope.launch {
            // First attempt high-fidelity Gemini TTS
            val ttsResult = GeminiService.generateTTSAudio(text, voiceName)
            if (ttsResult.isSuccess) {
                val audioBytes = ttsResult.getOrNull()
                if (audioBytes != null && audioBytes.isNotEmpty()) {
                    playAudioBytes(audioBytes)
                    return@launch
                }
            }

            // Fallback to local Android TextToSpeech engine
            withContext(Dispatchers.Main) {
                if (isTtsInitialized && ttsEngine != null) {
                    val targetLocale = when (language.lowercase()) {
                        "ko" -> Locale.KOREAN
                        "hi" -> Locale.forLanguageTag("hi-IN")
                        "hinglish" -> Locale.forLanguageTag("en-IN")
                        else -> Locale.ENGLISH
                    }
                    ttsEngine?.language = targetLocale
                    // Slight pitch and rate adjustment for warmth
                    if (voiceName == "Puck" || voiceName == "Fenrir") {
                        ttsEngine?.setPitch(0.85f)
                    } else {
                        ttsEngine?.setPitch(1.05f)
                    }
                    ttsEngine?.setSpeechRate(0.95f)

                    _isPlayingAudio.value = true
                    ttsEngine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "aura_utterance_${System.currentTimeMillis()}")
                }
            }
        }
    }

    private fun playAudioBytes(bytes: ByteArray) {
        try {
            stopAudioPlayback()
            val tempFile = File.createTempFile("aura_tts_", ".mp3", context.cacheDir)
            FileOutputStream(tempFile).use { it.write(bytes) }

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .build()
                )
                setDataSource(tempFile.absolutePath)
                setOnPreparedListener {
                    _isPlayingAudio.value = true
                    start()
                }
                setOnCompletionListener {
                    _isPlayingAudio.value = false
                    tempFile.delete()
                    release()
                    mediaPlayer = null
                }
                setOnErrorListener { _, _, _ ->
                    _isPlayingAudio.value = false
                    tempFile.delete()
                    release()
                    mediaPlayer = null
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error playing audio bytes", e)
            _isPlayingAudio.value = false
        }
    }

    /**
     * Immediately stops playback of any speech audio and discards queues.
     */
    fun stopAudioPlayback() {
        try {
            if (_isPlayingAudio.value || mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
            }
            if (ttsEngine?.isSpeaking == true) {
                ttsEngine?.stop()
            }
        } catch (e: Exception) {
            Log.w("VoiceManager", "Error during stopAudioPlayback", e)
        } finally {
            _isPlayingAudio.value = false
        }
    }

    fun release() {
        stopAudioPlayback()
        cancelListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        ttsEngine?.shutdown()
        ttsEngine = null
    }
}
