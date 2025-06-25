package com.github.fmueller.jarvis.conversation

import com.github.fmueller.jarvis.commands.SlashCommandParser
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.github.fmueller.jarvis.ai.OllamaService
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import java.time.LocalDateTime

enum class Role {

    ASSISTANT,
    USER,
    INFO;

    override fun toString() = name.lowercase()
}

data class Code(val content: String, val language: Language = Language.ANY)

data class CodeContext(val projectName: String, val selected: Code? = null) {

    fun hasSelectedCode() = selected != null
}

data class Message(
    val role: Role,
    val content: String,
    val codeContext: CodeContext? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
) {

    companion object {

        val HELP_MESSAGE = fromAssistant(
            """
            I'm Jarvis, your personal coding assistant. You can ask me anything. To make me work properly, please install and run Ollama locally.
            
            Available commands:

            - ```/help``` or ```/?``` - Shows this help message
            - ```/new``` - Starts a new conversation
            - ```/plain``` - Sends a chat message without code context
            - ```/copy``` - Copies the conversation to the clipboard
            - ```/model <modelName>``` - Changes the model to use (`default` is `qwen3:4b`)
            - ```/host <host>``` - Sets the Ollama host (`default` is `http://localhost:11434`)
            """.trimIndent()
        )

        fun fromAssistant(content: String) = Message(Role.ASSISTANT, content)
        fun fromUser(content: String) = Message(Role.USER, content)
        fun info(content: String) = Message(Role.INFO, content)
    }

    fun contentWithClosedTrailingCodeBlock(): String {
        if (isHelpMessage()) {
            return content.trim()
        }

        val normalized = normalizeCodeBlockDelimiters(content)
        return closeOpenMarkdownCodeBlockAtTheEndOfContent(normalized).trim()
    }

    fun isHelpMessage() = this == HELP_MESSAGE

    private fun closeOpenMarkdownCodeBlockAtTheEndOfContent(text: String): String {
        val delimiterPattern = Regex("""^\s*`{2,}""")
        val delimiterCount = text.lines().count { delimiterPattern.containsMatchIn(it) }
        return if (delimiterCount % 2 != 0) {
            "$text\n```"
        } else {
            text
        }
    }

    private fun normalizeCodeBlockDelimiters(text: String): String {
        return text.lines().joinToString("\n") { line ->
            val trimmed = line.trimStart()
            if (trimmed.startsWith("``") && !trimmed.startsWith("```") && !trimmed.startsWith("````")) {
                val indent = line.substring(0, line.length - trimmed.length)
                indent + "```" + trimmed.drop(2)
            } else {
                line
            }
        }
    }
}

// as long as we don't have conversation history persistence,
// we can keep the conversation in memory
// and declare it as a project-level service
@Service(Service.Level.PROJECT)
class Conversation : Disposable {

    companion object {

        fun greetingMessage(): Message {
            return Message.fromAssistant("Hello! How can I help you?")
        }
    }

    private var _messages = mutableListOf<Message>()
    val messages get() = _messages.toList()

    @Volatile
    private var chatJob: Job? = null

    private val _messageBeingGenerated = StringBuilder()

    private val propertyChangeSupport = PropertyChangeSupport(this)

    init {
        addMessage(greetingMessage())
    }

    fun isChatInProgress(): Boolean = chatJob?.isActive == true

    fun cancelChat() {
        chatJob?.cancel()
        OllamaService.cancelRequest()
    }

    fun startChat(message: Message, scope: CoroutineScope): Job {
        chatJob?.cancel()

        val job = scope.launch {
            try {
                chat(message)
            } finally {
                chatJob = null
                propertyChangeSupport.firePropertyChange("chatInProgress", true, false)
            }
        }

        chatJob = job
        propertyChangeSupport.firePropertyChange("chatInProgress", false, true)
        return job
    }

    suspend fun chat(message: Message): Conversation {
        addMessage(message)
        return SlashCommandParser.parse(message.content).run(this)
    }

    fun getLastUserMessage(): Message? = _messages.lastOrNull { it.role == Role.USER }

    fun isFirstUserMessage(): Boolean =
        _messages.count {
            it.role == Role.USER &&
                !it.content.startsWith("/model ") &&
                !it.content.startsWith("/host ")
        } == 1

    fun addToMessageBeingGenerated(text: String) {
        val old = _messageBeingGenerated.toString()
        _messageBeingGenerated.append(text)
        propertyChangeSupport.firePropertyChange("messageBeingGenerated", old, _messageBeingGenerated.toString())
    }

    fun addMessage(message: Message) {
        clearMessageBeingGenerated()
        val oldMessages = ArrayList(_messages)
        _messages.add(message)
        propertyChangeSupport.firePropertyChange("messages", oldMessages, ArrayList(_messages))
    }

    fun clearMessages() {
        clearMessageBeingGenerated()
        val oldMessages = ArrayList(_messages)
        _messages.clear()
        _messages.add(greetingMessage())
        propertyChangeSupport.firePropertyChange("messages", oldMessages, ArrayList(_messages))
    }

    fun addPropertyChangeListener(listener: PropertyChangeListener) {
        propertyChangeSupport.addPropertyChangeListener(listener)
    }

    override fun dispose() {
        propertyChangeSupport.propertyChangeListeners.forEach {
            propertyChangeSupport.removePropertyChangeListener(it)
        }
    }

    private fun clearMessageBeingGenerated() {
        val old = _messageBeingGenerated.toString()
        _messageBeingGenerated.clear()
        propertyChangeSupport.firePropertyChange("messageBeingGenerated", old, "")
    }
}
