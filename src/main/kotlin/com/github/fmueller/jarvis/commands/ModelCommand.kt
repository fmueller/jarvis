package com.github.fmueller.jarvis.commands

import com.github.fmueller.jarvis.ai.OllamaService
import com.github.fmueller.jarvis.conversation.Conversation
import com.github.fmueller.jarvis.conversation.Message

/**
 * Command to change the current model or display information about it.
 *
 * When [modelName] is `null` or blank the command will fetch the info card
 * of the currently configured model from Ollama's `/api/show` endpoint.
 * Otherwise the provided model name is set as the new model.
 */
class ModelCommand(private val modelName: String? = null) : SlashCommand {

    override suspend fun run(conversation: Conversation): Conversation {
        if (modelName.isNullOrBlank()) {
            val info = OllamaService.getModelInfo()
            conversation.addMessage(Message.info(info))
            return conversation
        }

        val newModel = if (modelName.equals("default", ignoreCase = true)) {
            OllamaService.DEFAULT_MODEL_NAME
        } else {
            modelName
        }

        OllamaService.modelName = newModel
        conversation.addMessage(Message.info("Model changed to $newModel"))
        return conversation
    }
}
