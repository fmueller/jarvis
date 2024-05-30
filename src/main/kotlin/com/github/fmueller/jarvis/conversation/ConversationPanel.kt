package com.github.fmueller.jarvis.conversation

import com.github.fmueller.jarvis.ui.ColorHelper.darker
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.UIUtil
import org.jdesktop.swingx.VerticalLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.SwingUtilities

class ConversationPanel(private val conversation: Conversation, private val project: Project) {

    private val panel = JPanel().apply {
        layout = VerticalLayout(1)
        background = UIUtil.getPanelBackground().darker(0.75)
    }

    // we are exposing the scrollable container here and keep it in the panel
    // because we need to adjust the scroll position when new messages are added
    val scrollableContainer = JBScrollPane(panel).apply {
        verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
    }

    init {
        var isUserScrolling = false

        scrollableContainer.verticalScrollBar.addAdjustmentListener { e ->
            if (!isUserScrolling) {
                e.adjustable.value = e.adjustable.maximum
            }
        }

        scrollableContainer.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                isUserScrolling = true
            }
        })

        scrollableContainer.addMouseWheelListener {
            isUserScrolling = true
        }

        conversation.addPropertyChangeListener {
            if (it.propertyName == "messages") {
                SwingUtilities.invokeLater {
                    isUserScrolling = false
                    update(conversation)
                }
            }
        }

        conversation.addMessage(Message(Role.ASSISTANT, "Hello! How can I help you?"))
    }

    // TODO recalculate colors on theme change and don't set darker panel background for bright themes
    private fun update(conversation: Conversation) {
        panel.removeAll()
        conversation.getMessages().forEach { message ->
            panel.add(MessagePanel(message, project))
        }
        panel.revalidate()
        panel.repaint()
    }
}
