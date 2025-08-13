package com.github.fmueller.jarvis.commands

import com.github.fmueller.jarvis.ai.OllamaService
import com.github.fmueller.jarvis.conversation.Conversation
import com.github.fmueller.jarvis.conversation.Role
import com.sun.net.httpserver.HttpServer
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress

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

    fun `test run without model name shows model info`() = runBlocking {
        val conversation = Conversation()
        val server = HttpServer.create(InetSocketAddress(0), 0)
        val response = "{\"license\":\"test\"}"
        server.createContext("/api/show") { exchange ->
            exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(response.toByteArray()) }
        }
        server.start()
        val originalHost = OllamaService.host
        try {
            OllamaService.host = "http://localhost:${server.address.port}"
            ModelCommand().run(conversation)
        } finally {
            OllamaService.host = originalHost
            server.stop(0)
        }
        val expectedFormattedOutput = " Model\n\n  Parameters\n\n  License\n    test"
        assertEquals(expectedFormattedOutput, conversation.messages.last().content)
        assertEquals(Role.INFO, conversation.messages.last().role)
    }
}
