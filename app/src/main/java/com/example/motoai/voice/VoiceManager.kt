package com.example.motoai.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class MicState {
    IDLE,
    LISTENING,
    PROCESSING,
    ERROR
}

class VoiceManager(
    private val context: Context,
    private val onResult: (String) -> Unit
) : RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    private val _micState = MutableStateFlow(MicState.IDLE)
    val micState: StateFlow<MicState> = _micState.asStateFlow()

    private val _soundLevel = MutableStateFlow(0f)
    val soundLevel: StateFlow<Float> = _soundLevel.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        initTts()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
                textToSpeech?.language = Locale.US
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })
            }
        }
    }

    fun setLanguage(langCode: String) {
        val locale = when (langCode) {
            "hi-IN" -> Locale("hi", "IN")
            "ur-PK" -> Locale("ur", "PK")
            "en-IN" -> Locale("en", "IN")
            "en-GB" -> Locale.UK
            else -> Locale.US
        }
        if (isTtsInitialized) {
            textToSpeech?.language = locale
        }
    }

    fun setPitchAndSpeed(pitch: Float, speed: Float) {
        if (isTtsInitialized) {
            textToSpeech?.setPitch(pitch)
            textToSpeech?.setSpeechRate(speed)
        }
    }

    fun speak(text: String, utteranceId: String = "moto_tts") {
        if (!isTtsInitialized || text.isBlank()) return
        textToSpeech?.stop()
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stopSpeaking() {
        if (isTtsInitialized) {
            textToSpeech?.stop()
            _isSpeaking.value = false
        }
    }

    fun startListening(languageTag: String = "en-US") {
        stopSpeaking()
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _micState.value = MicState.ERROR
            return
        }

        stopListening()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(this@VoiceManager)
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "ur-PK", "en-IN", "en-US"))
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        _micState.value = MicState.LISTENING
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _micState.value = MicState.ERROR
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // ignore
        }
        speechRecognizer = null
        _micState.value = MicState.IDLE
        _soundLevel.value = 0f
    }

    fun cancelVoice() {
        stopListening()
        stopSpeaking()
    }

    // RecognitionListener Callbacks
    override fun onReadyForSpeech(params: Bundle?) {
        _micState.value = MicState.LISTENING
    }

    override fun onBeginningOfSpeech() {
        _micState.value = MicState.LISTENING
    }

    override fun onRmsChanged(rmsdB: Float) {
        // Convert to 0f..1f range for animation
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        _soundLevel.value = normalized
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _micState.value = MicState.PROCESSING
        _soundLevel.value = 0f
    }

    override fun onError(error: Int) {
        _micState.value = MicState.ERROR
        _soundLevel.value = 0f
    }

    override fun onResults(results: Bundle?) {
        _micState.value = MicState.IDLE
        _soundLevel.value = 0f
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val best = matches?.firstOrNull() ?: ""
        if (best.isNotBlank()) {
            onResult(best)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        // Can be used for live transcription if desired
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun destroy() {
        cancelVoice()
        try {
            textToSpeech?.shutdown()
        } catch (e: Exception) {
            // ignore
        }
    }
}
