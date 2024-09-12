package com.github.fmueller.jarvis.commands

import com.github.fmueller.jarvis.ai.OllamaService
import com.github.fmueller.jarvis.conversation.Conversation
import com.github.fmueller.jarvis.conversation.Message

class PlainChatCommand : SlashCommand {

    override suspend fun run(conversation: Conversation): Conversation {
        if (!OllamaService.isAvailable()) {
            conversation.addMessage(
                Message.fromAssistant("I can't access Ollama at ```http://localhost:11434```. You need to install it first and download the ```llama3.1``` model.")
            )
            return conversation
        }

        val response = OllamaService.chat(conversation, false).trim()
        conversation.addMessage(Message.fromAssistant(response))
        return conversation
    }
}
