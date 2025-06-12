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
}
