package com.github.fmueller.jarvis.conversation

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBScrollPane
import org.jdesktop.swingx.VerticalLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.SwingUtilities

class ConversationPanel(conversation: Conversation, private val project: Project) : Disposable {

    private val panel = JPanel().apply {
        layout = VerticalLayout(1)
    }

    private var updatePanel: MessagePanel? = null

    // we are exposing the scrollable container here and keep it in the panel
    // because we need to adjust the scroll position when new messages are added
    val scrollableContainer = JBScrollPane(panel).apply {
        verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
    }

    init {
        Disposer.register(conversation, this)

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
                    update(it.newValue as List<Message>)
                }
            }
        }

        conversation.addPropertyChangeListener {
            if (it.propertyName == "messageBeingGenerated") {
                SwingUtilities.invokeLater {
                    isUserScrolling = false
                    updateMessageInProgress(it.newValue as String)
                }
            }
        }

        conversation.addMessage(Message(Role.ASSISTANT, "Hello! How can I help you?"))
    }

    private fun updateMessageInProgress(update: String) {
        if (update.isNotEmpty()) {
            if (updatePanel == null) {
                updatePanel = MessagePanel(Message(Role.ASSISTANT, update), project)
                panel.add(updatePanel)
            } else {
                updatePanel!!.message = Message(Role.ASSISTANT, update)
            }
        } else if (updatePanel != null) {
            panel.remove(updatePanel)
            updatePanel = null
        }

        panel.revalidate()
        panel.repaint()
    }

    private fun update(messages: List<Message>) {
        if (messages.size <= 1) {
            panel.components.filter { it is Disposable }.map { it as Disposable }.forEach { it.dispose() }
            panel.removeAll()
        }

        if (messages.isNotEmpty()) {
            val messagePanel = MessagePanel(messages.last(), project)
            Disposer.register(this, messagePanel)
            panel.add(messagePanel)
        }

        panel.revalidate()
        panel.repaint()
    }

    override fun dispose() {
        // nothing to dispose here, all message panels are disposed when the panel is cleared
        // or automatically when the conversation is disposed
    }
}
