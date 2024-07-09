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

data class CodeContext(val selected: Code)

data class Message(
    val role: Role,
    val content: String,
    val codeContext: CodeContext? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
) {

    fun asMarkdown() =
        if (shouldAddSelectedCode())
            """
            |${contentWithoutFlags()}
            |```
            |${codeContext!!.selected.content.trim()}
            |```
            """.trimMargin()
        else
            content.trim()

    private fun shouldAddSelectedCode() =
        hasCodeContext() && (content.contains("--selected-code") || content.contains("-s"))

    private fun hasCodeContext() = codeContext != null

    private fun contentWithoutFlags() = content
        .replace("--selected-code", "")
        .replace("-s", "")
        .trim()
}

// as long as we don't have conversation history persistence,
// we can keep the conversation in memory
// and declare it as a project-level service
@Service(Service.Level.PROJECT)
class Conversation : Disposable {

    private var _messages = mutableListOf<Message>()
    val messages get() = _messages.toList()

    private val propertyChangeSupport = PropertyChangeSupport(this)

    suspend fun chat(message: Message): Conversation {
        addMessage(message)
        return SlashCommandParser.parse(message.content).run(this)
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

    override fun dispose() {
        propertyChangeSupport.getPropertyChangeListeners("messages").forEach {
            propertyChangeSupport.removePropertyChangeListener(it)
        }
    }
}
