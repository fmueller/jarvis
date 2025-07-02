package com.github.fmueller.jarvis.toolWindow

import com.github.fmueller.jarvis.conversation.Conversation
import com.github.fmueller.jarvis.conversation.Role
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.runInEdtAndWait

class ConversationWindowFactoryTest : BasePlatformTestCase() {

    private lateinit var conversation: Conversation
    private lateinit var window: ConversationWindowFactory.ConversationWindow

    override fun setUp() {
        super.setUp()
        conversation = Conversation()
        window = ConversationWindowFactory.ConversationWindow(project, conversation, true)
    }

    override fun tearDown() {
        conversation.dispose()
        super.tearDown()
    }

    private fun createEvent(): AnActionEvent {
        return AnActionEvent(
            null,
            DataContext.EMPTY_CONTEXT,
            "",
            Presentation(),
            ActionManager.getInstance(),
            0
        )
    }

    fun `test send button disabled for empty input`() {
        val event = createEvent()
        runInEdtAndWait {
            window.inputArea.text = ""
            window.sendAction.update(event)
        }
        assertFalse(event.presentation.isEnabled)
    }

    fun `test send button sends message`() {
        runInEdtAndWait {
            window.inputArea.text = "/help"
            window.sendAction.actionPerformed(createEvent())
        }
        assertEquals(3, conversation.messages.size)
        val userMessage = conversation.messages[1]
        assertEquals(Role.USER, userMessage.role)
        assertEquals("/help", userMessage.content)
    }

    fun `test send button cancels chat when in progress`() {
        conversation.markChatInProgressForTesting(true)
        runInEdtAndWait {
            window.sendAction.actionPerformed(createEvent())
        }
        val last = conversation.messages.last()
        assertEquals(Role.INFO, last.role)
        assertTrue(last.content.contains("cancelled"))
        assertTrue(window.inputArea.isEnabled)
    }

    fun `test send button presentation when in progress`() {
        conversation.markChatInProgressForTesting(true)
        val event = createEvent()
        runInEdtAndWait {
            window.sendAction.update(event)
        }
        assertEquals("Stop", event.presentation.text)
        assertEquals(AllIcons.Actions.Suspend, event.presentation.icon)
        assertTrue(event.presentation.isEnabled)
    }
}
