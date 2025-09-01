package com.github.fmueller.jarvis.ai

import com.github.fmueller.jarvis.ai.http.CancellableHttpClient
import com.github.fmueller.jarvis.conversation.CodeContext
import com.github.fmueller.jarvis.conversation.Conversation
import com.github.fmueller.jarvis.conversation.Message
import com.intellij.lang.Language
import dev.langchain4j.memory.chat.TokenWindowChatMemory
import dev.langchain4j.model.chat.request.DefaultChatRequestParameters
import dev.langchain4j.model.ollama.OllamaStreamingChatModel
import dev.langchain4j.model.ollama.OllamaChatRequestParameters
import dev.langchain4j.service.AiServices
import dev.langchain4j.service.TokenStream
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.annotations.VisibleForTesting
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

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

    private var currentInferenceClient: CancellableHttpClient? = null

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
            resetParameters()
            assistant = createAiService()
        }

    /**
     * Maximum number of tokens sent as context to Ollama.
     */
    var contextWindowSize: Int = 4096
        set(value) {
            field = value
            assistant = createAiService()
        }

    var temperature: Double = 0.7

    var topP: Double = 0.9

    var topK: Int = 40

    var maxTokens: Int? = null

    var repeatPenalty: Double = 1.1

    var seed: Int? = null

    var stopSequences: List<String> = emptyList()

    var presencePenalty: Double? = null

    var frequencyPenalty: Double? = null

    private fun resetParameters() {
        temperature = 0.7
        topP = 0.9
        topK = 40
        maxTokens = null
        repeatPenalty = 1.1
        seed = null
        stopSequences = emptyList()
        presencePenalty = null
        frequencyPenalty = null
    }

    private var assistant = createAiService()

    fun clearChatMemory() {
        assistant = createAiService()
    }

    fun setParameter(name: String, value: String): String? {
        return when (name.lowercase()) {
            "temperature", "t" -> {
                val v = value.toDoubleOrNull()
                if (v != null && v in 0.0..2.0) {
                    temperature = v
                    null
                } else {
                    "temperature must be between 0.0 and 2.0"
                }
            }
            "top_p", "p" -> {
                val v = value.toDoubleOrNull()
                if (v != null && v in 0.0..1.0) {
                    topP = v
                    null
                } else {
                    "top_p must be between 0.0 and 1.0"
                }
            }
            "top_k", "k" -> {
                val v = value.toIntOrNull()
                if (v != null && v in 1..100) {
                    topK = v
                    null
                } else {
                    "top_k must be between 1 and 100"
                }
            }
            "max_tokens", "m" -> {
                val v = value.toIntOrNull()
                if (v != null && v in 1..4096) {
                    maxTokens = v
                    null
                } else {
                    "max_tokens must be between 1 and 4096"
                }
            }
            "repeat_penalty", "r" -> {
                val v = value.toDoubleOrNull()
                if (v != null && v in 0.0..2.0) {
                    repeatPenalty = v
                    null
                } else {
                    "repeat_penalty must be between 0.0 and 2.0"
                }
            }
            "seed", "s" -> {
                val v = value.toIntOrNull()
                if (v != null) {
                    seed = v
                    null
                } else {
                    "seed must be an integer"
                }
            }
            "num_ctx", "c" -> {
                val v = value.toIntOrNull()
                if (v != null && v in 512..32768) {
                    contextWindowSize = v
                    null
                } else {
                    "num_ctx must be between 512 and 32768"
                }
            }
            "stop" -> {
                stopSequences = value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                null
            }
            "presence_penalty", "pp" -> "presence_penalty is not supported"
            "frequency_penalty", "fp" -> "frequency_penalty is not supported"
            else -> "unsupported parameter $name"
        }
    }

    fun getParametersInfo(): String {
        val result = StringBuilder()
        result.append("  Inference parameters\n")
        result.append("    temperature        $temperature    Controls randomness\n")
        result.append("    top_p              $topP    Nucleus sampling threshold\n")
        result.append("    top_k              $topK    Limits token candidates\n")
        result.append("    max_tokens         ${maxTokens ?: "default"}    Maximum output tokens\n")
        result.append("    repeat_penalty     $repeatPenalty    Prevents repetition\n")
        result.append("    seed               ${seed ?: "random"}    For reproducible outputs\n")
        result.append("    num_ctx            $contextWindowSize    Context window size\n")
        if (stopSequences.isNotEmpty()) {
            result.append("    stop               ${stopSequences.joinToString()}    Stop sequences\n")
        }
        result.append(
            "    presence_penalty   ${presencePenalty ?: 0.0}    Penalizes repeated topics\n",
        )
        result.append(
            "    frequency_penalty  ${frequencyPenalty ?: 0.0}    Penalizes token frequency",
        )
        return result.toString()
    }

    /**
     * Retrieve the info card of the current model from Ollama.
     *
     * The info card is obtained via the `/api/show` endpoint and returned as
     * a nicely formatted card similar to `ollama show <model>` CLI output.
     */
    fun getModelInfo(): String {
        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$host/api/show"))
                .timeout(Duration.ofSeconds(2))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"$modelName\"}"))
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                val info = formatModelInfo(response.body())
                val params = getParametersInfo()
                "$info\n\n$params"
            } else {
                "Model info request failed: status ${response.statusCode()}"
            }
        } catch (e: Exception) {
            "Model info request failed: ${e.message}"
        }
    }

    private fun formatModelInfo(jsonResponse: String): String {
        return try {
            val json = Json.parseToJsonElement(jsonResponse).jsonObject
            val result = StringBuilder()

            // Model section
            result.append(" Model\n")
            json["details"]?.jsonObject?.let { details ->
                details["family"]?.jsonPrimitive?.content?.let {
                    result.append("    architecture        $it\n")
                }
                details["parameter_size"]?.jsonPrimitive?.content?.let {
                    result.append("    parameters          $it\n")
                }
                details["quantization_level"]?.jsonPrimitive?.content?.let {
                    result.append("    quantization        $it\n")
                }
            }

            result.append("\n")

            // Parameters section
            result.append("  Parameters\n")

            json["model_info"]?.jsonObject?.get("llama.context_length")?.jsonPrimitive?.content?.let { contextLength ->
                result.append("    context length      $contextLength\n")
            }

            json["parameters"]?.let { params ->
                if (params.jsonPrimitive.isString) {
                    // Parameters is a string literal, append it with proper formatting
                    val parametersText = params.jsonPrimitive.content
                    parametersText.lines().forEach { line ->
                        val trimmedLine = line.trim()
                        if (trimmedLine.isNotEmpty()) {
                            result.append("    $trimmedLine\n")
                        }
                    }
                }
            }

            result.append("\n")

            // License section (if available)
            json["license"]?.jsonPrimitive?.content?.let { license ->
                result.append("  License\n")
                result.append("    ${license.lines().first()}")
            }

            result.toString().trimEnd()
        } catch (e: Exception) {
            "Failed to format model info: ${e.message}\n\nRaw response:\n$jsonResponse"
        }
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
                        responseInFlight.append(update)
                        conversation.addToMessageBeingGenerated(update)
                    }
                    .onCompleteResponse { response ->
                        continuation.resumeWith(Result.success(response.aiMessage().text().trim()))
                    }
                    .onError { error ->
                        if (!continuation.isCancelled) {
                            val errorMessage = StringBuilder()
                                .appendLine()
                                .appendLine()
                                .appendLine("An error occurred while processing the message.")
                                .appendLine()
                                .append("Error: ")
                                .append(error.message ?: "Unknown error")
                                .toString()
                            conversation.addToMessageBeingGenerated(errorMessage)
                        }
                        cancelCurrentRequest()
                        continuation.cancel(Exception(error.message))
                    }

                continuation.invokeOnCancellation {
                    cancelCurrentRequest()
                }

                tokenStream.start()
            }
        } catch (e: Exception) {
            val errorMessage = StringBuilder()
                .appendLine()
                .appendLine()
                .appendLine("An error occurred while processing the message.")
                .appendLine()
                .append("Error: ")
                .append(e.message)
                .toString()

            val job = currentCoroutineContext()[Job]
            if (job?.isCancelled == false) {
                conversation.addToMessageBeingGenerated(errorMessage)
            }

            responseInFlight.append(errorMessage).toString().trim()
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
                .uri(URI.create(host))
                .timeout(Duration.ofSeconds(2))
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            response.statusCode() == 200
        } catch (e: Exception) {
            false
        }
    }

    @VisibleForTesting
    fun isModelAvailable(): Boolean {
        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$host/api/tags"))
                .timeout(Duration.ofSeconds(2))
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
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
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$host/api/pull"))
                .timeout(Duration.ofSeconds(2))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"$modelName\"}"))
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
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
        if (isModelAvailable()) {
            return true
        }

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
        currentInferenceClient = CancellableHttpClient()

        val paramsBuilder = OllamaChatRequestParameters.builder()
            .keepAlive(Duration.ofMinutes(5).toSeconds().toInt())
            .temperature(temperature)
            .topP(topP)
            .topK(topK)
            .maxOutputTokens(maxTokens)
            .presencePenalty(presencePenalty)
            .frequencyPenalty(frequencyPenalty)
        if (stopSequences.isNotEmpty()) {
            paramsBuilder.stopSequences(stopSequences)
        }

        return AiServices
            .builder(Assistant::class.java)
            .streamingChatModel(
                OllamaStreamingChatModel.builder()
                    .httpClientBuilder(currentInferenceClient!!.getHttpClientBuilder())
                    .timeout(Duration.ofMinutes(5))
                    .baseUrl(host)
                    .modelName(modelName)
                    .numCtx(contextWindowSize)
                    .repeatPenalty(repeatPenalty)
                    .seed(seed)
                    .defaultRequestParameters(paramsBuilder.build())
                    .build()
            )
            .systemMessageProvider { chatMemoryId -> systemPrompt }
            .chatMemory(
                TokenWindowChatMemory.builder()
                    .maxTokens(contextWindowSize, SimpleTokenizer())
                    .build()
            )
            .build()
    }

    private fun cancelCurrentRequest() {
        currentInferenceClient?.cancel()
    }
}
