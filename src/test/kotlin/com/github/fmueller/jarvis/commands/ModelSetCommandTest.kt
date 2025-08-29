package com.github.fmueller.jarvis.commands

import com.github.fmueller.jarvis.ai.OllamaService
import com.github.fmueller.jarvis.conversation.Conversation
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking

class ModelSetCommandTest : TestCase() {

    override fun setUp() {
        super.setUp()
        OllamaService.temperature = 0.7
    }

    fun `test run sets parameter`() = runBlocking {
        val conversation = Conversation()
        ModelSetCommand(mapOf("temperature" to "1.0")).run(conversation)
        assertEquals(1.0, OllamaService.temperature)
        assertEquals("Parameters updated", conversation.messages.last().content)
    }
}

