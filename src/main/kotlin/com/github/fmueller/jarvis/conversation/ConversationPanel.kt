package com.github.fmueller.jarvis.conversation

import com.github.fmueller.jarvis.conversation.ColorHelper.darker
import com.intellij.openapi.project.Project
import com.intellij.util.ui.UIUtil
import org.jdesktop.swingx.VerticalLayout
import javax.swing.JPanel

class ConversationPanel(private val project: Project) : JPanel() {

    init {
        layout = VerticalLayout(1)
        background = UIUtil.getPanelBackground().darker(0.75)
    }

    // TODO recalculate colors on theme change and don't set darker panel background for bright themes
    fun update(conversation: Conversation) {
        removeAll()
        conversation.getMessages().forEach { message ->
            add(MessagePanel(message, project))
        }
        revalidate()
        repaint()
    }
}
