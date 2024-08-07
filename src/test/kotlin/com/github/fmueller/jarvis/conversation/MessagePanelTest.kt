package com.github.fmueller.jarvis.conversation

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MessagePanelTest : BasePlatformTestCase() {

    private lateinit var messagePanel: MessagePanel

    override fun setUp() {
        super.setUp()
        messagePanel = MessagePanel(Message(Role.ASSISTANT, "Hello, I am Jarvis."), project)
    }

    override fun tearDown() {
        messagePanel.dispose()
        super.tearDown()
    }

    fun `test updateUI re-renders the initial message`() {
        messagePanel.updateUI()

        assertEquals("Hello, I am Jarvis.", messagePanel.message.content)
    }

    fun `test updating the message works`() {
        messagePanel.message = Message(Role.ASSISTANT, "Hi")

        assertEquals("Hi", messagePanel.message.content)
    }

    fun `test code block is rendered correctly`() {
        messagePanel.message = Message(Role.ASSISTANT, "```kotlin\nprintln(\"Hello, World!\")\n```")

        assertEquals(1, messagePanel.parsed.size)
        assertTrue(messagePanel.parsed[0] is MessagePanel.Code)

        val parsedCode = messagePanel.parsed[0] as MessagePanel.Code
        assertEquals("kotlin", parsedCode.languageId)
        assertEquals("println(\"Hello, World!\")", parsedCode.content)
    }

    fun `test multiple code blocks are rendered correctly`() {
        messagePanel.message = Message(Role.ASSISTANT, "```kotlin\nprintln(\"Hello, World!\")\n```\n```java\nSystem.out.println(\"Hello, World!\")\n```")

        assertEquals(3, messagePanel.parsed.size)
        assertTrue(messagePanel.parsed[0] is MessagePanel.Code)
        assertTrue(messagePanel.parsed[2] is MessagePanel.Code)

        val parsedCode1 = messagePanel.parsed[0] as MessagePanel.Code
        assertEquals("kotlin", parsedCode1.languageId)
        assertEquals("println(\"Hello, World!\")", parsedCode1.content)

        val parsedCode2 = messagePanel.parsed[2] as MessagePanel.Code
        assertEquals("java", parsedCode2.languageId)
        assertEquals("System.out.println(\"Hello, World!\")", parsedCode2.content)
    }

    fun `test unclosed code block is rendered correctly as code block`() {
        messagePanel.message = Message(Role.ASSISTANT, "```kotlin\nprintln(\"Hello, World!\")")

        assertEquals(1, messagePanel.parsed.size)
        assertTrue(messagePanel.parsed[0] is MessagePanel.Code)

        val parsedCode = messagePanel.parsed[0] as MessagePanel.Code
        assertEquals("kotlin", parsedCode.languageId)
        assertEquals("println(\"Hello, World!\")", parsedCode.content)
    }

    // TODO test case with multiple new messages, simulating the streaming from the LLM
}