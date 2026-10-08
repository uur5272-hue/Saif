package com.example

import com.example.motoai.data.service.GeminiService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MotoAiUnitTest {

    @Test
    fun testOfflineRouterAppIntent() {
        val service = GeminiService { "" }
        val action = service.fallbackOfflineRouter("MOTO, open YouTube")
        assertEquals("OPEN_APP", action.intent)
        assertEquals("YouTube", action.target)
    }

    @Test
    fun testOfflineRouterWebSearchIntent() {
        val service = GeminiService { "" }
        val action = service.fallbackOfflineRouter("MOTO, search for Free Fire")
        assertEquals("WEB_SEARCH", action.intent)
        assertEquals("Free Fire", action.target)
    }

    @Test
    fun testOfflineRouterCallConfirmationRequirement() {
        val service = GeminiService { "" }
        val action = service.fallbackOfflineRouter("MOTO, call Rahul")
        assertEquals("MAKE_CALL", action.intent)
        assertEquals("Rahul", action.target)
        assertTrue(action.requiresConfirmation)
        assertNotNull(action.confirmationMessage)
    }

    @Test
    fun testOfflineRouterImageGeneration() {
        val service = GeminiService { "" }
        val action = service.fallbackOfflineRouter("MOTO, generate an image of a futuristic city at night")
        assertEquals("GENERATE_IMAGE", action.intent)
        assertTrue(action.target?.contains("futuristic city") == true)
    }
}
