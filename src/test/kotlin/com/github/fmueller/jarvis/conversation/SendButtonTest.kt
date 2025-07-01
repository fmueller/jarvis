package com.github.fmueller.jarvis.conversation

import com.intellij.icons.AllIcons
import junit.framework.TestCase
import javax.swing.SwingUtilities

class SendButtonTest : TestCase() {

    fun `test initial state is send`() {
        val button = SendButton({}, {})
        assertSame(AllIcons.Actions.Execute, button.icon)
        assertEquals("Send message", button.toolTipText)
    }

    fun `test setSending updates to stop`() {
        val button = SendButton({}, {})
        SwingUtilities.invokeAndWait {
            button.setSending(true)
        }
        assertSame(AllIcons.Actions.Suspend, button.icon)
        assertEquals("Stop generating response", button.toolTipText)
    }

    fun `test clicking calls appropriate action`() {
        var sendCalled = false
        var stopCalled = false
        val button = SendButton({ sendCalled = true }, { stopCalled = true })

        // click in send mode
        SwingUtilities.invokeAndWait { button.doClick() }
        assertTrue(sendCalled)
        assertFalse(stopCalled)

        // switch to stop mode
        SwingUtilities.invokeAndWait { button.setSending(true) }
        SwingUtilities.invokeAndWait { button.doClick() }
        assertTrue(stopCalled)
    }
}
