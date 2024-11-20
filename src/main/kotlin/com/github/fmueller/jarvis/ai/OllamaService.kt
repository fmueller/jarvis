package com.github.fmueller.jarvis.ai

import com.github.fmueller.jarvis.conversation.CodeContext
import com.github.fmueller.jarvis.conversation.Conversation
import com.intellij.lang.Language
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

    // TODO add project info to system prompt instead of adding it to each user message
    // TODO add something about selected code has higher priority than the open files or references
    private val systemPrompt = """
                    You are Jarvis, an intelligent and helpful coding assistant on the level of an expert software developer. You assist users by providing code completions, debugging tips, explanations, and suggestions in various programming languages. Your responses are clear, concise, and directly address the user's needs.

                    All your responses should be formatted in Markdown. To improve readability, use paragraphs, lists, and headlines where appropriate.

                    - Follow coding best practices for readability, efficiency, and security.
                    - Use appropriate code blocks with syntax highlighting.
                    - Include comments and brief explanations where necessary.
                    - If unsure or lacking context, ask clarifying questions.
                    - Keep responses focused and avoid unnecessary details.

                    Examples:

                    Example 1:

                    [User]: How do I reverse a linked list in Java?

                    [Code Context]:

                    *No code context provided.*

                    [Assistant]: You can reverse a singly linked list in Java by iterating through the list and adjusting the pointers. Here's how you can do it:

                    ```java
                    public ListNode reverseList(ListNode head) {
                        ListNode prev = null;
                        ListNode current = head;
                        while (current != null) {
                            ListNode nextTemp = current.next;
                            current.next = prev;
                            prev = current;
                            current = nextTemp;
                        }
                        return prev;
                    }
                    ´´´

                    This function initializes three pointers and iteratively reverses the direction of the next pointers.

                    Example 2:

                    [User]: I'm getting a NullPointerException when I run my code.

                    [Code Context]:

                    ´´´java
                    public class Main {
                        public static void main(String[] args) {
                            String text = null;
                            System.out.println(text.length());
                        }
                    }
                    ´´´

                    [Assistant]: The `NullPointerException` occurs because you're trying to call `length()` on a null object. You need to initialize `text` before using it:

                    ´´´java
                    public class Main {
                        public static void main(String[] args) {
                            String text = "Hello, World!";
                            System.out.println(text.length());
                        }
                    }
                    ´´´

                    By assigning a value to `text`, you can safely call `length()` without triggering a `NullPointerException`.

                    Example 3:

                    [User]: Optimize this function for better performance.

                    [Code Context]:

                    ´´´python
                    def process_data(data):
                        result = []
                        for item in data:
                            if item not in result:
                                result.append(item)
                        return result
                    ´´´

                    [Assistant]: You can optimize the function by using a `set` to remove duplicates more efficiently:

                    ´´´python
                    def process_data(data):
                        # Use a set to remove duplicates and convert back to a list
                        return list(set(data))
                    ´´´

                    This approach leverages the `set` data structure to eliminate duplicate items more efficiently than iterating through the list.
                    """.trimIndent()

    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .build()

    private interface Assistant {

        fun chat(message: String): TokenStream
    }

    private var modelName = "llama3.1"
    private var assistant = createAiService()

    fun clearChatMemory() {
        assistant = createAiService()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun chat(conversation: Conversation, useCodeContext: Boolean): String = withContext(Dispatchers.IO) {
        // TODO check if model is available
        // TODO if not, download model

        val nextMessagePrompt =
            if (conversation.getLastUserMessage() != null) {
                val lastUserMessage = conversation.getLastUserMessage()!!
                """
                |[User]: ${lastUserMessage.contentWithClosedTrailingCodeBlock().removePrefix("/plain ")}
                |
                |My project in the IDE is called ${lastUserMessage.codeContext!!.projectName}.
                |
                |[Code Context]:
                |
                |${getCodeContextPrompt(lastUserMessage.codeContext, !lastUserMessage.isHelpMessage() && useCodeContext)}
                |
                |[Assistant]: """.trimMargin()
            } else {
                "Tell me that there was no message provided."
            }

        val responseInFlight = StringBuilder()
        try {
            suspendCancellableCoroutine { continuation ->
                assistant
                    .chat(nextMessagePrompt)
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

    private fun getCodeContextPrompt(codeContext: CodeContext?, useCodeContext: Boolean): String {
        return if (useCodeContext && codeContext != null && codeContext.hasSelectedCode()) {
            val selectedCode = codeContext.selected!!
            """
            |```${if (selectedCode.language == Language.ANY) "plaintext" else selectedCode.language.id.lowercase()}
            |${selectedCode.content}
            |```
            """.trimMargin()
        } else {
            "*No code context provided.*"
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
                    .modelName(modelName)
                    .build()
            )
            .systemMessageProvider { chatMemoryId -> systemPrompt }
            .chatMemory(
                TokenWindowChatMemory.builder()
                    .maxTokens(128_000, SimpleTokenizer())
                    .build()
            )
            .build()
    }

    fun setModel(newModelName: String) {
        modelName = newModelName
        assistant = createAiService()
    }
}
