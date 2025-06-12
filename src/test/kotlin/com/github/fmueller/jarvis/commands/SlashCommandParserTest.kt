package com.github.fmueller.jarvis.commands

import junit.framework.TestCase.assertTrue
import org.junit.Test

class SlashCommandParserTest {

    @Test
    fun `test parse help returns HelpCommand`() {
        val command = SlashCommandParser.parse("/help")
        assertTrue(command is HelpCommand)
    }

    @Test
    fun `test parse new returns NewConversationCommand`() {
        val command = SlashCommandParser.parse("/new")
        assertTrue(command is NewConversationCommand)
    }

    @Test
    fun `test parse plain returns PlainChatCommand`() {
        val command = SlashCommandParser.parse("/plain hello")
        assertTrue(command is PlainChatCommand)
    }

    @Test
    fun `test parse model returns ModelCommand`() {
        val command = SlashCommandParser.parse("/model llama3.1")
        assertTrue(command is ModelCommand)
    }
}
