package com.github.fmueller.jarvis.commands

import com.github.fmueller.jarvis.conversation.Conversation
import com.github.fmueller.jarvis.conversation.Message
import com.github.fmueller.jarvis.conversation.Role
import com.github.fmueller.jarvis.conversation.Code
import com.intellij.lang.Language
import com.intellij.openapi.ide.CopyPasteManager
import org.jetbrains.annotations.VisibleForTesting
import java.awt.datatransfer.StringSelection

class CopyCommand : SlashCommand {

    override suspend fun run(conversation: Conversation): Conversation {
        val prompt = buildPrompt(conversation)
        CopyPasteManager.getInstance().setContents(StringSelection(prompt))
        conversation.addMessage(Message.info("Conversation copied to clipboard."))
        return conversation
    }

    @VisibleForTesting
    internal fun buildPrompt(conversation: Conversation): String {
        val builder = StringBuilder()
        builder.appendLine(
            "I used the Jarvis plugin with a local model. Please take over this conversation and help me."
        )
        builder.appendLine()
        conversation.messages.forEach { message ->
            if (message.role == Role.USER) {
                builder.appendLine(
                    "[User]: " +
                        message.contentWithClosedTrailingCodeBlock().removePrefix("/plain ")
                )
                val code = message.codeContext?.selected
                if (code != null && !message.content.startsWith("/plain ")) {
                    builder.appendLine("[Code Context]:")
                    builder.appendLine(formatCode(code))
                }
            }
            if (message.role == Role.ASSISTANT) {
                builder.appendLine("[Assistant]: ${message.contentWithClosedTrailingCodeBlock()}")
            }
            builder.appendLine()
        }
        return builder.toString().trimEnd()
    }

    private fun formatCode(code: Code): String {
        val languageId = if (code.language == Language.ANY) "plaintext" else code.language.id.lowercase()
        return "```$languageId\n${code.content}\n```"
    }
}
