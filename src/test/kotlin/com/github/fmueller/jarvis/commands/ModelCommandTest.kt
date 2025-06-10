package com.github.fmueller.jarvis.commands

import com.github.fmueller.jarvis.ai.OllamaService
import com.github.fmueller.jarvis.conversation.Conversation
import junit.framework.TestCase

class ModelCommandTest : TestCase() {

    override fun tearDown() {
        OllamaService.modelName = "qwen3:4b"
    }

    fun `test default model name`() {
        assertEquals("qwen3:4b", OllamaService.modelName)
    }

    fun `test run sets arbitrary model name`() {
        val conversation = Conversation()
        ModelCommand("foobar").run(conversation)

        assertEquals("foobar", OllamaService.modelName)
        assertEquals("Model changed to foobar", conversation.messages.last().content)
    }
}
