package com.github.fmueller.jarvis.conversation

import com.github.fmueller.jarvis.ai.OllamaService
import com.sun.net.httpserver.HttpServer
import junit.framework.TestCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket

class ConversationCancellationTest : TestCase() {

    private lateinit var server: HttpServer
    override fun setUp() {
        super.setUp()
        val port = findAvailablePort()
        server = HttpServer.create(InetSocketAddress(port), 0)
        server.createContext("/") { exchange ->
            exchange.sendResponseHeaders(200, -1)
            exchange.close()
        }
        server.createContext("/api/tags") { exchange ->
            val body = """{"models":[{"name":"${OllamaService.modelName}"}]}"""
            exchange.sendResponseHeaders(200, body.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(body.toByteArray()) }
        }
        server.createContext("/api/generate") { exchange ->
            exchange.sendResponseHeaders(200, 0)
            val out = exchange.responseBody
            try {
                out.write("{\"response\":\"hello\",\"done\":false}".toByteArray())
                out.flush()
                Thread.sleep(2000)
                out.write("{\"done\":true}".toByteArray())
            } catch (_: IOException) {
            } finally {
                out.close()
            }
        }
        server.start()
        OllamaService.host = "http://localhost:$port"
    }

    override fun tearDown() {
        server.stop(0)
        super.tearDown()
    }

    fun `test cancel chat aborts request`() = runBlocking {
        val conversation = Conversation()
        val job = conversation.startChat(Message(Role.USER, "hi"), this)
        delay(200)
        conversation.cancelChat()
        job.join()

        assertFalse(conversation.isChatInProgress())
        assertEquals(2, conversation.messages.size)
    }

    private fun findAvailablePort(): Int {
        return ServerSocket(0).use { it.localPort }
    }
}
