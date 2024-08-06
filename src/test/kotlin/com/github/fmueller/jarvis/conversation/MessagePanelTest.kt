package com.github.fmueller.jarvis.conversation

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MessagePanelTest : BasePlatformTestCase() {

    private lateinit var messagePanel: MessagePanel

    override fun setUp() {
        super.setUp()
        messagePanel = MessagePanel(Message(Role.ASSISTANT, "Hello, I am Jarvis."), project)
    }

    fun `test updateUI re-renders the initial message`() {
        messagePanel.updateUI()

        assertEquals("Hello, I am Jarvis.", messagePanel.message.content)
    }

    fun `test updating the message works`() {
        messagePanel.message = Message(Role.ASSISTANT, "Hi")

        assertEquals("Hi", messagePanel.message.content)
    }
}