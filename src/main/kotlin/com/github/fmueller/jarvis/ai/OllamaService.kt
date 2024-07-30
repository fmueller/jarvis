package com.github.fmueller.jarvis.ai

import com.github.fmueller.jarvis.conversation.Conversation
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

    suspend fun chat(conversation: Conversation): String = withContext(Dispatchers.IO) {
        // TODO check if model is available
        // TODO if not, download model
        try {
            val client = HttpClient.newHttpClient()
            val chatRequest = ChatRequest(
                "llama3.1",
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
                ) + conversation.messages.map { ChatMessage(it.role.toString(), it.asMarkdown()) },
                true
            )
            val httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:11434/api/chat"))
                .POST(HttpRequest.BodyPublishers.ofString(Json.encodeToString(chatRequest)))
                .build()

            val json = Json { ignoreUnknownKeys = true }
            val response = StringBuilder()
            client.send(httpRequest) {
                HttpResponse.BodySubscribers.mapping(HttpResponse.BodySubscribers.ofLines(Charsets.UTF_8)) { lines ->
                    lines
                        .filter { line -> line.isNotEmpty() }
                        .forEach { line ->
                            run {
                                val update = json.decodeFromString<ChatResponse>(line).message.content
                                response.append(update)
                                conversation.addToMessageBeingGenerated(update)
                            }
                        }
                }
            }
            response.toString()
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
