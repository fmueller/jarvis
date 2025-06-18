package com.github.fmueller.jarvis.ai

import com.sun.net.httpserver.HttpServer
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.net.ServerSocket

class OllamaServiceModelNameComparisonTest : TestCase() {

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
            val body = """
                {
                    "models": [
                        {"name": "modelName1"},
                        {"name": "ModelName2"},
                        {"name": "modelname3"}
                    ]
                }
            """.trimIndent()
            exchange.sendResponseHeaders(200, body.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(body.toByteArray()) }
        }
        server.start()
        OllamaService.host = "http://localhost:$port"
    }

    override fun tearDown() {
        server.stop(0)
        super.tearDown()
    }

    fun `test case insensitive model name comparison`() = runBlocking {
        OllamaService.modelName = "modelname2"

        val result = OllamaService.isModelAvailable()

        assertTrue(result)
    }

    private fun findAvailablePort(): Int {
        return ServerSocket(0).use { socket ->
            socket.localPort
        }
    }
}
