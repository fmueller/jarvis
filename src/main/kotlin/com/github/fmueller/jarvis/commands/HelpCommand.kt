package com.github.fmueller.jarvis.commands

import com.github.fmueller.jarvis.conversation.Conversation
import com.github.fmueller.jarvis.conversation.Message
import com.github.fmueller.jarvis.conversation.Role

class HelpCommand : SlashCommand {

    override suspend fun run(conversation: Conversation): Conversation {
        conversation.addMessage(
            Message(
                Role.ASSISTANT,
                "I'm Jarvis, your personal coding assistant. You can ask me anything. To make me work properly," +
                        " please install and run Ollama locally."
            )
        )
        return conversation
    }
}
