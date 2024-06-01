package com.github.fmueller.jarvis.commands

import com.github.fmueller.jarvis.ai.OllamaService

class SlashCommandParser(private val ollamaService: OllamaService) {

    fun parse(message: String): SlashCommand {
        val trimmedMessage = message.trim().lowercase()
        if (trimmedMessage == "/help" || trimmedMessage == "/?") {
            return HelpCommand()
        }

        return ChatCommand(ollamaService)
    }
}
