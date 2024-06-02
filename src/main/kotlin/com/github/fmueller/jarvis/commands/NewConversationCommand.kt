package com.github.fmueller.jarvis.commands

import com.github.fmueller.jarvis.conversation.Conversation

class NewConversationCommand : SlashCommand {

    // as long as we don't have multiple conversations
    // we can just clear the messages
    override suspend fun run(conversation: Conversation): Conversation {
        conversation.clearMessages()
        return conversation
    }
}
