package com.example.motoai.data.service

import com.example.motoai.data.api.GeminiApiClient
import com.example.motoai.data.model.GeminiContent
import com.example.motoai.data.model.GeminiGenerationConfig
import com.example.motoai.data.model.GeminiPart
import com.example.motoai.data.model.GeminiRequest
import com.example.motoai.data.model.MotoAction
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

sealed class GeminiResult<out T> {
    data class Success<out T>(val data: T) : GeminiResult<T>()
    data class Error(val message: String, val isKeyError: Boolean = false, val isQuotaError: Boolean = false) : GeminiResult<Nothing>()
}

class GeminiService(private val getApiKey: () -> String) {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val actionAdapter = moshi.adapter(MotoAction::class.java)

    // Recommended model from skill: gemini-3.5-flash
    private val textModel = "gemini-3.5-flash"

    private val systemInstructionText = """
        You are MOTO AI, a powerful, futuristic Android AI assistant and smart device commander.
        Tagline: "Your Voice. Your AI. Your MOTO."
        
        Your job is to understand user voice or text requests (in English, Hindi, Urdu, or mixed Hinglish) and output a clean JSON object representing the action to execute.
        
        You MUST respond ONLY with a raw valid JSON object (no markdown quotes, no triple backticks, no prefix or suffix).
        JSON Schema:
        {
          "intent": "<OPEN_APP | WEB_SEARCH | OPEN_URL | OPEN_SETTINGS | MAKE_CALL | SEND_SMS | CREATE_REMINDER | START_NAVIGATION | PLAY_STORE_SEARCH | GENERATE_IMAGE | GENERATE_VIDEO | CONVERSE>",
          "target": "<App name, contact name, url, query, or prompt>",
          "param": "<Additional parameter like message body or setting type>",
          "requires_confirmation": <true if calling/deleting/sending, false otherwise>,
          "confirmation_message": "<Prompt asking user before doing sensitive action, or empty>",
          "spoken_response": "<Concise natural voice response to speak aloud to user>",
          "display_text": "<Clear futuristic visual text response>"
        }
        
        Intents to recognize:
        1. "OPEN_APP": when user says "open YouTube", "open Chrome", "open Free Fire", "launch WhatsApp". target = app name.
        2. "WEB_SEARCH": when user asks to search for something ("search for Free Fire", "search latest cricket score"). target = query.
        3. "OPEN_URL": when user provides or asks for a specific website URL.
        4. "OPEN_SETTINGS": when user wants to open Android settings ("open wifi settings", "open bluetooth", "open display settings").
        5. "MAKE_CALL": when user wants to call someone ("call Rahul", "call 9876543210"). target = contact name or number. requires_confirmation = true. confirmation_message = "MOTO wants to call <target>. Continue?".
        6. "SEND_SMS": when user wants to message someone ("send message to Mom I will be late"). target = contact, param = message. requires_confirmation = true.
        7. "CREATE_REMINDER": when user wants to set a reminder or alarm. target = title.
        8. "START_NAVIGATION": when user asks for directions or navigation ("navigate to Taj Mahal"). target = destination.
        9. "PLAY_STORE_SEARCH": when user explicitly wants to download or install or view an app in Play Store.
        10. "GENERATE_IMAGE": when user asks to create/generate an image or picture ("generate an image of a futuristic city at night"). target = image prompt.
        11. "GENERATE_VIDEO": when user asks to create/generate a video ("create a video of a neon cyber robot"). target = video prompt.
        12. "CONVERSE": general knowledge, questions, weather, chit-chat, calculations, jokes, assistant info. spoken_response = clear concise answer (1-2 sentences). display_text = complete detailed answer.
        
        Keep spoken responses crisp, energetic, friendly, and robotic-futuristic.
        Support English, Hindi, Urdu, and Hinglish seamlessly.
    """.trimIndent()

