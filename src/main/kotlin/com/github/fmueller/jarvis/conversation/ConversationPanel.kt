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

    internal val panel = JPanel().apply {
        layout = VerticalLayout(1)
    }

    internal var updatePanel: MessagePanel? = null

    private val messagePanels = mutableListOf<MessagePanel>()
    private var shouldFadeNextMessage = false

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
                        ?: throw IllegalStateException("Property 'messages' must be a list of messages")
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

        updateSmooth(conversation.messages)
    }

    internal fun updateMessageInProgress(update: String) {
        // currently, an empty update message is treated as a request to signal the finish of message generation
        // TODO refactor this into a proper message object
        if (update.isEmpty()) {
            updatePanel?.fadeInFinalMessage()
            return
        }

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

    internal fun updateSmooth(messages: List<Message>) {
        val currentComponentCount = panel.componentCount
        val existingMessageCount = if (updatePanel != null) currentComponentCount - 1 else currentComponentCount

        // Handle the case where messages have been cleared (e.g., new conversation)
        if (messages.size < existingMessageCount) {
            messagePanels.forEach { Disposer.dispose(it) }
            messagePanels.clear()
            panel.removeAll()
            Disposer.dispose { updatePanel }
            updatePanel = null

            messages.forEach { message ->
                val messagePanel = MessagePanel.create(message, project)
                Disposer.register(this, messagePanel)
                messagePanels.add(messagePanel)
                panel.add(messagePanel)
            }
        } else if (updatePanel != null && messages.size > existingMessageCount) {
            // Handle the case where we have an updatePanel that should become permanent
            messagePanels.add(updatePanel!!)
            updatePanel = null

            val remainingMessages = messages.drop(currentComponentCount)
            remainingMessages.forEach { message ->
                val messagePanel = MessagePanel.create(message, project)
                Disposer.register(this, messagePanel)
                messagePanels.add(messagePanel)
                panel.add(messagePanel)
            }
        } else {
            // Handle normal case where new messages are added
            val messagesToAdd = messages.drop(existingMessageCount)
            messagesToAdd.forEach { message ->
                val messagePanel = MessagePanel.create(message, project)
                Disposer.register(this, messagePanel)
                messagePanels.add(messagePanel)
                panel.add(messagePanel)
            }
        }

        panel.revalidate()
        panel.repaint()
    }

    override fun dispose() {
        messagePanels.forEach { Disposer.dispose(it) }
        messagePanels.clear()

        updatePanel?.let { Disposer.dispose(it) }
        updatePanel = null
    }
}
