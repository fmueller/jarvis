package com.github.fmueller.jarvis.conversation

import junit.framework.TestCase
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport

class ConversationTest : TestCase() {

    private lateinit var conversation: Conversation

    override fun setUp() {
        super.setUp()
        conversation = Conversation()
    }

    override fun tearDown() {
        conversation.dispose()
        super.tearDown()
    }

    fun `test dispose removes property change listeners`() {
        var called = false
        val listener1 = PropertyChangeListener { called = true }
        val listener2 = PropertyChangeListener { called = true }
        conversation.addPropertyChangeListener(listener1)
        conversation.addPropertyChangeListener(listener2)

        val field = conversation.javaClass.getDeclaredField("propertyChangeSupport")
        field.isAccessible = true
        val pcs = field.get(conversation) as PropertyChangeSupport
        assertEquals(2, pcs.propertyChangeListeners.size)

        conversation.dispose()
        conversation.addMessage(Message.fromAssistant("test"))
        assertFalse(called)
        assertEquals(0, pcs.propertyChangeListeners.size)
    }
}
