package com.github.fmueller.jarvis.commands

object SlashCommandParser {

    fun parse(message: String): SlashCommand {
        val trimmedMessage = message.trim().lowercase()
        if (trimmedMessage == "/help" || trimmedMessage == "/?") {
            return HelpCommand()
        }

        if (trimmedMessage == "/new") {
            return NewConversationCommand()
        }

        if (trimmedMessage.startsWith("/plain ")) {
            return PlainChatCommand()
        }

        if (trimmedMessage == "/model" || trimmedMessage == "/model-info") {
            return ModelCommand()
        }

        if (trimmedMessage.startsWith("/model ")) {
            val modelName = trimmedMessage.removePrefix("/model ").trim()
            return ModelCommand(modelName)
        }

        if (trimmedMessage.startsWith("/host ")) {
            val host = trimmedMessage.removePrefix("/host ").trim()
            return HostCommand(host)
        }

        if (trimmedMessage == "/copy") {
            return CopyCommand()
        }

        return ChatCommand()
    }
}
