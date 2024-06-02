package com.github.fmueller.jarvis.conversation

import com.github.fmueller.jarvis.ai.OllamaService
import com.github.fmueller.jarvis.commands.SlashCommandParser
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import java.time.LocalDateTime

enum class Role {

    ASSISTANT,
    USER;

    override fun toString() = name.lowercase()
}

data class Message(val role: Role, val content: String, val createdAt: LocalDateTime = LocalDateTime.now())

class Conversation(ollamaService: OllamaService) {

    private var _messages = mutableListOf<Message>()
    val messages get() = _messages.toList()

    private val commandParser = SlashCommandParser(ollamaService)
    private val propertyChangeSupport = PropertyChangeSupport(this)

    suspend fun chat(message: String): Conversation {
        addMessage(Message(Role.USER, message.trim()))

        val command = commandParser.parse(message)
        return command.run(this)
    }

    fun addMessage(message: Message) {
        val oldMessages = ArrayList(_messages)
        _messages.add(message)
        propertyChangeSupport.firePropertyChange("messages", oldMessages, ArrayList(_messages))
    }

    fun clearMessages() {
        val oldMessages = ArrayList(_messages)
        _messages.clear()
        _messages.add(Message(Role.ASSISTANT, "Hello! How can I help you?"))
        propertyChangeSupport.firePropertyChange("messages", oldMessages, ArrayList(_messages))
    }

    fun addPropertyChangeListener(listener: PropertyChangeListener) {
        propertyChangeSupport.addPropertyChangeListener(listener)
    }
}
