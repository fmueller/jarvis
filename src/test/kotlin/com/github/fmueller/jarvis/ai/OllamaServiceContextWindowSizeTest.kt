package com.github.fmueller.jarvis.ai

import junit.framework.TestCase

class OllamaServiceContextWindowSizeTest : TestCase() {

    fun `test default context window size`() {
        assertEquals(4096, OllamaService.contextWindowSize)
    }

    fun `test changing context window size recreates assistant`() {
        val assistantField = OllamaService::class.java.getDeclaredField("assistant")
        assistantField.isAccessible = true
        val initialAssistant = assistantField.get(OllamaService)

        OllamaService.contextWindowSize = 1234

        val newAssistant = assistantField.get(OllamaService)
        assertNotSame(initialAssistant, newAssistant)
        assertEquals(1234, OllamaService.contextWindowSize)
    }
}
