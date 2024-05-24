package com.github.fmueller.jarvis.conversation

import com.intellij.openapi.project.Project
import com.intellij.util.ui.UIUtil
import org.jdesktop.swingx.VerticalLayout
import java.awt.Color
import javax.swing.BorderFactory
import javax.swing.JPanel

class ConversationPanel(private val project: Project) : JPanel() {

    private val assistantBgColor = UIUtil.getPanelBackground().darker(0.95)
    private val userBgColor = UIUtil.getPanelBackground().darker(0.85)

    init {
        layout = VerticalLayout(1)
        background = UIUtil.getPanelBackground().darker(0.75)
    }

    // TODO recalculate colors on theme change and don't set darker panel background for bright themes
    fun update(conversation: Conversation) {
        removeAll()
        conversation.getMessages().forEach { m ->
            // TODO use label in MarkdownContentPanel to show the role
            val markdown = "**${if (m.role == Role.ASSISTANT) "Jarvis" else "You"}**\n\n${m.content}"
            val bgColor = if (m.role == Role.ASSISTANT) assistantBgColor else userBgColor
            add(MarkdownContentPanel(project, markdown, bgColor).apply {
                border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
            })
        }
        // TODO scroll to the bottom
        revalidate()
        repaint()
    }

    private fun Color.darker(factor: Double): Color {
        val r = (red * factor).toInt().coerceIn(0, 255)
        val g = (green * factor).toInt().coerceIn(0, 255)
        val b = (blue * factor).toInt().coerceIn(0, 255)
        return Color(r, g, b, alpha)
    }
}
