package com.github.fmueller.jarvis.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*

@Service(Service.Level.PROJECT)
class OllamaService : Disposable {

    private val process = OllamaProcess()

    init {
        if (isOllamaRunning()) {
            thisLogger().info("Ollama process is already running")
        } else {
            process.start()
            runBlocking {
                val startTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startTime < 5000) {
                    if (isOllamaRunning()) {
                        thisLogger().info("Ollama process started")
                        break
                    } else {
                        delay(500)
                    }
                }

                if (!isOllamaRunning()) {
                    thisLogger().error("Failed to start Ollama process")
                    // TODO send message to user interface
                }
            }
        }
    }

    // TODO make it non-blocking
    fun ask(question: String): String = runBlocking {
        // TODO check if model is available
        // TODO if not download model
        try {
            val client = HttpClient.newHttpClient()
            val request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:11434/api/chat"))
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        """
                    {
                      "model": "llama3",
                      "messages": [
                        {
                          "role": "user",
                          "content": "$question"
                        }
                      ],
                      "stream": false
                    }
                """.trimIndent()
                    )
                )
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            val body = response.body()
            thisLogger().warn("Response: $body")
            body
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    override fun dispose() {
        process.stop()
    }

    private fun isOllamaRunning(): Boolean {
        return try {
            val client = HttpClient.newHttpClient()
            val request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:11434"))
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            response.statusCode() == 200
        } catch (e: Exception) {
            false
        }
    }

    private class OllamaProcess {

        private var process: Process? = null

        fun start() {
            thisLogger().info("Starting Ollama process")
            try {
                process = ProcessBuilder(getOllamaCommand(), "serve").start()
            } catch (e: UnsupportedOperationException) {
                thisLogger().error("Unsupported operating system", e)
            } catch (e: Exception) {
                thisLogger().error("Failed to start Ollama process", e)
            }
        }

        fun stop() {
            thisLogger().info("Destroying Ollama process")
            process?.destroy()
        }

        private fun getOllamaCommand(): String {
            val os = System.getProperty("os.name").lowercase(Locale.getDefault())
            return when {
                os.contains("win") -> "ollama.exe"
                os.contains("nix") || os.contains("nux") || os.contains("mac") -> "ollama"
                else -> throw UnsupportedOperationException("Unsupported operating system: $os")
            }
        }
    }
}