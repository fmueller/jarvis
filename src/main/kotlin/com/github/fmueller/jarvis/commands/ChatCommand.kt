package com.github.fmueller.jarvis.commands

import com.github.fmueller.jarvis.ai.OllamaService
import com.github.fmueller.jarvis.conversation.Conversation
import com.github.fmueller.jarvis.conversation.Message

class ChatCommand : SlashCommand {

    override suspend fun run(conversation: Conversation): Conversation {
        if (!OllamaService.isAvailable()) {
            conversation.addMessage(
                Message.fromAssistant("I can't access Ollama at ```http://localhost:11434```.")
            )
            return conversation
        }

        val response = OllamaService.chat(conversation, true).trim()
        if (response.isNotBlank()) {
            conversation.addMessage(Message.fromAssistant(response))
        }
        return conversation
    }
}
