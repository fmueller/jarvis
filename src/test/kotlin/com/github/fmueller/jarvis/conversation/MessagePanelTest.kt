package com.github.fmueller.jarvis.conversation

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.components.JBLabel
import javax.swing.JButton
import javax.swing.JPanel

class MessagePanelTest : BasePlatformTestCase() {

    private lateinit var messagePanel: MessagePanel

    override fun setUp() {
        super.setUp()
        messagePanel = MessagePanel(Message.fromAssistant("Hello, I am Jarvis."), project, false)
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
        messagePanel.message = Message.fromAssistant("Hi")

        assertEquals("Hi", messagePanel.message.content)
    }

    fun `test code block is rendered correctly`() {
        messagePanel.message = Message.fromAssistant("```kotlin\nprintln(\"Hello, World!\")\n```")

        assertEquals(1, messagePanel.parsed.size)
        assertTrue(messagePanel.parsed[0] is MessagePanel.Code)

        val parsedCode = messagePanel.parsed[0] as MessagePanel.Code
        assertEquals("kotlin", parsedCode.languageId)
        assertEquals("println(\"Hello, World!\")", parsedCode.content)
    }

    fun `test multiple code blocks are rendered correctly`() {
        messagePanel.message =
            Message.fromAssistant("```kotlin\nprintln(\"Hello, World!\")\n```\n```java\nSystem.out.println(\"Hello, World!\")\n```")

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
        messagePanel.message = Message.fromAssistant("```kotlin\nprintln(\"Hello, World!\")")

        assertEquals(1, messagePanel.parsed.size)
        assertTrue(messagePanel.parsed[0] is MessagePanel.Code)

        val parsedCode = messagePanel.parsed[0] as MessagePanel.Code
        assertEquals("kotlin", parsedCode.languageId)
        assertEquals("println(\"Hello, World!\")", parsedCode.content)
    }

    fun `test indented closing code block is rendered correctly`() {
        messagePanel.message = Message.fromAssistant("```kotlin\nprintln(\"Hello, World!\")\n    ```")

        assertEquals(1, messagePanel.parsed.size)
        assertTrue(messagePanel.parsed[0] is MessagePanel.Code)

        val parsedCode = messagePanel.parsed[0] as MessagePanel.Code
        assertEquals("kotlin", parsedCode.languageId)
        assertEquals("println(\"Hello, World!\")", parsedCode.content)
    }

    fun `test code block without preceding blank line is parsed correctly`() {
        messagePanel.message = Message.fromAssistant("Here is some code:\n```kotlin\nprintln(\"Hello, World!\")\n```")

        assertEquals(2, messagePanel.parsed.size)
        assertTrue(messagePanel.parsed[0] is MessagePanel.Content)
        assertTrue(messagePanel.parsed[1] is MessagePanel.Code)

        val parsedCode = messagePanel.parsed[1] as MessagePanel.Code
        assertEquals("kotlin", parsedCode.languageId)
        assertEquals("println(\"Hello, World!\")", parsedCode.content)
    }

    fun `test empty code block is rendered correctly as code block`() {
        messagePanel.message = Message.fromAssistant("```kotlin\n\n```")

        assertEquals(1, messagePanel.parsed.size)
        assertTrue(messagePanel.parsed[0] is MessagePanel.Code)

        val parsedCode = messagePanel.parsed[0] as MessagePanel.Code
        assertEquals("kotlin", parsedCode.languageId)
        assertEquals("", parsedCode.content)
    }

    fun `test multiple message updates`() {
        messagePanel.message = Message.fromAssistant("Hello World:\n\n")
        messagePanel.message = Message.fromAssistant("Hello World:\n\n```")
        messagePanel.message = Message.fromAssistant("Hello World:\n\n```kotlin")
        messagePanel.message = Message.fromAssistant("Hello World:\n\n```kotlin\n")
        messagePanel.message = Message.fromAssistant("Hello World:\n\n```kotlin\nprintln(\"Hello, World!\")")
        messagePanel.message = Message.fromAssistant("Hello World:\n\n```kotlin\nprintln(\"Hello, World!\")\n```")
        messagePanel.message =
            Message.fromAssistant("Hello World:\n\n```kotlin\nprintln(\"Hello, World!\")\n```\n\nWhat's next?")

        assertEquals(3, messagePanel.parsed.size)
        assertTrue(messagePanel.parsed[0] is MessagePanel.Content)
        assertTrue(messagePanel.parsed[1] is MessagePanel.Code)
        assertTrue(messagePanel.parsed[2] is MessagePanel.Content)

        val parsedCode = messagePanel.parsed[1] as MessagePanel.Code
        assertEquals("kotlin", parsedCode.languageId)
        assertEquals("println(\"Hello, World!\")", parsedCode.content)
    }

    fun `test info message label`() {
        val panel = MessagePanel(Message.info("Downloading"), project, false)
        val label = panel.getComponent(0) as JBLabel
        assertEquals("Info", label.text)
    }

    fun `test reasoning block is parsed`() {
        messagePanel.message = Message.fromAssistant("<think>Reason</think>Hello")

        assertEquals(1, messagePanel.parsed.size)
        assertTrue(messagePanel.reasoningMessagePanel?.message?.content?.contains("Reason") ?: false)
    }

    fun `test reasoning panel toggle functionality`() {
        // Set a message with reasoning to make the reasoning panel visible
        messagePanel.message = Message.fromAssistant("<think>Reasoning content</think>Hello")

        // Get the reasoning panel components using reflection
        val reasoningPanelField = MessagePanel::class.java.getDeclaredField("reasoningPanel")
        reasoningPanelField.isAccessible = true
        val reasoningPanel = reasoningPanelField.get(messagePanel) as JPanel

        val reasoningContentPanelField = MessagePanel::class.java.getDeclaredField("reasoningContentPanel")
        reasoningContentPanelField.isAccessible = true
        val reasoningContentPanel = reasoningContentPanelField.get(messagePanel) as JPanel

        val reasoningHeaderButtonField = MessagePanel::class.java.getDeclaredField("reasoningHeaderButton")
        reasoningHeaderButtonField.isAccessible = true
        val reasoningHeaderButton = reasoningHeaderButtonField.get(messagePanel) as JButton

        // Verify initial state
        assertTrue(reasoningPanel.isVisible)
        assertFalse(reasoningContentPanel.isVisible)
        assertEquals("Reasoning", reasoningHeaderButton.text)

        // Click the button to expand the panel
        reasoningHeaderButton.doClick()

        // Verify expanded state
        assertTrue(reasoningContentPanel.isVisible)

        // Click the button again to collapse the panel
        reasoningHeaderButton.doClick()

        // Verify collapsed state
        assertFalse(reasoningContentPanel.isVisible)
    }
}
