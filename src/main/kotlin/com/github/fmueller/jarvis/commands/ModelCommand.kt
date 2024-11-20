package com.github.fmueller.jarvis.commands

import com.github.fmueller.jarvis.ai.OllamaService
import com.github.fmueller.jarvis.conversation.Conversation
import com.github.fmueller.jarvis.conversation.Message

class ModelCommand(private val modelName: String) : SlashCommand {

    override suspend fun run(conversation: Conversation): Conversation {
        if (modelName == "llama3.1" || modelName == "llama3.2") {
            OllamaService.setModel(modelName)
            conversation.addMessage(Message.fromAssistant("Model changed to $modelName"))
        } else {
            conversation.addMessage(Message.fromAssistant("Invalid model name: $modelName"))
        }
        return conversation
    }
}
