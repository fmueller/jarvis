package com.github.fmueller.jarvis.commands

import com.github.fmueller.jarvis.ai.OllamaService
import com.github.fmueller.jarvis.conversation.Conversation
import com.github.fmueller.jarvis.conversation.Message

/**
 * Command to change the Ollama host.
 *
 * The special value `default` resets the host to `http://localhost:11434`.
 */
class HostCommand(private val host: String) : SlashCommand {

    override suspend fun run(conversation: Conversation): Conversation {
        val newHost = if (host.equals("default", ignoreCase = true)) {
            "http://localhost:11434"
        } else {
            host
        }
        OllamaService.host = newHost
        conversation.addMessage(Message.info("Host changed to $newHost"))
        return conversation
    }
}
