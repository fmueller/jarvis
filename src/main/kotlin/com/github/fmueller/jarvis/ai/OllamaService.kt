package com.github.fmueller.jarvis.ai

import com.github.fmueller.jarvis.conversation.CodeContext
import com.github.fmueller.jarvis.conversation.Conversation
import com.github.fmueller.jarvis.conversation.Message
import com.intellij.lang.Language
import dev.langchain4j.http.client.HttpClient
import dev.langchain4j.http.client.HttpClientBuilder
import dev.langchain4j.http.client.HttpRequest
import dev.langchain4j.http.client.SuccessfulHttpResponse
import dev.langchain4j.http.client.sse.ServerSentEventListener
import dev.langchain4j.http.client.sse.ServerSentEventParser
import dev.langchain4j.memory.chat.TokenWindowChatMemory
import dev.langchain4j.model.ollama.OllamaStreamingChatModel
import dev.langchain4j.service.AiServices
import dev.langchain4j.service.TokenStream
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.jetbrains.annotations.VisibleForTesting
import java.io.IOException
import java.net.URI
import java.time.Duration
import java.util.concurrent.TimeUnit

object OllamaService {

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
                    ```

                    This function initializes three pointers and iteratively reverses the direction of the next pointers.

                    Example 2:

                    [User]: I'm getting a NullPointerException when I run my code.

                    [Code Context]:

                    ```java
                    public class Main {
                        public static void main(String[] args) {
                            String text = null;
                            System.out.println(text.length());
                        }
                    }
                    ```

                    [Assistant]: The `NullPointerException` occurs because you're trying to call `length()` on a null object. You need to initialize `text` before using it:

                    ```java
                    public class Main {
                        public static void main(String[] args) {
                            String text = "Hello, World!";
                            System.out.println(text.length());
                        }
                    }
                    ```

                    By assigning a value to `text`, you can safely call `length()` without triggering a `NullPointerException`.

                    Example 3:

                    [User]: Optimize this function for better performance.

                    [Code Context]:

                    ```python
                    def process_data(data):
                        result = []
                        for item in data:
                            if item not in result:
                                result.append(item)
                        return result
                    ```

                    [Assistant]: You can optimize the function by using a `set` to remove duplicates more efficiently:

                    ```python
                    def process_data(data):
                        # Use a set to remove duplicates and convert back to a list
                        return list(set(data))
                    ```

                    This approach leverages the `set` data structure to eliminate duplicate items more efficiently than iterating through the list.
                    """.trimIndent()

    private var currentHttpClient: CancellableHttpClient? = null

    private val client = java.net.http.HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .build()

    /**
     * Base URL used for all requests to Ollama.
     */
    var host: String = "http://localhost:11434"
        set(value) {
            field = value
            assistant = createAiService()
        }

    private interface Assistant {

        fun chat(message: String): TokenStream
    }

    const val DEFAULT_MODEL_NAME = "qwen3:1.7b"

    var modelName: String = DEFAULT_MODEL_NAME
        set(value) {
            field = value
            assistant = createAiService()
        }

    private var assistant = createAiService()

    fun clearChatMemory() {
        assistant = createAiService()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun chat(conversation: Conversation, useCodeContext: Boolean): String = withContext(Dispatchers.IO) {
        if (!ensureModelAvailable(conversation)) {
            return@withContext ""
        }

        val projectPromptPart = if (conversation.isFirstUserMessage()) {
            val lastUserMessage = conversation.getLastUserMessage()
            val codeContext = lastUserMessage?.codeContext
            val projectName = codeContext?.projectName

            if (projectName != null) {
                """
                |
                |Project: $projectName
                |
                """.trimMargin()
            } else {
                ""
            }
        } else {
            ""
        }

        val nextMessagePrompt = conversation.getLastUserMessage()?.let {
            lastUserMessage ->
                """
                |[User]: ${lastUserMessage.contentWithClosedTrailingCodeBlock().removePrefix("/plain ")}
                |$projectPromptPart
                |[Code Context]:
                |
                |${getCodeContextPrompt(lastUserMessage.codeContext, !lastUserMessage.isHelpMessage() && useCodeContext)}
                |
                |[Assistant]: """.trimMargin()
        } ?: "Tell me that there was no message provided."

        val responseInFlight = StringBuilder()
        try {
            suspendCancellableCoroutine { continuation ->
                val tokenStream = assistant
                    .chat(nextMessagePrompt)
                    .onPartialResponse { update ->
                        if (continuation.context.job.isCancelled) {
                            cancelCurrentRequest()
                            return@onPartialResponse
                        }
                        responseInFlight.append(update)
                        conversation.addToMessageBeingGenerated(update)
                    }
                    .onCompleteResponse { response ->
                        if (!continuation.context.job.isCancelled) {
                            continuation.resumeWith(Result.success(response.aiMessage().text()))
                        } else {
                            cancelCurrentRequest()
                        }
                    }
                    .onError { error ->
                        cancelCurrentRequest()
                        continuation.cancel(Exception(error.message))
                    }

                continuation.invokeOnCancellation {
                    cancelCurrentRequest()
                    runCatching {
                        if (responseInFlight.isNotEmpty()) {
                            responseInFlight
                                .appendLine()
                                .appendLine()
                                .appendLine("*Request cancelled by user.*")
                        }
                    }
                }

                tokenStream.start()
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
            val request = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create(host))
                .timeout(Duration.ofSeconds(2))
                .build()

            val response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString())
            response.statusCode() == 200
        } catch (e: Exception) {
            false
        }
    }

    @VisibleForTesting
    fun isModelAvailable(): Boolean {
        return try {
            val request = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create("$host/api/tags"))
                .timeout(Duration.ofSeconds(2))
                .build()

            val response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() != 200) return false
            val json = Json.parseToJsonElement(response.body())
            val models = json.jsonObject["models"]?.jsonArray ?: return false
            models.any {
                val name = it.jsonObject["name"]?.jsonPrimitive?.content ?: ""
                name.lowercase().startsWith(modelName.lowercase())
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun pullModel(): String? {
        return try {
            val request = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create("$host/api/pull"))
                .timeout(Duration.ofSeconds(2))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{\"name\":\"$modelName\"}"))
                .build()

            val response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString())
            val body = response.body()
            val error = body.lineSequence()
                .mapNotNull { runCatching {
                    Json.parseToJsonElement(it).jsonObject["error"]?.jsonPrimitive?.content }.getOrNull()
                }
                .firstOrNull()

            if (response.statusCode() != 200 || error != null) {
                error ?: "status ${response.statusCode()}"
            } else {
                null
            }
        } catch (e: Exception) {
            e.message
        }
    }

    private suspend fun ensureModelAvailable(conversation: Conversation): Boolean {
        if (isModelAvailable()) return true

        conversation.addMessage(Message.info("Downloading model..."))
        val pullError = pullModel()
        if (pullError != null) {
            conversation.addMessage(Message.info("Model download failed: $pullError"))
            return false
        }

        val timeout = System.currentTimeMillis() + Duration.ofMinutes(10).toMillis()
        while (System.currentTimeMillis() < timeout) {
            if (isModelAvailable()) {
                conversation.addMessage(Message.info("Model downloaded successfully."))
                return true
            }
            delay(3000)
        }

        conversation.addMessage(Message.info("Model download failed."))
        return false
    }

    private fun createAiService(): Assistant {
        cancelCurrentRequest()
        currentHttpClient = CancellableHttpClient()
        return AiServices
            .builder(Assistant::class.java)
            .streamingChatModel(
                OllamaStreamingChatModel.builder()
                    .httpClientBuilder(currentHttpClient!!.getHttpClientBuilder())
                    .timeout(Duration.ofMinutes(5))
                    .baseUrl(host)
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

    private fun cancelCurrentRequest() {
        currentHttpClient?.cancel()
    }

    /**
    * Wrapper for HttpClient that can be canceled
    */
    private class CancellableHttpClient {

        @Volatile
        private var currentCall: Call? = null

        fun getHttpClientBuilder(): HttpClientBuilder {
            return OkHttpClientBuilder(this)
        }

        fun setCurrentCall(call: Call) {
            currentCall?.cancel()
            currentCall = call
        }

        fun cancel() {
            currentCall?.cancel()
            currentCall = null
        }
    }

    private class OkHttpClientBuilder(private val cancellableHttpClient: CancellableHttpClient) : HttpClientBuilder {

        private var connectTimeout: Duration = Duration.ofSeconds(30)
        private var readTimeout: Duration = Duration.ofSeconds(5)

        override fun connectTimeout(): Duration = connectTimeout

        override fun connectTimeout(timeout: Duration?): HttpClientBuilder {
            timeout?.let { this.connectTimeout = it }
            return this
        }

        override fun readTimeout(): Duration = readTimeout

        override fun readTimeout(timeout: Duration?): HttpClientBuilder {
            timeout?.let { this.readTimeout = it }
            return this
        }

        override fun build(): HttpClient? {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(connectTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .readTimeout(readTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .writeTimeout(readTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .build()

            return OkHttpClientAdapter(okHttpClient, cancellableHttpClient)
        }
    }

    private class OkHttpClientAdapter(
        private val okHttpClient: OkHttpClient,
        private val cancellableHttpClient: CancellableHttpClient
    ) : HttpClient {

        override fun execute(request: HttpRequest): SuccessfulHttpResponse {
            val call = okHttpClient.newCall(convertToOkHttpRequest(request))
            cancellableHttpClient.setCurrentCall(call)

            return try {
                val response = call.execute()
                convertToLangChain4jResponse(response)
            } catch (e: IOException) {
                throw RuntimeException("HTTP request failed", e)
            }
        }

        override fun execute(
            request: HttpRequest?,
            parser: ServerSentEventParser?,
            listener: ServerSentEventListener?
        ) {
            if (request == null || parser == null || listener == null) {
                throw IllegalArgumentException("Request, parser, and listener cannot be null")
            }

            val call = okHttpClient.newCall(convertToOkHttpRequest(request))
            cancellableHttpClient.setCurrentCall(call)

            try {
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        listener.onError(RuntimeException("SSE request failed", e))
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (!response.isSuccessful) {
                            listener.onError(RuntimeException("SSE request failed with status: ${response.code}"))
                            return
                        }

                        response.body?.let { responseBody ->
                            try {
                                responseBody.byteStream().use { inputStream ->
                                    // The parser will read from the InputStream and call the listener methods directly
                                    parser.parse(inputStream, listener)
                                }
                            } catch (e: Exception) {
                                if (!call.isCanceled()) {
                                    listener.onError(RuntimeException("Error reading SSE stream", e))
                                }
                            }
                        } ?: run {
                            listener.onError(RuntimeException("No response body received"))
                        }
                    }
                })
            } catch (e: Exception) {
                listener.onError(RuntimeException("Failed to execute SSE request", e))
            }
        }

        private fun convertToOkHttpRequest(request: HttpRequest): Request {
            val builder = Request.Builder().url(request.url())
            request.headers().forEach { (name, values) ->
                values.forEach { value ->
                    builder.addHeader(name, value)
                }
            }

            when (request.method()) {
                dev.langchain4j.http.client.HttpMethod.GET -> builder.get()
                dev.langchain4j.http.client.HttpMethod.POST -> {
                    val body = request.body()?.let { bodyContent ->
                        val contentType = request.headers()["Content-Type"]?.firstOrNull()
                            ?: "application/json"
                        bodyContent.toRequestBody(contentType.toMediaType())
                    } ?: "".toRequestBody()
                    builder.post(body)
                }
                dev.langchain4j.http.client.HttpMethod.DELETE -> builder.delete()
                else -> throw IllegalArgumentException("Unsupported HTTP method: ${request.method()}")
            }

            return builder.build()
        }

        private fun convertToLangChain4jResponse(response: Response): SuccessfulHttpResponse {
            val body = response.body?.string() ?: ""
            val headers = mutableMapOf<String, List<String>>()
            response.headers.forEach { (name, value) ->
                headers[name] = headers.getOrDefault(name, emptyList()) + value
            }

            return SuccessfulHttpResponse.builder()
                .statusCode(response.code)
                .headers(headers)
                .body(body)
                .build()
        }
    }
}
