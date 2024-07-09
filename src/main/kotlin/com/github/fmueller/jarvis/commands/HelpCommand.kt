package com.github.fmueller.jarvis.commands

import com.github.fmueller.jarvis.conversation.Conversation
import com.github.fmueller.jarvis.conversation.Message
import com.github.fmueller.jarvis.conversation.Role

class HelpCommand : SlashCommand {

    override suspend fun run(conversation: Conversation): Conversation {
        conversation.addMessage(
            Message(
                Role.ASSISTANT,
                """
                I'm Jarvis, your personal coding assistant. You can ask me anything. To make me work properly, please install and run Ollama locally.
                
                Available commands:
                
                - ```/help``` or ```/?``` - Shows this help message
                - ```/new``` - Starts a new conversation
                
                Available flags (just add them to your input message):
                
                - ```--selected-code``` or ```-s``` - Adds the selected code from your editor to the prompt
                """.trimIndent()
            )
        )
        return conversation
    }
}
