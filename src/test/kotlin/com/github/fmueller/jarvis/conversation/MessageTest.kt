package com.github.fmueller.jarvis.conversation

import junit.framework.TestCase

class MessageTest : TestCase() {

    fun `test parseReasoning returns reasoning and remaining text`() {
        val message = Message.fromAssistant("<think>Reason</think>Hello")

        val (reasoning, remaining) = message.parseReasoning()

        assertNotNull(reasoning)
        assertEquals("Reason", reasoning?.markdown)
        assertFalse(reasoning?.isInProgress ?: true)
        assertEquals("Hello", remaining)
    }

    fun `test parseReasoning unfinished reasoning`() {
        val message = Message.fromAssistant("<think>Reasoning in progress")

        val (reasoning, remaining) = message.parseReasoning()

        assertNotNull(reasoning)
        assertEquals("Reasoning in progress", reasoning?.markdown)
        assertTrue(reasoning?.isInProgress ?: false)
        assertEquals("", remaining)
    }

    fun `test parseReasoning with no reasoning`() {
        val message = Message.fromAssistant("Hello")

        val (reasoning, remaining) = message.parseReasoning()

        assertNull(reasoning)
        assertEquals("Hello", remaining)
    }
}
