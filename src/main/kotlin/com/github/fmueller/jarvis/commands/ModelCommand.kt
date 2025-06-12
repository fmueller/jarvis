package com.github.fmueller.jarvis.commands

import com.github.fmueller.jarvis.ai.OllamaService
import com.github.fmueller.jarvis.conversation.Conversation
import com.github.fmueller.jarvis.conversation.Message

class ModelCommand(private val modelName: String) : SlashCommand {

    override suspend fun run(conversation: Conversation): Conversation {
        OllamaService.modelName = if (modelName.equals("default", ignoreCase = true)) "qwen3:4b" else modelName
        conversation.addMessage(Message.info("Model changed to $modelName"))
        return conversation
    }
}
