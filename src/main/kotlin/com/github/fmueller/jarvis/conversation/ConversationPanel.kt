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
                    val newMessages = (it.newValue as? List<*>)?.filterIsInstance<Message>()
                    if (newMessages == null) {
                        throw IllegalStateException("Property 'messages' must be a list of messages")
                    }
                    updateSmooth(newMessages)
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

        conversation.addMessage(Message.fromAssistant("Hello! How can I help you?"))
    }

    private fun updateMessageInProgress(update: String) {
        if (updatePanel == null) {
            updatePanel = MessagePanel.create(Message.fromAssistant(update), project)
            Disposer.register(this, updatePanel!!)
            panel.add(updatePanel)
            panel.revalidate()
            panel.repaint()
        } else {
            updatePanel!!.message = Message.fromAssistant(update)
        }
    }

    private fun updateSmooth(messages: List<Message>) {
        val currentComponentCount = panel.componentCount
        val existingMessageCount = if (updatePanel != null) currentComponentCount - 1 else currentComponentCount

        // Handle the case where we have an updatePanel that should become permanent
        if (updatePanel != null && messages.size > existingMessageCount) {
            updatePanel!!.message = messages.last()
            // Clear the updatePanel reference since it's now a permanent part of the conversation
            updatePanel = null

            val remainingMessages = messages.drop(currentComponentCount)
            remainingMessages.forEach { message ->
                panel.add(MessagePanel.create(message, project))
            }
        } else {
            // Handle normal case where new messages are added
            val messagesToAdd = messages.drop(existingMessageCount)
            messagesToAdd.forEach { message ->
                panel.add(MessagePanel.create(message, project))
            }
        }

        panel.revalidate()
        panel.repaint()
    }

    override fun dispose() {
        // nothing to dispose here, all message panels are disposed when the panel is cleared
        // or automatically when the conversation is disposed
    }
}
