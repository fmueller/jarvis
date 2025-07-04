package com.github.fmueller.jarvis.commands

import com.github.fmueller.jarvis.ai.OllamaService
import com.github.fmueller.jarvis.conversation.Conversation
import com.github.fmueller.jarvis.conversation.Message

class PlainChatCommand : SlashCommand {

    override suspend fun run(conversation: Conversation): Conversation {
        if (!OllamaService.isAvailable()) {
            conversation.addMessage(
                Message.fromAssistant("I can't access Ollama at ```${OllamaService.host}```. You need to install it first and download the ```qwen3:1.7b``` model.")
            )
            return conversation
        }

        val response = OllamaService.chat(conversation, false).trim()
        if (response.isNotBlank()) {
            conversation.addMessage(Message.fromAssistant(response))
        }
        return conversation
    }
}
