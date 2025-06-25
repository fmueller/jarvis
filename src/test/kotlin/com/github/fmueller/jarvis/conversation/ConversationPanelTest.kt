package com.github.fmueller.jarvis.conversation

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.runInEdtAndGet
import com.intellij.util.ui.UIUtil

class ConversationPanelTest : BasePlatformTestCase() {

    private lateinit var conversation: Conversation
    private lateinit var conversationPanel: ConversationPanel

    override fun setUp() {
        super.setUp()
        conversation = Conversation()
        conversationPanel = runInEdtAndGet {
            ConversationPanel(conversation, project)
        }
    }

    override fun tearDown() {
        conversationPanel.dispose()
        super.tearDown()
    }

    fun `test updateMessageInProgress creates a new message panel when none exists`() {
        assertNull(conversationPanel.updatePanel)

        runInEdtAndGet {
            conversationPanel.updateMessageInProgress("Test message")
        }

        val updatePanel = conversationPanel.updatePanel
        assertNotNull("updatePanel should not be null after updateMessageInProgress", updatePanel)
        assertEquals("Test message", updatePanel!!.message.content)
    }

    fun `test updateMessageInProgress updates existing message panel`() {
        runInEdtAndGet {
            conversationPanel.updateMessageInProgress("Initial message")
        }

        val initialUpdatePanel = conversationPanel.updatePanel!!
        runInEdtAndGet {
            conversationPanel.updateMessageInProgress("Updated message")
        }

        // The updatePanel should be the same instance
        val updatedUpdatePanel = conversationPanel.updatePanel!!
        assertSame("updatePanel should be the same instance", initialUpdatePanel, updatedUpdatePanel)

        // But the message content should be updated
        assertEquals("Updated message", updatedUpdatePanel.message.content)
    }

    fun `test updateSmooth adds new messages`() {
        val initialCount = runInEdtAndGet { conversationPanel.panel.componentCount }

        val messages = listOf(
            Conversation.greetingMessage(),
            Message.fromAssistant("New message")
        )

        runInEdtAndGet {
            conversationPanel.updateSmooth(messages)
        }

        val newCount = runInEdtAndGet { conversationPanel.panel.componentCount }
        assertTrue("Panel should have more components", newCount > initialCount)

        val lastComponent = runInEdtAndGet { conversationPanel.panel.getComponent(newCount - 1) as MessagePanel }
        assertEquals("New message", lastComponent.message.content)
    }

    fun `test updateSmooth converts in-progress message to permanent`() {
        runInEdtAndGet {
            conversationPanel.updateMessageInProgress("In progress")
        }

        val messages = listOf(
            Conversation.greetingMessage(),
            Message.fromAssistant("In progress")
        )

        runInEdtAndGet {
            conversationPanel.updateSmooth(messages)
        }

        assertNull("updatePanel should be null after conversion", conversationPanel.updatePanel)

        val newCount = runInEdtAndGet { conversationPanel.panel.componentCount }
        // We don't assert the exact count since the implementation might change,
        // but we do verify that the last component has the expected content
        val lastComponent = runInEdtAndGet { conversationPanel.panel.getComponent(newCount - 1) as MessagePanel }
        assertEquals("In progress", lastComponent.message.content)
    }

    fun `test updateSmooth adds multiple messages`() {
        val initialCount = runInEdtAndGet { conversationPanel.panel.componentCount }

        val messages = listOf(
            Conversation.greetingMessage(),
            Message.fromAssistant("Message 1"),
            Message.fromUser("Message 2"),
            Message.fromAssistant("Message 3")
        )

        runInEdtAndGet {
            conversationPanel.updateSmooth(messages)
        }

        val newCount = runInEdtAndGet { conversationPanel.panel.componentCount }
        assertTrue("Panel should have more components after update", newCount > initialCount)

        // Find the indices of our new message panels
        val indices = (0 until newCount).filter { i ->
            val component = runInEdtAndGet { conversationPanel.panel.getComponent(i) as MessagePanel }
            component.message.content in listOf("Message 1", "Message 2", "Message 3")
        }

        assertEquals("Should find all three new messages", 3, indices.size)

        // Get the message panels by their indices
        val messagePanels = indices.map { i ->
            runInEdtAndGet { conversationPanel.panel.getComponent(i) as MessagePanel }
        }

        // Verify that all three messages are present
        val messageContents = messagePanels.map { it.message.content }
        assertTrue("Should contain Message 1", messageContents.contains("Message 1"))
        assertTrue("Should contain Message 2", messageContents.contains("Message 2"))
        assertTrue("Should contain Message 3", messageContents.contains("Message 3"))
    }

    fun `test constructor renders existing conversation messages`() {
        val conversation = Conversation().apply {
            clearMessages()
            addMessage(Message.fromUser("User message"))
            addMessage(Message.fromAssistant("Assistant message"))
        }

        val panel = ConversationPanel(conversation, project)

        UIUtil.invokeAndWaitIfNeeded {
            val expectedCount = conversation.messages.size
            assertEquals(
                "Panel should render $expectedCount messages from constructor",
                expectedCount,
                panel.panel.componentCount
            )
            conversation.messages.forEachIndexed { index, message ->
                val component = panel.panel.getComponent(index)
                assertTrue("Component at $index should be a MessagePanel", component is MessagePanel)
                assertEquals("Rendered message at $index should match",
                    message, (component as MessagePanel).message)
            }
        }
    }

    fun `test updateSmooth clears and re-renders when messages decrease`() {
        val initialMessages = listOf(
            Message.fromUser("First"),
            Message.fromAssistant("Second"),
            Message.fromUser("Third")
        )
        runInEdtAndGet {
            conversationPanel.updateSmooth(initialMessages)
        }
        val initialCount = runInEdtAndGet { conversationPanel.panel.componentCount }
        assertEquals(
            "Panel should have ${initialMessages.size} components after initial update",
            initialMessages.size,
            initialCount
        )

        // Now simulate clearing to a smaller set of messages,
        // e.g., when a conversation gets cleared
        val fewerMessages = listOf(
            Message.fromAssistant("OnlyOne")
        )
        runInEdtAndGet {
            conversationPanel.updateSmooth(fewerMessages)
        }
        val newCount = runInEdtAndGet { conversationPanel.panel.componentCount }
        assertEquals(
            "Panel should have ${fewerMessages.size} component after clearing and re-render",
            fewerMessages.size,
            newCount
        )

        assertNull("updatePanel should be null after panel is cleared", conversationPanel.updatePanel)

        // Verify the remaining message is rendered correctly
        runInEdtAndGet {
            val comp = conversationPanel.panel.getComponent(0)
            assertTrue("Component should be a MessagePanel", comp is MessagePanel)
            assertEquals("Rendered message should match the new list",
                fewerMessages[0], (comp as MessagePanel).message)
        }
    }
}
