package com.github.fmueller.jarvis.conversation

import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.PlatformTestUtil
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
        conversation.dispose()
        super.tearDown()
    }

    private fun waitForEDT() {
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    }

    fun `test initial state with greeting message`() {
        assertEquals(1, conversationPanel.panel.componentCount)
        assertNull(conversationPanel.updatePanel)
    }

    fun `test disposal of message panels when conversation is cleared`() {
        conversation.addMessage(Message.fromUser("Test user message"))
        conversation.addMessage(Message.fromAssistant("Test assistant response"))
        waitForEDT()

        val initialComponentCount = conversationPanel.panel.componentCount
        assertTrue("Should have more than just greeting message", initialComponentCount > 1)

        val newConversation = Conversation()
        val newMessages = newConversation.messages // Just the greeting message

        conversationPanel.updateSmooth(newMessages)

        assertEquals(1, conversationPanel.panel.componentCount)
        assertNull(conversationPanel.updatePanel)
    }

    fun `test updatePanel becomes permanent when message is finalized`() {
        conversation.addToMessageBeingGenerated("Partial response")
        waitForEDT()

        assertNotNull("Should have updatePanel", conversationPanel.updatePanel)
        val componentCountWithUpdate = conversationPanel.panel.componentCount
        conversation.addMessage(Message.fromAssistant("Final response"))
        waitForEDT()

        assertNull("updatePanel should be cleared", conversationPanel.updatePanel)
        assertEquals("Component count should remain the same",
            componentCountWithUpdate, conversationPanel.panel.componentCount)

        val lastComponent = conversationPanel.panel.getComponent(conversationPanel.panel.componentCount - 1) as MessagePanel
        assertEquals("Last component should have the finalized message",
            "Final response", lastComponent.message.content)

        val messagePanelsField = ConversationPanel::class.java.getDeclaredField("messagePanels")
        messagePanelsField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val messagePanels = messagePanelsField.get(conversationPanel) as MutableList<MessagePanel>
        assertTrue("MessagePanels should contain the permanent panel",
            messagePanels.any { it.message.content == "Final response" })
    }

    fun `test updateMessageInProgress creates and updates panel`() {
        // Initially no update panel
        assertNull(conversationPanel.updatePanel)
        val initialComponentCount = conversationPanel.panel.componentCount

        conversationPanel.updateMessageInProgress("Partial message")

        assertNotNull("Should create updatePanel", conversationPanel.updatePanel)
        assertEquals("Should add component to panel",
            initialComponentCount + 1, conversationPanel.panel.componentCount)

        conversationPanel.updateMessageInProgress("Updated partial message")

        assertNotNull("Should still have updatePanel", conversationPanel.updatePanel)
        assertEquals("Should not add another component",
            initialComponentCount + 1, conversationPanel.panel.componentCount)
        assertEquals("Should update message content",
            "Updated partial message", conversationPanel.updatePanel!!.message.content)
    }

    fun `test updateMessageInProgress removes panel when empty`() {
        conversationPanel.updateMessageInProgress("Some content")
        assertNotNull("Should have updatePanel", conversationPanel.updatePanel)
        val componentCountWithUpdate = conversationPanel.panel.componentCount

        conversationPanel.updateMessageInProgress("")

        assertNull("Should remove updatePanel", conversationPanel.updatePanel)
        assertEquals("Should remove component from panel",
            componentCountWithUpdate - 1, conversationPanel.panel.componentCount)
    }

    fun `test dispose cleans up all message panels`() {
        conversation.addMessage(Message.fromUser("User message"))
        conversation.addMessage(Message.fromAssistant("Assistant message"))
        waitForEDT()

        conversationPanel.updateMessageInProgress("In progress...")

        val messagePanelsField = ConversationPanel::class.java.getDeclaredField("messagePanels")
        messagePanelsField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val messagePanels = messagePanelsField.get(conversationPanel) as MutableList<MessagePanel>

        assertTrue("Should have message panels", messagePanels.isNotEmpty())
        assertNotNull("Should have update panel", conversationPanel.updatePanel)

        Disposer.dispose(conversationPanel)

        assertTrue("Should clear messagePanels list", messagePanels.isEmpty())
        assertNull("Should clear updatePanel", conversationPanel.updatePanel)
    }

    fun `test smooth update handles normal message addition`() {
        val initialMessages = conversation.messages
        val initialCount = conversationPanel.panel.componentCount

        val newMessages = initialMessages + Message.fromUser("New message")
        conversationPanel.updateSmooth(newMessages)

        assertEquals("Should add one component", initialCount + 1, conversationPanel.panel.componentCount)
    }

    fun `test smooth update handles message clearing scenario`() {
        conversation.addMessage(Message.fromUser("Message 1"))
        conversation.addMessage(Message.fromAssistant("Response 1"))
        waitForEDT()

        val componentCountWithMessages = conversationPanel.panel.componentCount
        assertTrue("Should have multiple components", componentCountWithMessages > 1)

        val greetingOnly = listOf(Conversation.greetingMessage())
        conversationPanel.updateSmooth(greetingOnly)

        assertEquals("Should only have greeting message", 1, conversationPanel.panel.componentCount)
    }

    fun `test property change listener updates messages correctly`() {
        val initialCount = conversationPanel.panel.componentCount
        conversation.addMessage(Message.fromUser("Test message"))
        waitForEDT()

        assertEquals("Should have added message panel",
            initialCount + 1, conversationPanel.panel.componentCount)
    }

    fun `test property change listener handles message being generated`() {
        assertNull("Initially no update panel", conversationPanel.updatePanel)

        conversation.addToMessageBeingGenerated("Generating...")
        waitForEDT()

        assertNotNull("Should create update panel", conversationPanel.updatePanel)
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

    fun `test updateMessageInProgress with empty update removes existing panel`() {
        runInEdtAndGet {
            conversationPanel.updateMessageInProgress("Temporary")
        }
        assertNotNull("updatePanel should be non-null after initial update", conversationPanel.updatePanel)
        val initialCount = runInEdtAndGet { conversationPanel.panel.componentCount }

        runInEdtAndGet {
            conversationPanel.updateMessageInProgress("")
        }

        assertNull("updatePanel should be null after empty update", conversationPanel.updatePanel)
        val newCount = runInEdtAndGet { conversationPanel.panel.componentCount }
        assertTrue("Panel should have fewer components after removing the update panel", newCount < initialCount)
    }
}
