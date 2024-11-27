package com.github.fmueller.jarvis.conversation

import com.github.fmueller.jarvis.commands.SlashCommandParser
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import java.time.LocalDateTime

enum class Role {

    ASSISTANT,
    USER;

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
            - ```/model <modelName>``` - Changes the model to use ('llama3.1' or 'llama3.2')
            """.trimIndent()
        )

        fun fromAssistant(content: String) = Message(Role.ASSISTANT, content)
    }

    fun contentWithClosedTrailingCodeBlock() =
        if (isHelpMessage()) content.trim()
        else closeOpenMarkdownCodeBlockAtTheEndOfContent().trim()

    fun isHelpMessage() = this == HELP_MESSAGE

    private fun closeOpenMarkdownCodeBlockAtTheEndOfContent(): String {
        val tripleBackticksCount = content.split("```").size - 1
        return if (tripleBackticksCount % 2 != 0) {
            "$content\n```"
        } else {
            content
        }
    }
}

// as long as we don't have conversation history persistence,
// we can keep the conversation in memory
// and declare it as a project-level service
@Service(Service.Level.PROJECT)
class Conversation : Disposable {

    private var _messages = mutableListOf<Message>()
    val messages get() = _messages.toList()

    private val _messageBeingGenerated = StringBuilder()

    private val propertyChangeSupport = PropertyChangeSupport(this)

    suspend fun chat(message: Message): Conversation {
        addMessage(message)
        return SlashCommandParser.parse(message.content).run(this)
    }

    fun getLastUserMessage(): Message? = _messages.lastOrNull { it.role == Role.USER }

    fun isFirstUserMessage(): Boolean = _messages.count { it.role == Role.USER && !it.content.startsWith("/model ") } == 1

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
        _messages.add(Message.fromAssistant("Hello! How can I help you?"))
        propertyChangeSupport.firePropertyChange("messages", oldMessages, ArrayList(_messages))
    }

    fun addPropertyChangeListener(listener: PropertyChangeListener) {
        propertyChangeSupport.addPropertyChangeListener(listener)
    }

    override fun dispose() {
        propertyChangeSupport.getPropertyChangeListeners("messages").forEach {
            propertyChangeSupport.removePropertyChangeListener(it)
        }
    }

    private fun clearMessageBeingGenerated() {
        val old = _messageBeingGenerated.toString()
        _messageBeingGenerated.clear()
        propertyChangeSupport.firePropertyChange("messageBeingGenerated", old, "")
    }
}
