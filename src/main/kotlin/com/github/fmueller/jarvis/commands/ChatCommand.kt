package com.github.fmueller.jarvis.commands

import com.github.fmueller.jarvis.ai.OllamaService
import com.github.fmueller.jarvis.conversation.Conversation
import com.github.fmueller.jarvis.conversation.Message
import com.github.fmueller.jarvis.conversation.Role

class ChatCommand(private val ollamaService: OllamaService) : SlashCommand {

    override suspend fun run(conversation: Conversation): Conversation {
        val response = ollamaService.chat(conversation.messages).trim()
        conversation.addMessage(Message(Role.ASSISTANT, response))
        return conversation
    }
}
