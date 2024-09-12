package com.github.fmueller.jarvis.ai

import com.github.fmueller.jarvis.conversation.Conversation
import dev.langchain4j.memory.chat.TokenWindowChatMemory
import dev.langchain4j.model.ollama.OllamaStreamingChatModel
import dev.langchain4j.service.AiServices
import dev.langchain4j.service.TokenStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

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

    private var assistant = createAiService()

    fun clearChatMemory() {
        assistant = createAiService()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun chat(conversation: Conversation, useCodeContext: Boolean): String = withContext(Dispatchers.IO) {
        // TODO check if model is available
        // TODO if not, download model

        var lastUserMessage = if (conversation.getLastUserMessage() != null)
            if (useCodeContext)
                conversation.getLastUserMessage()!!.contentWithCodeContext()
            else
                conversation.getLastUserMessage()!!.contentWithClosedTrailingCodeBlock()
        else "Tell me that there was no message provided."

        val responseInFlight = StringBuilder()
        try {
            suspendCancellableCoroutine<String> { continuation ->
                assistant
                    .chat(lastUserMessage.removePrefix("/plain "))
                    .onNext { update ->
                        responseInFlight.append(update)
                        conversation.addToMessageBeingGenerated(update)
                    }
                    .onComplete { response -> continuation.resume(response.content().text()) { /* noop */ } }
                    .onError { error -> continuation.cancel(Exception(error.message)) }
                    .start()

                continuation.invokeOnCancellation {
                    // TODO when LangChain4j implemented AbortController, call it here
                }
            }
        } catch (e: Exception) {
            responseInFlight
                .appendLine()
                .appendLine()
                .appendLine("An error occurred while processing the message.")
                .appendLine()
                .append("Error: ")
                .append(e.message)
                .toString()
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

    private fun createAiService(): Assistant {
        return AiServices
            .builder(Assistant::class.java)
            .streamingChatLanguageModel(
                OllamaStreamingChatModel.builder()
                    .timeout(Duration.ofMinutes(5))
                    .baseUrl("http://localhost:11434")
                    .modelName("llama3.1")
                    .build()
            )
            .systemMessageProvider { chatMemoryId -> systemMessage }
            .chatMemory(
                TokenWindowChatMemory.builder()
                    .maxTokens(128_000, SimpleTokenizer())
                    .build()
            )
            .build()
    }
}
