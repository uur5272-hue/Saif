package com.example.motoai.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Structured Action Command returned by MOTO AI logic router.
 * Examples:
 * OPEN_APP (target: YouTube)
 * WEB_SEARCH (query: Free Fire)
 * OPEN_URL (url: https://...)
 * OPEN_SETTINGS (setting: wifi, bluetooth, display, main)
 * MAKE_CALL (target: Rahul or phone number, requires_confirmation: true)
 * SEND_SMS (target: contact, message: text)
 * CREATE_REMINDER (title: Buy milk)
 * START_NAVIGATION (location: Taj Mahal)
 * PLAY_STORE_SEARCH (query: Free Fire)
 * GENERATE_IMAGE (prompt: a neon cyber robot)
 * GENERATE_VIDEO (prompt: cyber city)
 * CONVERSE (reply: conversational assistant text)
 */
@JsonClass(generateAdapter = true)
data class MotoAction(
    @Json(name = "intent") val intent: String,
    @Json(name = "target") val target: String? = null,
    @Json(name = "param") val param: String? = null,
    @Json(name = "requires_confirmation") val requiresConfirmation: Boolean = false,
    @Json(name = "confirmation_message") val confirmationMessage: String? = null,
    @Json(name = "spoken_response") val spokenResponse: String = "",
    @Json(name = "display_text") val displayText: String = ""
)

enum class MotoIntent {
    OPEN_APP,
    WEB_SEARCH,
    OPEN_URL,
    OPEN_SETTINGS,
    MAKE_CALL,
    SEND_SMS,
    CREATE_REMINDER,
    START_NAVIGATION,
    PLAY_STORE_SEARCH,
    GENERATE_IMAGE,
    GENERATE_VIDEO,
    CONVERSE,
    UNKNOWN
}
