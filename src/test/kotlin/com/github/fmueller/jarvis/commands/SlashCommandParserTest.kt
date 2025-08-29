package com.github.fmueller.jarvis.commands

import junit.framework.TestCase

class SlashCommandParserTest : TestCase() {

    fun `test parse help returns HelpCommand`() {
        val command = SlashCommandParser.parse("/help")
        assertTrue(command is HelpCommand)
    }

    fun `test parse new returns NewConversationCommand`() {
        val command = SlashCommandParser.parse("/new")
        assertTrue(command is NewConversationCommand)
    }

    fun `test parse plain returns PlainChatCommand`() {
        val command = SlashCommandParser.parse("/plain hello")
        assertTrue(command is PlainChatCommand)
    }

    fun `test parse model returns ModelCommand`() {
        val command = SlashCommandParser.parse("/model llama3.1")
        assertTrue(command is ModelCommand)
    }

    fun `test parse model without name returns ModelCommand`() {
        val command = SlashCommandParser.parse("/model")
        assertTrue(command is ModelCommand)
    }

    fun `test parse model info returns ModelCommand`() {
        val command = SlashCommandParser.parse("/model-info")
        assertTrue(command is ModelCommand)
    }

    fun `test parse model set returns ModelSetCommand`() {
        val command = SlashCommandParser.parse("/model set -temperature 1.0")
        assertTrue(command is ModelSetCommand)
    }

    fun `test parse host returns HostCommand`() {
        val command = SlashCommandParser.parse("/host http://1.2.3.4")
        assertTrue(command is HostCommand)
    }

    fun `test parse copy returns CopyCommand`() {
        val command = SlashCommandParser.parse("/copy")
        assertTrue(command is CopyCommand)
    }
}
