package com.github.fmueller.jarvis.commands

import com.github.fmueller.jarvis.conversation.Conversation
import com.github.fmueller.jarvis.conversation.Message
import com.github.fmueller.jarvis.conversation.Role
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking

class NewConversationCommandTest : TestCase() {

    private lateinit var conversation: Conversation
    private lateinit var command: NewConversationCommand

    override fun setUp() {
        super.setUp()
        conversation = Conversation()
        command = NewConversationCommand()
    }

    override fun tearDown() {
        conversation.dispose()
        super.tearDown()
    }

    fun `test run clears conversation messages`() = runBlocking {
        conversation.addMessage(Message.fromAssistant("First message"))
        conversation.addMessage(Message.fromAssistant("Second message"))

        val result = command.run(conversation)

        assertSame(conversation, result)
        assertEquals(1, result.messages.size)

        val greetingMessage = Conversation.greetingMessage()
        assertEquals(greetingMessage.content, result.messages.first().content)
        assertEquals(greetingMessage.role, result.messages.first().role)
    }

    fun `test run with a new conversation`() = runBlocking {
        val result = command.run(conversation)

        assertEquals(1, result.messages.size)
        assertEquals("Hello! How can I help you?", result.messages.first().content)
        assertEquals(Role.ASSISTANT, result.messages.first().role)
    }
}
