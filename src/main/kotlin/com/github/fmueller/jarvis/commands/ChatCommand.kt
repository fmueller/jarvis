package com.github.fmueller.jarvis.commands

import com.github.fmueller.jarvis.ai.OllamaService
import com.github.fmueller.jarvis.conversation.Conversation
import com.github.fmueller.jarvis.conversation.Message
import com.github.fmueller.jarvis.conversation.Role

class ChatCommand : SlashCommand {

    override suspend fun run(conversation: Conversation): Conversation {
        if (!OllamaService.isAvailable()) {
            conversation.addMessage(
                Message(
                    Role.ASSISTANT,
                    "I can't access Ollama at ```http://localhost:11434```. You need to install it first and download the ```llama3.1``` model."
                )
            )
            return conversation
        }

        val response = OllamaService.chat(conversation).trim()
        conversation.addMessage(Message(Role.ASSISTANT, response))
        return conversation
    }
}
