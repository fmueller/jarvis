package com.github.fmueller.jarvis.commands

import com.github.fmueller.jarvis.ai.OllamaService
import com.github.fmueller.jarvis.conversation.Conversation
import com.github.fmueller.jarvis.conversation.Role
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking

class ModelCommandTest : TestCase() {

    override fun setUp() {
        super.setUp()
        OllamaService.modelName = OllamaService.DEFAULT_MODEL_NAME
    }

    override fun tearDown() {
        OllamaService.modelName = OllamaService.DEFAULT_MODEL_NAME
    }

    fun `test default model name`() {
        assertEquals("qwen3:1.7b", OllamaService.modelName)
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

        assertEquals(OllamaService.DEFAULT_MODEL_NAME, OllamaService.modelName)
    }
}
