package com.github.fmueller.jarvis.ai

import com.github.fmueller.jarvis.conversation.Conversation
import com.github.fmueller.jarvis.conversation.Message
import com.github.fmueller.jarvis.conversation.Role
import com.sun.net.httpserver.HttpServer
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.net.ServerSocket

class OllamaServiceModelDownloadErrorTest : TestCase() {

    private lateinit var server: HttpServer
    private var port: Int = 0

    override fun setUp() {
        super.setUp()
        port = findAvailablePort()
        server = HttpServer.create(InetSocketAddress(port), 0)
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
        OllamaService.port = port
        OllamaService.clearChatMemory()
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

    private fun findAvailablePort(): Int {
        return ServerSocket(0).use { socket ->
            socket.localPort
        }
    }
}
