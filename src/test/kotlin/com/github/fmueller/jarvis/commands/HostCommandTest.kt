package com.github.fmueller.jarvis.commands

import com.github.fmueller.jarvis.ai.OllamaService
import com.github.fmueller.jarvis.conversation.Conversation
import com.github.fmueller.jarvis.conversation.Role
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking

class HostCommandTest : TestCase() {

    override fun tearDown() {
        OllamaService.host = "http://localhost:11434"
    }

    fun `test default host`() {
        assertEquals("http://localhost:11434", OllamaService.host)
    }

    fun `test run sets arbitrary host`() = runBlocking {
        val conversation = Conversation()
        HostCommand("http://192.168.0.1:11434").run(conversation)

        assertEquals("http://192.168.0.1:11434", OllamaService.host)
        assertEquals("Host changed to http://192.168.0.1:11434", conversation.messages.last().content)
        assertEquals(Role.INFO, conversation.messages.last().role)
    }

    fun `test default host resets value`() = runBlocking {
        val conversation = Conversation()
        HostCommand("http://1.2.3.4").run(conversation)
        HostCommand("default").run(conversation)

        assertEquals("http://localhost:11434", OllamaService.host)
    }
}
