package com.github.fmueller.jarvis.ai

import com.github.fmueller.jarvis.conversation.Conversation
import dev.langchain4j.model.ollama.OllamaStreamingChatModel
import dev.langchain4j.service.AiServices
import dev.langchain4j.service.TokenStream
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
import java.time.Duration
import java.util.concurrent.CompletableFuture

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

    // TODO should I add something about putting the language identifier in the code block?
    private val systemMessage = """
                        You are Jarvis.
                        You are a helpful coding assistant on the level of an expert software developer.
                        You ask clarifying questions to understand the user's problem if you don't have enough information.
                        You are running in the IDE of the user.
                        You have access to code the user is working on.
                        You format responses in Markdown.
                        You use paragraphs, lists, and code blocks to make your responses more readable.
                        """.trimIndent()

    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .build()

    private interface Assistant {

        fun chat(message: String): TokenStream
    }

    private val assistant = AiServices
        .builder(Assistant::class.java)
        .streamingChatLanguageModel(
            OllamaStreamingChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3.1")
                .build()
        )
        .systemMessageProvider { chatMemoryId -> systemMessage }
        .build()

    suspend fun chatLangChain4J(conversation: Conversation): String = withContext(Dispatchers.IO) {
        // TODO check if model is available
        // TODO if not, download model

        // TODO migration to LangChain4J: change to Kotlin Coroutines
        // TODO migration to LangChain4J: add timeout handling
        // TODO migration to LangChain4J: research how token limits work and context windows
        // TODO migration to LangChain4J: research how chat memory is configured
        val future = CompletableFuture<String>()
        assistant
            .chat(conversation.getLastUserMessage()?.content ?: "Tell me that there was not message provided.")
            .onNext { update -> conversation.addToMessageBeingGenerated(update) }
            .onComplete { response -> future.complete(response.content().text()) }
            .onError { error -> future.complete("Error: ${error.message}") }
            .start()
        future.get()
    }

    suspend fun chat(conversation: Conversation): String = withContext(Dispatchers.IO) {
        try {
            val chatRequest = ChatRequest(
                "llama3.1",
                listOf(
                    ChatMessage("system", systemMessage)
                ) + conversation.messages.map { ChatMessage(it.role.toString(), it.asMarkdown()) },
                true
            )
            val httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:11434/api/chat"))
                .timeout(Duration.ofSeconds(5))
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
            val request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:11434"))
                .timeout(Duration.ofSeconds(2))
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            response.statusCode() == 200
        } catch (e: Exception) {
            false
        }
    }
}
