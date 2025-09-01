package com.github.fmueller.jarvis.commands

import com.github.fmueller.jarvis.ai.OllamaService
import com.github.fmueller.jarvis.conversation.Conversation
import com.github.fmueller.jarvis.conversation.Message

/**
 * Command to configure inference parameters of the current model.
 *
 * Supported syntax: `/model set -<parameter> <value>`
 * Multiple parameter-value pairs can be provided in one invocation.
 */
class ModelSetCommand(private val params: Map<String, String>) : SlashCommand {

    override suspend fun run(conversation: Conversation): Conversation {
        if (params.isEmpty()) {
            conversation.addMessage(Message.info("No parameters specified"))
            return conversation
        }
        val errors = mutableListOf<String>()
        params.forEach { (name, value) ->
            val error = OllamaService.setParameter(name, value)
            if (error != null) {
                errors.add(error)
            }
        }
        OllamaService.clearChatMemory()
        val message = if (errors.isEmpty()) {
            "Parameters updated"
        } else {
            errors.joinToString("\n")
        }
        conversation.addMessage(Message.info(message))
        return conversation
    }
}

