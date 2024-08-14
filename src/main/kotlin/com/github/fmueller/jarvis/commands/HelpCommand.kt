package com.github.fmueller.jarvis.commands

import com.github.fmueller.jarvis.conversation.Conversation
import com.github.fmueller.jarvis.conversation.Message

class HelpCommand : SlashCommand {

    override suspend fun run(conversation: Conversation): Conversation {
        conversation.addMessage(Message.HELP_MESSAGE)
        return conversation
    }
}
