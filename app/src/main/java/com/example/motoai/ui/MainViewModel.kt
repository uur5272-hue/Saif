package com.example.motoai.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.motoai.action.ActionDispatcher
import com.example.motoai.action.ActionResult
import com.example.motoai.data.model.MotoAction
import com.example.motoai.data.security.SecurePreferences
import com.example.motoai.data.service.GeminiResult
import com.example.motoai.data.service.GeminiService
import com.example.motoai.media.image.GeminiImageProvider
import com.example.motoai.media.image.HighQualityWebImageProvider
import com.example.motoai.media.image.ImageGenerationProvider
import com.example.motoai.media.image.ImageGenerationResult
import com.example.motoai.media.video.CustomApiVideoProvider
import com.example.motoai.media.video.VeoVideoProvider
import com.example.motoai.media.video.VideoGenerationProvider
import com.example.motoai.media.video.VideoGenerationResult
import com.example.motoai.voice.MicState
import com.example.motoai.voice.VoiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppScreen {
    HOME,
    IMAGE_GEN,
    VIDEO_GEN,
    SETTINGS
}

data class CommandHistoryItem(
    val id: String = System.currentTimeMillis().toString() + "_" + (1..1000).random(),
    val query: String,
    val response: String,
    val intent: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class GeneratedImageItem(
    val id: String = System.currentTimeMillis().toString(),
    val bitmap: Bitmap,
    val prompt: String,
    val filePath: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class GeneratedVideoItem(
    val id: String = System.currentTimeMillis().toString(),
    val videoUrl: String,
    val prompt: String,
    val resolution: String,
    val hasAudio: Boolean,
    val durationSeconds: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class ConfirmationDialogState(
    val isVisible: Boolean = false,
    val title: String = "",
    val message: String = "",
    val onConfirm: () -> Unit = {},
    val onDismiss: () -> Unit = {}
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val securePrefs = SecurePreferences(application)
    private val actionDispatcher = ActionDispatcher(application)
    private val geminiService = GeminiService { securePrefs.getGeminiApiKey() }

    // Navigation state
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Query & Response State
    private val _queryInput = MutableStateFlow("")
    val queryInput: StateFlow<String> = _queryInput.asStateFlow()

    private val _latestResponse = MutableStateFlow("Tap the microphone or say 'MOTO' to command apps, search, generate art, or ask questions.")
    val latestResponse: StateFlow<String> = _latestResponse.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _commandHistory = MutableStateFlow<List<CommandHistoryItem>>(
        listOf(
            CommandHistoryItem(query = "Open YouTube", response = "Opened YouTube", intent = "OPEN_APP"),
            CommandHistoryItem(query = "Search Free Fire", response = "Searching web for Free Fire", intent = "WEB_SEARCH"),
            CommandHistoryItem(query = "Create image of cyber robot", response = "Rendered neural image", intent = "GENERATE_IMAGE")
        )
    )
    val commandHistory: StateFlow<List<CommandHistoryItem>> = _commandHistory.asStateFlow()

    // Voice Manager
    lateinit var voiceManager: VoiceManager
    val micState: StateFlow<MicState> get() = voiceManager.micState
    val soundLevel: StateFlow<Float> get() = voiceManager.soundLevel
    val isSpeaking: StateFlow<Boolean> get() = voiceManager.isSpeaking

    // Confirmation Dialog
    private val _confirmationState = MutableStateFlow(ConfirmationDialogState())
    val confirmationState: StateFlow<ConfirmationDialogState> = _confirmationState.asStateFlow()

    // Image Generation State
    private val geminiImageProvider = GeminiImageProvider { securePrefs.getGeminiApiKey() }
    private val webImageProvider = HighQualityWebImageProvider()

    private val _isImageGenerating = MutableStateFlow(false)
    val isImageGenerating: StateFlow<Boolean> = _isImageGenerating.asStateFlow()

    private val _imageHistory = MutableStateFlow<List<GeneratedImageItem>>(emptyList())
    val imageHistory: StateFlow<List<GeneratedImageItem>> = _imageHistory.asStateFlow()

    private val _imagePrompt = MutableStateFlow("A futuristic robot AI assistant with glowing cyan eyes in a neon cyberpunk metropolis")
    val imagePrompt: StateFlow<String> = _imagePrompt.asStateFlow()

    // Video Generation State
    private val veoVideoProvider = VeoVideoProvider { securePrefs.getGeminiApiKey() }
    private val customVideoProvider = CustomApiVideoProvider()

    private val _isVideoGenerating = MutableStateFlow(false)
    val isVideoGenerating: StateFlow<Boolean> = _isVideoGenerating.asStateFlow()

    private val _videoProgress = MutableStateFlow(0)
    val videoProgress: StateFlow<Int> = _videoProgress.asStateFlow()

    private val _videoStatusText = MutableStateFlow("")
    val videoStatusText: StateFlow<String> = _videoStatusText.asStateFlow()

    private val _videoHistory = MutableStateFlow<List<GeneratedVideoItem>>(emptyList())
    val videoHistory: StateFlow<List<GeneratedVideoItem>> = _videoHistory.asStateFlow()

    private val _videoPrompt = MutableStateFlow("A cinematic video of a futuristic robot walking through a neon cyber city at night, 4K")
    val videoPrompt: StateFlow<String> = _videoPrompt.asStateFlow()

    init {
        voiceManager = VoiceManager(application) { spokenText ->
            handleSpokenText(spokenText)
        }
        applyVoiceSettings()
    }

    fun applyVoiceSettings() {
        voiceManager.setLanguage(securePrefs.getVoiceLanguage())
        voiceManager.setPitchAndSpeed(securePrefs.getVoicePitch(), securePrefs.getVoiceSpeed())
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setQueryInput(text: String) {
        _queryInput.value = text
    }

    fun setImagePrompt(text: String) {
        _imagePrompt.value = text
    }

    fun setVideoPrompt(text: String) {
        _videoPrompt.value = text
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun dismissConfirmation() {
        _confirmationState.value = ConfirmationDialogState(isVisible = false)
    }

    fun startListening() {
        _errorMessage.value = null
        voiceManager.startListening(securePrefs.getVoiceLanguage())
    }

    fun stopListening() {
        voiceManager.stopListening()
    }

    fun cancelVoice() {
        voiceManager.cancelVoice()
    }

    private fun handleSpokenText(spoken: String) {
        _queryInput.value = spoken
        processUserCommand(spoken)
    }

    fun submitCurrentTextQuery() {
        val q = _queryInput.value.trim()
        if (q.isNotEmpty()) {
            processUserCommand(q)
            _queryInput.value = ""
        }
    }

    fun processUserCommand(rawCommand: String) {
        if (rawCommand.isBlank()) return

        _isAiThinking.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            when (val result = geminiService.processCommand(rawCommand)) {
                is GeminiResult.Success -> {
                    _isAiThinking.value = false
                    executeMotoAction(result.data, rawCommand)
                }
                is GeminiResult.Error -> {
                    _isAiThinking.value = false
                    _errorMessage.value = result.message
                    _latestResponse.value = result.message
                    voiceManager.speak("Attention: ${result.message}")
                }
            }
        }
    }

    private fun executeMotoAction(action: MotoAction, originalQuery: String) {
        // Handle shortcuts to Image / Video screens
        when (action.intent) {
            "GENERATE_IMAGE" -> {
                val prompt = action.target?.ifEmpty { _imagePrompt.value } ?: _imagePrompt.value
                _imagePrompt.value = prompt
                _latestResponse.value = "Preparing visual generator for: $prompt"
                voiceManager.speak(action.spokenResponse.ifEmpty { "Opening image generator" })
                navigateTo(AppScreen.IMAGE_GEN)
                generateImage(prompt)
                addToHistory(originalQuery, "Generated image: $prompt", action.intent)
                return
            }
            "GENERATE_VIDEO" -> {
                val prompt = action.target?.ifEmpty { _videoPrompt.value } ?: _videoPrompt.value
                _videoPrompt.value = prompt
                _latestResponse.value = "Preparing video synthesizer for: $prompt"
                voiceManager.speak(action.spokenResponse.ifEmpty { "Opening video creator" })
                navigateTo(AppScreen.VIDEO_GEN)
                generateVideo(prompt)
                addToHistory(originalQuery, "Generated video: $prompt", action.intent)
                return
            }
        }

        // Check if sensitive action requires confirmation dialog
        if (action.requiresConfirmation && securePrefs.isConfirmSensitiveActions()) {
            _confirmationState.value = ConfirmationDialogState(
                isVisible = true,
                title = "Security Confirmation",
                message = action.confirmationMessage ?: "MOTO wants to execute ${action.intent} for ${action.target}. Proceed?",
                onConfirm = {
                    dismissConfirmation()
                    dispatchAction(action, originalQuery, confirmed = true)
                },
                onDismiss = {
                    dismissConfirmation()
                    _latestResponse.value = "Action cancelled by user."
                    voiceManager.speak("Action cancelled.")
                }
            )
            voiceManager.speak(action.spokenResponse.ifEmpty { "Please confirm action on screen." })
            return
        }

        dispatchAction(action, originalQuery, confirmed = false)
    }

    private fun dispatchAction(action: MotoAction, originalQuery: String, confirmed: Boolean) {
        val result = actionDispatcher.execute(action, confirmed = confirmed)
        when (result) {
            is ActionResult.Success -> {
                val text = if (action.displayText.isNotBlank()) action.displayText else result.message
                _latestResponse.value = text
                val speech = if (action.spokenResponse.isNotBlank()) action.spokenResponse else text
                voiceManager.speak(speech)
                addToHistory(originalQuery, text, action.intent)
            }
            is ActionResult.Failure -> {
                val err = result.reason
                _errorMessage.value = err
                _latestResponse.value = err
                voiceManager.speak("Could not complete action: $err")
                addToHistory(originalQuery, "Failed: $err", action.intent)
            }
            is ActionResult.NeedsConfirmation -> {
                _confirmationState.value = ConfirmationDialogState(
                    isVisible = true,
                    title = "Confirm Action",
                    message = result.message,
                    onConfirm = {
                        dismissConfirmation()
                        val res = result.onConfirm()
                        if (res is ActionResult.Success) {
                            _latestResponse.value = res.message
                            voiceManager.speak(res.message)
                        }
                    },
                    onDismiss = {
                        dismissConfirmation()
                    }
                )
            }
        }
    }

    fun generateImage(prompt: String) {
        if (_isImageGenerating.value) return
        _isImageGenerating.value = true
        _errorMessage.value = null

        val provider: ImageGenerationProvider = if (securePrefs.getImageProvider() == "gemini") {
            geminiImageProvider
        } else {
            webImageProvider
        }

        viewModelScope.launch {
            val result = provider.generateImage(prompt, getApplication())
            _isImageGenerating.value = false

            when (result) {
                is ImageGenerationResult.Success -> {
                    val newItem = GeneratedImageItem(
                        bitmap = result.bitmap,
                        prompt = result.prompt,
                        filePath = result.filePath
                    )
                    _imageHistory.value = listOf(newItem) + _imageHistory.value
                    voiceManager.speak("Image generated successfully.")
                }
                is ImageGenerationResult.Error -> {
                    // Try fallback provider if primary failed
                    if (provider is GeminiImageProvider) {
                        val fallbackRes = webImageProvider.generateImage(prompt, getApplication())
                        if (fallbackRes is ImageGenerationResult.Success) {
                            val newItem = GeneratedImageItem(
                                bitmap = fallbackRes.bitmap,
                                prompt = fallbackRes.prompt,
                                filePath = fallbackRes.filePath
                            )
                            _imageHistory.value = listOf(newItem) + _imageHistory.value
                            voiceManager.speak("Rendered with neural diffusion.")
                            return@launch
                        }
                    }
                    _errorMessage.value = result.message
                    voiceManager.speak("Image generation error.")
                }
            }
        }
    }

    fun generateVideo(prompt: String, includeAudio: Boolean = true) {
        if (_isVideoGenerating.value) return
        _isVideoGenerating.value = true
        _videoProgress.value = 0
        _videoStatusText.value = "Initializing generation pipeline..."
        _errorMessage.value = null

        val provider: VideoGenerationProvider = if (securePrefs.getVideoProvider() == "veo") {
            veoVideoProvider
        } else {
            customVideoProvider
        }

        viewModelScope.launch {
            val result = provider.generateVideo(
                prompt = prompt,
                imageInputPath = null,
                includeAudio = includeAudio,
                context = getApplication()
            ) { pct, status ->
                _videoProgress.value = pct
                _videoStatusText.value = status
            }

            _isVideoGenerating.value = false

            when (result) {
                is VideoGenerationResult.Success -> {
                    val item = GeneratedVideoItem(
                        videoUrl = result.videoUrl,
                        prompt = result.prompt,
                        resolution = result.resolution,
                        hasAudio = result.hasAudio,
                        durationSeconds = result.durationSeconds
                    )
                    _videoHistory.value = listOf(item) + _videoHistory.value
                    voiceManager.speak("Video render completed.")
                }
                is VideoGenerationResult.Error -> {
                    _errorMessage.value = result.message
                    voiceManager.speak("Video generation failed.")
                }
                is VideoGenerationResult.Progress -> {}
            }
        }
    }

    fun clearLocalHistory() {
        _commandHistory.value = emptyList()
        _imageHistory.value = emptyList()
        _videoHistory.value = emptyList()
        _latestResponse.value = "MOTO AI memory reset. Ready for new instructions."
    }

    private fun addToHistory(query: String, response: String, intent: String) {
        val item = CommandHistoryItem(
            query = query,
            response = response,
            intent = intent
        )
        _commandHistory.value = (listOf(item) + _commandHistory.value).take(20)
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}
