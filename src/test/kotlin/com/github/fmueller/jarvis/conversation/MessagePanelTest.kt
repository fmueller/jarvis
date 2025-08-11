package com.github.fmueller.jarvis.conversation

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.runInEdtAndGet
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import javax.swing.JButton
import javax.swing.JEditorPane
import javax.swing.JPanel

class MessagePanelTest : BasePlatformTestCase() {

    private lateinit var messagePanel: MessagePanel

    override fun setUp() {
        super.setUp()
        messagePanel = MessagePanel.createForTesting(Message.fromAssistant("Hello, I am Jarvis."), project)
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

    fun `test code block starting with two backticks`() {
        messagePanel.message = Message.fromAssistant("``kotlin\nprintln(\"Hi\")\n```")

        assertEquals(1, messagePanel.parsed.size)
        assertTrue(messagePanel.parsed[0] is MessagePanel.Code)

        val parsedCode = messagePanel.parsed[0] as MessagePanel.Code
        assertEquals("kotlin", parsedCode.languageId)
        assertEquals("println(\"Hi\")", parsedCode.content)
        assertTrue(messagePanel.message.contentWithClosedTrailingCodeBlock().lines().first().trimStart().startsWith("```"))
    }

    fun `test code block ending with two backticks`() {
        messagePanel.message = Message.fromAssistant("```kotlin\nprintln(\"Hi\")\n``")

        assertEquals(1, messagePanel.parsed.size)
        assertTrue(messagePanel.parsed[0] is MessagePanel.Code)

        val parsedCode = messagePanel.parsed[0] as MessagePanel.Code
        assertEquals("kotlin", parsedCode.languageId)
        assertEquals("println(\"Hi\")", parsedCode.content)
        assertTrue(messagePanel.message.contentWithClosedTrailingCodeBlock().lines().last().trim() == "```")
    }

    fun `test unclosed code block starting with two backticks`() {
        messagePanel.message = Message.fromAssistant("``kotlin\nprintln(\"Hi\")")

        assertEquals(1, messagePanel.parsed.size)
        assertTrue(messagePanel.parsed[0] is MessagePanel.Code)

        val parsedCode = messagePanel.parsed[0] as MessagePanel.Code
        assertEquals("kotlin", parsedCode.languageId)
        assertEquals("println(\"Hi\")", parsedCode.content)
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
        val panel = MessagePanel.createForTesting(Message.info("Downloading"), project)
        val label = panel.getComponent(0) as JBLabel
        assertEquals("Info", label.text)
    }

    fun `test reasoning block is parsed`() {
        messagePanel.message = Message.fromAssistant("<think>Reason</think>Hello")

        assertEquals(1, messagePanel.parsed.size)
        assertTrue(messagePanel.reasoningMessagePanel?.displayedText?.contains("Reason") ?: false)
    }

    fun `test reasoning panel shows last paragraph when in progress`() {
        messagePanel.message = Message.fromAssistant("<think>First paragraph.\n\nSecond incomplete")

        assertEquals("First paragraph.", messagePanel.reasoningMessagePanel?.displayedText)
    }

    fun `test reasoning duration label`() {
        messagePanel.reasoningStartTimeMs = System.currentTimeMillis() - 65000
        messagePanel.message = Message.fromAssistant("<think>Done</think>Hi")

        val headerField = MessagePanel::class.java.getDeclaredField("reasoningHeaderButton")
        headerField.isAccessible = true
        val headerButton = headerField.get(messagePanel) as JButton
        assertEquals("Reasoned for 1 minutes and 05 seconds", headerButton.text)
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

    fun `test updating content wrapped in scrollpane`() {
        // prepare a JBScrollPane wrapping a JEditorPane
        val editorPane = JEditorPane().apply {
            text = "Hello, "
        }
        val scroll = JBScrollPane(editorPane)

        runInEdtAndGet {
            messagePanel.removeAll()
            messagePanel.parsed.clear()
            messagePanel.parsed.add(MessagePanel.Content("Hello, "))
            messagePanel.add(scroll)
        }

        // trigger an update to new content
        messagePanel.message = Message.fromAssistant("Hello, I am Jarvis.")

        assertEquals("<p>Hello, I am Jarvis.</p>", editorPane.text.trim())
    }

    fun `test first content part is preserved when code content is rendered`() {
        // Start with a message containing text and then add code
        messagePanel.message = Message.fromAssistant("Here is the solution:")

        // Verify initial state - should have one content part
        assertEquals(1, messagePanel.parsed.size)
        assertTrue(messagePanel.parsed[0] is MessagePanel.Content)
        assertEquals("Here is the solution:", (messagePanel.parsed[0] as MessagePanel.Content).markdown)

        // Update to include a code block
        messagePanel.message = Message.fromAssistant("Here is the solution:\n\n```kotlin\nprintln(\"Hello, World!\")\n```")

        // Verify that both content and code parts are preserved
        assertEquals(2, messagePanel.parsed.size)
        assertTrue(messagePanel.parsed[0] is MessagePanel.Content)
        assertTrue(messagePanel.parsed[1] is MessagePanel.Code)

        // Verify the first content part is still there
        assertEquals("Here is the solution:\n\n", (messagePanel.parsed[0] as MessagePanel.Content).markdown)

        // Verify the code part is correct
        val codeContent = messagePanel.parsed[1] as MessagePanel.Code
        assertEquals("kotlin", codeContent.languageId)
        assertEquals("println(\"Hello, World!\")", codeContent.content)

        // Verify the UI components are present
        // Should have: role label + reasoning panel + content component + code component
        val expectedComponentCount = 4
        assertTrue("Expected at least $expectedComponentCount components, but got ${messagePanel.componentCount}",
            messagePanel.componentCount >= expectedComponentCount)
    }

    fun `test content not duplicated when adding code after existing content`() {
        // Start with content only
        messagePanel.message = Message.fromAssistant("Here is the solution:")

        // Verify initial state
        assertEquals(1, messagePanel.parsed.size)
        assertTrue(messagePanel.parsed[0] is MessagePanel.Content)
        assertEquals("Here is the solution:", (messagePanel.parsed[0] as MessagePanel.Content).markdown)

        // Count initial UI components (role label + reasoning panel + content component)
        val initialComponentCount = messagePanel.componentCount

        // Update to add code block - this should modify the first content and add code
        messagePanel.message = Message.fromAssistant("Here is the solution:\n\n```kotlin\nprintln(\"Hello, World!\")\n```")

        // Verify parsing is correct
        assertEquals(2, messagePanel.parsed.size)
        assertTrue(messagePanel.parsed[0] is MessagePanel.Content)
        assertTrue(messagePanel.parsed[1] is MessagePanel.Code)

        // Verify the first content part was updated, not duplicated
        assertEquals("Here is the solution:\n\n", (messagePanel.parsed[0] as MessagePanel.Content).markdown)

        // Verify we have exactly one more component (the code block)
        assertEquals(initialComponentCount + 1, messagePanel.componentCount)

        // Verify no duplicate content by checking that we don't have extra text components
        var textComponentCount = 0
        var codeComponentCount = 0

        for (i in 0 until messagePanel.componentCount) {
            val component = messagePanel.getComponent(i)
            when {
                component is JEditorPane -> textComponentCount++
                component is JBScrollPane && component.viewport.view !is JEditorPane -> codeComponentCount++
                component is JBLabel -> { /* role label - ignore */ }
                component is JPanel -> { /* reasoning panel - ignore */ }
            }
        }

        // Should have exactly 1 text component and 1 code component
        assertEquals("Should have exactly 1 text component but found $textComponentCount", 1, textComponentCount)
        assertEquals("Should have exactly 1 code component but found $codeComponentCount", 1, codeComponentCount)
    }

    fun `test removeAllComponentsAfter preserves correct components`() {
        // Set up a message with multiple content parts
        messagePanel.message = Message.fromAssistant("First part\n\n```kotlin\ncode1\n```\n\nSecond part\n\n```java\ncode2\n```")

        // Verify initial parsing
        assertEquals(4, messagePanel.parsed.size)
        assertTrue(messagePanel.parsed[0] is MessagePanel.Content) // "First part\n\n"
        assertTrue(messagePanel.parsed[1] is MessagePanel.Code)    // kotlin code
        assertTrue(messagePanel.parsed[2] is MessagePanel.Content) // "\n\nSecond part\n\n"
        assertTrue(messagePanel.parsed[3] is MessagePanel.Code)    // java code

        val initialComponentCount = messagePanel.componentCount

        // Update a message to change the second content part - this should trigger removeAllComponentsAfter(2)
        messagePanel.message = Message.fromAssistant("First part\n\n```kotlin\ncode1\n```\n\nModified second part")

        // Verify the parsing updated correctly
        assertEquals(3, messagePanel.parsed.size)
        assertTrue(messagePanel.parsed[0] is MessagePanel.Content) // "First part\n\n"
        assertTrue(messagePanel.parsed[1] is MessagePanel.Code)    // kotlin code (unchanged)
        assertTrue(messagePanel.parsed[2] is MessagePanel.Content) // "Modified second part" (changed)

        // Verify the first two parts are preserved
        assertEquals("First part\n\n", (messagePanel.parsed[0] as MessagePanel.Content).markdown)
        assertEquals("kotlin", (messagePanel.parsed[1] as MessagePanel.Code).languageId)
        assertEquals("code1", (messagePanel.parsed[1] as MessagePanel.Code).content)
        assertEquals("\n\nModified second part", (messagePanel.parsed[2] as MessagePanel.Content).markdown)

        // Should have one less component (removed the second code block)
        assertEquals(initialComponentCount - 1, messagePanel.componentCount)
    }
}
