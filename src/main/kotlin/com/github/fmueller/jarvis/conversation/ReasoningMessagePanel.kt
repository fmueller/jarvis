package com.github.fmueller.jarvis.conversation

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.VisibleForTesting
import java.awt.BorderLayout
import javax.swing.JPanel

class ReasoningMessagePanel(
    project: Project
) : JPanel(), Disposable {

    private val innerPanel = MessagePanel.create(Message.fromAssistant(""), project, true)

    @VisibleForTesting
    var displayedText: String = ""
        private set

    init {
        layout = BorderLayout()
        innerPanel.border = null
        add(innerPanel, BorderLayout.CENTER)
    }

    fun update(reasoning: Message.Reasoning) {
        val text = if (reasoning.isInProgress) {
            extractLastFullParagraph(reasoning.markdown)
        } else {
            reasoning.markdown
        }
        displayedText = text
        innerPanel.message = Message.fromAssistant(text)
    }

    private fun extractLastFullParagraph(markdown: String): String {
        val paragraphs = markdown.split("\n\n")
        return paragraphs.dropLast(1).lastOrNull()?.trim() ?: ""
    }

    override fun dispose() {
        innerPanel.dispose()
    }
}

