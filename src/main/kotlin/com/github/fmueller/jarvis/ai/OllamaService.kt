package com.github.fmueller.jarvis.ai

import com.github.fmueller.jarvis.conversation.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

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

object OllamaService {

    suspend fun chat(messages: List<Message>): String = withContext(Dispatchers.IO) {
        // TODO check if model is available
        // TODO if not, download model
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
                ) + messages.map { ChatMessage(it.role.toString(), it.toString()) },
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

    fun isAvailable(): Boolean {
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
}
