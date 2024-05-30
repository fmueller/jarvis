package com.github.fmueller.jarvis.ai

import com.github.fmueller.jarvis.conversation.Conversation
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*

@Serializable
private data class ChatMessage(val role: String, val content: String)

@Serializable
private data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean
)

@Serializable
private data class ChatResponse(val message: ChatMessage)

@Service(Service.Level.PROJECT)
class OllamaService : Disposable {

    private val process = OllamaProcess()

    init {
        // TODO remove this autostart logic and show messages in conversation panel instead
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

    suspend fun chat(conversation: Conversation): String = withContext(Dispatchers.IO) {
        // TODO check if model is available
        // TODO if not download model
        try {
            val client = HttpClient.newHttpClient()
            val chatRequest = ChatRequest(
                "llama3",
                listOf(
                    ChatMessage(
                        "system",
                        // TODO is it better to reference the assistant by name or role?
                        // TODO should I add something about putting the language identifier in the code block?
                        """
                        You are Jarvis.
                        You are a helpful coding assistant on the level of an expert software developer.
                        You ask clarifying questions to understand the user's problem if you don't have enough information.
                        You are running in the IDE of the user.
                        You have access to code the user is working on.
                        You format responses in Markdown.
                        You use paragraphs, lists, and code blocks to make your responses more readable.
                        """.trimIndent()
                    )
                ) + conversation.getMessages().map { ChatMessage(it.role.toString(), it.content) },
                false
            )
            val httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:11434/api/chat"))
                .POST(HttpRequest.BodyPublishers.ofString(Json.encodeToString(chatRequest)))
                .build()

            val httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString())
            val json = Json { ignoreUnknownKeys = true }
            val response = json.decodeFromString<ChatResponse>(httpResponse.body())
            response.message.content
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