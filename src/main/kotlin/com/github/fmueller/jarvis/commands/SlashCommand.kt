package com.github.fmueller.jarvis.commands

import com.github.fmueller.jarvis.conversation.Conversation

interface SlashCommand {

    suspend fun run(conversation: Conversation): Conversation
}
