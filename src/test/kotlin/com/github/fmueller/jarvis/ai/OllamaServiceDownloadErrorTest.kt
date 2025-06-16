package com.github.fmueller.jarvis.ai

import com.github.fmueller.jarvis.conversation.Conversation
import com.github.fmueller.jarvis.conversation.Message
import com.github.fmueller.jarvis.conversation.Role
import com.sun.net.httpserver.HttpServer
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress

class OllamaServiceDownloadErrorTest : TestCase() {

    private lateinit var server: HttpServer

    override fun setUp() {
        super.setUp()
        server = HttpServer.create(InetSocketAddress(11434), 0)
        server.createContext("/") { exchange ->
            exchange.sendResponseHeaders(200, -1)
            exchange.close()
        }
        server.createContext("/api/tags") { exchange ->
            val body = """{"models": []}"""
            exchange.sendResponseHeaders(200, body.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(body.toByteArray()) }
        }
        server.createContext("/api/pull") { exchange ->
            val body = """{"error":"model not found"}"""
            exchange.sendResponseHeaders(200, body.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(body.toByteArray()) }
        }
        server.start()
    }

    override fun tearDown() {
        server.stop(0)
        super.tearDown()
    }

    fun `test model download error is shown`() = runBlocking {
        val conversation = Conversation()
        conversation.addMessage(Message(Role.USER, "hello"))

        OllamaService.chat(conversation, true)

        val lastMessage = conversation.messages.last()
        assertEquals(Role.INFO, lastMessage.role)
        assertTrue(lastMessage.content.contains("model not found"))
    }
}
