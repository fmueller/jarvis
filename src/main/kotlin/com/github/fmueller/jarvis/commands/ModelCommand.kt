package com.github.fmueller.jarvis.commands

import com.github.fmueller.jarvis.ai.OllamaService
import com.github.fmueller.jarvis.conversation.Conversation
import com.github.fmueller.jarvis.conversation.Message

class ModelCommand(private val modelName: String) : SlashCommand {

    override suspend fun run(conversation: Conversation): Conversation {
        val newModel = if (modelName.equals("default", ignoreCase = true)) "qwen3:4b" else modelName
        OllamaService.modelName = newModel
        conversation.addMessage(Message.info("Model changed to $newModel"))
        return conversation
    }
}
