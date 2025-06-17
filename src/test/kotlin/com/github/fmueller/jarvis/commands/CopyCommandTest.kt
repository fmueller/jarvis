package com.github.fmueller.jarvis.commands

import com.github.fmueller.jarvis.conversation.Code
import com.github.fmueller.jarvis.conversation.CodeContext
import com.github.fmueller.jarvis.conversation.Conversation
import com.github.fmueller.jarvis.conversation.Message
import com.github.fmueller.jarvis.conversation.Role
import com.intellij.lang.Language
import junit.framework.TestCase

class CopyCommandTest : TestCase() {

    fun `test buildPrompt contains conversation and code`() {
        val conversation = Conversation()
        conversation.addMessage(Message.fromAssistant("Hi"))
        conversation.addMessage(Message(Role.USER, "Explain", CodeContext("project", Code("println()", Language.ANY))))
        conversation.addMessage(Message.fromAssistant("Sure"))

        val prompt = CopyCommand().buildPrompt(conversation)

        assertTrue(prompt.contains("Explain"))
        assertTrue(prompt.contains("println()"))
    }

    fun `test buildPrompt omits code section when none`() {
        val conversation = Conversation()
        conversation.addMessage(Message.fromAssistant("Hi"))
        conversation.addMessage(Message(Role.USER, "Hello"))

        val prompt = CopyCommand().buildPrompt(conversation)

        assertFalse(prompt.contains("Code Context"))
        assertFalse(prompt.contains("No code context"))
    }

    fun `test buildPrompt omits code for plain message`() {
        val conversation = Conversation()
        conversation.addMessage(Message.fromAssistant("Hi"))
        conversation.addMessage(
            Message(
                Role.USER,
                "/plain Explain",
                CodeContext("project", Code("println()", Language.ANY))
            )
        )

        val prompt = CopyCommand().buildPrompt(conversation)

        assertFalse(prompt.contains("Code Context"))
        assertFalse(prompt.contains("println()"))
    }

    fun `test buildPrompt ignores commands and info messages`() {
        val conversation = Conversation()
        conversation.addMessage(Message.fromAssistant("Hi"))
        conversation.addMessage(Message(Role.USER, "/model llama"))
        conversation.addMessage(Message.info("Model changed"))
        conversation.addMessage(Message(Role.USER, "/host http://1.2.3.4"))
        conversation.addMessage(Message.info("Host changed"))
        conversation.addMessage(Message(Role.USER, "/help"))
        conversation.addMessage(Message.HELP_MESSAGE)
        conversation.addMessage(Message(Role.USER, "/copy"))
        conversation.addMessage(Message.info("Conversation copied"))
        conversation.addMessage(Message(Role.USER, "Explain"))
        conversation.addMessage(Message.fromAssistant("Sure"))

        val prompt = CopyCommand().buildPrompt(conversation)

        assertTrue(prompt.contains("Explain"))
        assertTrue(prompt.contains("Sure"))
        assertFalse(prompt.contains("Model changed"))
        assertFalse(prompt.contains("/model"))
        assertFalse(prompt.contains("/host"))
        assertFalse(prompt.contains("/help"))
        assertFalse(prompt.contains("Conversation copied"))
    }
}
