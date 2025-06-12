package com.github.fmueller.jarvis.commands

import com.github.fmueller.jarvis.ai.OllamaService
import com.github.fmueller.jarvis.conversation.Conversation
import com.github.fmueller.jarvis.conversation.Role
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking

class ModelCommandTest : TestCase() {

    override fun tearDown() {
        OllamaService.modelName = "qwen3:4b"
    }

    fun `test default model name`() {
        assertEquals("qwen3:4b", OllamaService.modelName)
    }

    fun `test run sets arbitrary model name`() = runBlocking {
        val conversation = Conversation()
        ModelCommand("foobar").run(conversation)

        assertEquals("foobar", OllamaService.modelName)
        assertEquals("Model changed to foobar", conversation.messages.last().content)
        assertEquals(Role.INFO, conversation.messages.last().role)
    }

    fun `test default model name should set it back to qwen model`() = runBlocking {
        val conversation = Conversation()
        ModelCommand("foobar").run(conversation)
        ModelCommand("default").run(conversation)

        assertEquals("qwen3:4b", OllamaService.modelName)
    }
}