    suspend fun processCommand(userQuery: String): GeminiResult<MotoAction> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            // Provide offline smart rule fallback if no key is configured yet
            val offlineAction = fallbackOfflineRouter(userQuery)
            return@withContext GeminiResult.Success(offlineAction)
        }

        try {
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = userQuery))
                    )
                ),
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = systemInstructionText))
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.2f,
                    topP = 0.9f
                )
            )

            val response = GeminiApiClient.service.generateContent(textModel, apiKey, request)

            if (response.error != null) {
                val code = response.error.code ?: 0
                val msg = response.error.message ?: "Unknown Gemini API error"
                return@withContext when {
                    code == 400 || code == 403 || msg.contains("API key", ignoreCase = true) ->
                        GeminiResult.Error("Invalid or unauthorized Gemini API key: $msg", isKeyError = true)
                    code == 429 || msg.contains("quota", ignoreCase = true) || msg.contains("resource exhausted", ignoreCase = true) ->
                        GeminiResult.Error("Gemini API quota exceeded or rate limited: $msg", isQuotaError = true)
                    else -> GeminiResult.Error("Gemini API error ($code): $msg")
                }
            }

            val rawOutput = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                ?: return@withContext GeminiResult.Error("Empty response from MOTO AI core.")

            // Clean output if markdown fences were accidentally produced
            val cleanJson = rawOutput
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val action = try {
                actionAdapter.fromJson(cleanJson) ?: fallbackOfflineRouter(userQuery)
            } catch (e: Exception) {
                // If JSON parsing fails, treat as conversation
                MotoAction(
                    intent = "CONVERSE",
                    target = "",
                    spokenResponse = cleanJson.take(150),
                    displayText = cleanJson
                )
            }

            GeminiResult.Success(action)

        } catch (e: HttpException) {
            val code = e.code()
            val errorBody = e.response()?.errorBody()?.string() ?: e.message()
            when {
                code == 400 || code == 403 || errorBody.contains("API_KEY_INVALID", ignoreCase = true) ->
                    GeminiResult.Error("API Key invalid or unauthorized ($code). Please check Settings.", isKeyError = true)
                code == 429 || errorBody.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ->
                    GeminiResult.Error("Rate limit / Quota exceeded ($code). Please try again shortly.", isQuotaError = true)
                else -> GeminiResult.Error("Network error ($code): $errorBody")
            }
        } catch (e: IOException) {
            // Offline fallback
            val offline = fallbackOfflineRouter(userQuery)
            GeminiResult.Success(offline)
        } catch (e: Exception) {
            GeminiResult.Error("Processing failure: ${e.localizedMessage ?: e.message}")
        }
    }

    /**
     * Highly responsive on-device rule router when network or API key is absent,
     * ensuring MOTO AI core voice and smart actions still work reliably!
     */
    fun fallbackOfflineRouter(input: String): MotoAction {
        val query = input.trim()

        // Strip wake words "moto" or "hey moto" while preserving original casing
        val originalClean = query
            .replace(Regex("^(hey\\s+|hi\\s+|ok\\s+)?moto[,\\s:]*", RegexOption.IGNORE_CASE), "")
            .trim()
        val lower = originalClean.lowercase()

        return when {
            lower.startsWith("open youtube") || lower == "youtube" ->
                MotoAction("OPEN_APP", "YouTube", null, false, null, "Opening YouTube for you.", "Opening YouTube")

            lower.startsWith("open chrome") || lower == "chrome" ->
                MotoAction("OPEN_APP", "Chrome", null, false, null, "Opening Google Chrome.", "Opening Chrome")

            lower.startsWith("open settings") || lower.startsWith("settings") ->
                MotoAction("OPEN_SETTINGS", "main", null, false, null, "Opening system settings.", "Opening Settings")

            lower.startsWith("open wifi") || lower.contains("wifi setting") ->
                MotoAction("OPEN_SETTINGS", "wifi", null, false, null, "Opening Wi-Fi settings.", "Opening Wi-Fi")

            lower.startsWith("open bluetooth") || lower.contains("bluetooth setting") ->
                MotoAction("OPEN_SETTINGS", "bluetooth", null, false, null, "Opening Bluetooth settings.", "Opening Bluetooth")

            lower.startsWith("open play store") || lower.startsWith("play store") ->
                MotoAction("OPEN_APP", "Play Store", null, false, null, "Launching Google Play Store.", "Opening Play Store")

            lower.startsWith("open whatsapp") ->
                MotoAction("OPEN_APP", "WhatsApp", null, false, null, "Opening WhatsApp.", "Opening WhatsApp")

            lower.startsWith("open free fire") || lower.contains("launch free fire") ->
                MotoAction("OPEN_APP", "Free Fire", null, false, null, "Opening Free Fire.", "Opening Free Fire")

            lower.startsWith("open ") -> {
                val appName = originalClean.substring(5).trim()
                MotoAction("OPEN_APP", appName, null, false, null, "Opening $appName.", "Opening $appName")
            }

            lower.startsWith("search for ") -> {
                val q = originalClean.substring(11).trim()
                MotoAction("WEB_SEARCH", q, null, false, null, "Searching web for $q.", "Searching: $q")
            }

            lower.startsWith("search ") -> {
                val q = originalClean.substring(7).trim()
                MotoAction("WEB_SEARCH", q, null, false, null, "Searching web for $q.", "Searching: $q")
            }

            lower.startsWith("call ") -> {
                val target = originalClean.substring(5).trim()
                MotoAction(
                    intent = "MAKE_CALL",
                    target = target,
                    param = null,
                    requiresConfirmation = true,
                    confirmationMessage = "MOTO wants to call $target. Continue?",
                    spokenResponse = "Do you want to call $target?",
                    displayText = "Confirm calling $target"
                )
            }

            lower.startsWith("navigate to ") || lower.startsWith("directions to ") -> {
                val prefixLen = if (lower.startsWith("navigate to ")) 12 else 14
                val dest = originalClean.substring(prefixLen).trim()
                MotoAction("START_NAVIGATION", dest, null, false, null, "Navigating to $dest.", "Navigating to $dest")
            }

            lower.startsWith("generate an image") || lower.startsWith("generate image") || lower.startsWith("create an image") || lower.startsWith("create image") -> {
                val p = originalClean.replace(Regex("^(generate|create)\\s+(an\\s+)?image(\\s+of)?", RegexOption.IGNORE_CASE), "").trim()
                MotoAction("GENERATE_IMAGE", if (p.isEmpty()) "Futuristic neon robotic AI in glowing cyberpunk city" else p, null, false, null, "Generating image: $p", "Generating Image")
            }

            lower.startsWith("generate a video") || lower.startsWith("create a video") || lower.startsWith("create video") || lower.startsWith("generate video") -> {
                val p = originalClean.replace(Regex("^(generate|create)\\s+(a\\s+)?video(\\s+of)?", RegexOption.IGNORE_CASE), "").trim()
                MotoAction("GENERATE_VIDEO", if (p.isEmpty()) "Futuristic robot walking through a neon city" else p, null, false, null, "Starting video creation for $p", "Generating Video")
            }

            lower.contains("who are you") || lower.contains("what is your name") ->
                MotoAction("CONVERSE", "", null, false, null, "I am MOTO AI, your personal voice and device assistant.", "I am MOTO AI. Your Voice. Your AI. Your MOTO.")

            lower.contains("weather") ->
                MotoAction("WEB_SEARCH", "current weather forecast", null, false, null, "Checking current weather conditions.", "Checking Weather")

            else ->
                MotoAction(
                    intent = "CONVERSE",
                    target = "",
                    spokenResponse = "MOTO is ready. (Configure your Gemini API key in Settings for full intelligence, or give me a device command).",
                    displayText = "MOTO AI standing by. You can command apps, web searches, navigation, phone calls, or set your Gemini API key in Settings."
                )
        }
    }
}
