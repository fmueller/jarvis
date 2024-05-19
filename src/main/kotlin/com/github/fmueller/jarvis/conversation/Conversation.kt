package com.github.fmueller.jarvis.conversation

import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import java.time.LocalDateTime

enum class Role {
    ASSISTANT, USER
}

data class Message(val role: Role, val content: String, val createdAt: LocalDateTime = LocalDateTime.now())

class Conversation {

    private val messages = mutableListOf<Message>()
    private val propertyChangeSupport = PropertyChangeSupport(this)

    fun addMessage(message: Message) {
        val oldMessages = ArrayList(messages)
        messages.add(message)
        propertyChangeSupport.firePropertyChange("messages", oldMessages, ArrayList(messages))
    }

    fun getMessages() = messages.toList()

    fun addPropertyChangeListener(listener: PropertyChangeListener) {
        propertyChangeSupport.addPropertyChangeListener(listener)
    }
}
