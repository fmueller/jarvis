package com.github.fmueller.jarvis.ai

import junit.framework.TestCase
import java.lang.reflect.Proxy

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

    fun `test chat memory max tokens uses context window size`() {
        try {
            OllamaService.contextWindowSize = 2345

            val assistantField = OllamaService::class.java.getDeclaredField("assistant")
            assistantField.isAccessible = true
            val assistant = assistantField.get(OllamaService)

            val handler = Proxy.getInvocationHandler(assistant)
            val outerField = handler.javaClass.getDeclaredField("this$0")
            outerField.isAccessible = true
            val defaultAiServices = outerField.get(handler)

            val contextField = defaultAiServices.javaClass.superclass.getDeclaredField("context")
            contextField.isAccessible = true
            val context = contextField.get(defaultAiServices)

            val chatMemoryServiceField = context.javaClass.getDeclaredField("chatMemoryService")
            chatMemoryServiceField.isAccessible = true
            val chatMemoryService = chatMemoryServiceField.get(context)

            val defaultChatMemoryField = chatMemoryService.javaClass.getDeclaredField("defaultChatMemory")
            defaultChatMemoryField.isAccessible = true
            val chatMemory = defaultChatMemoryField.get(chatMemoryService)

            val maxTokensField = chatMemory.javaClass.getDeclaredField("maxTokens")
            maxTokensField.isAccessible = true
            val maxTokens = maxTokensField.get(chatMemory) as Int

            assertEquals(2345, maxTokens)
        } finally {
            OllamaService.contextWindowSize = 4096
        }
    }
}
