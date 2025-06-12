package com.github.fmueller.jarvis.ai

import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class OllamaServicePromptTest {

    @Test
    fun `system prompt uses triple backticks`() {
        val field = OllamaService::class.java.getDeclaredField("systemPrompt")
        field.isAccessible = true
        val prompt = field.get(OllamaService) as String
        assertFalse("System prompt contains accent backticks", prompt.contains("´´´"))
        assertTrue("System prompt should contain triple backticks", prompt.contains("```"))
    }
}
