package com.github.fmueller.jarvis.toolWindow

import com.github.fmueller.jarvis.conversation.Conversation
import com.github.fmueller.jarvis.conversation.ConversationPanel
import com.github.fmueller.jarvis.conversation.Message
import com.github.fmueller.jarvis.conversation.Role
import com.github.fmueller.jarvis.services.OllamaService
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.awt.*
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.border.AbstractBorder

class ConversationWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val conversationWindow = ConversationWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(conversationWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class ConversationWindow(toolWindow: ToolWindow) {

        private val project = toolWindow.project
        private val ollama = project.service<OllamaService>()

        // TODO conversationPanel should have a reference to the conversation and register the property change listener
        private val conversation = Conversation()
        private val conversationPanel = ConversationPanel(project)

        init {
            conversation.addPropertyChangeListener {
                if (it.propertyName == "messages") {
                    SwingUtilities.invokeLater {
                        conversationPanel.update(conversation)
                    }
                }
            }

            conversation.addMessage(Message(Role.ASSISTANT, "Hello! How can I help you?"))
        }

        @OptIn(DelicateCoroutinesApi::class)
        fun getContent() = BorderLayoutPanel().apply {
            var borderColor = JBColor.GRAY
            // TODO extract input area to a separate class and add placeholder text
            val inputArea = JBTextArea().apply {
                lineWrap = true
                wrapStyleWord = true
                font = UIManager.getFont("Label.font")
                border = object : AbstractBorder() {
                    override fun getBorderInsets(c: Component?): Insets {
                        return JBUI.insets(9)
                    }

                    override fun paintBorder(c: Component?, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
                        val g2 = g.create() as Graphics2D
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                        g2.color = borderColor
                        g2.drawRoundRect(x, y, width - 1, height - 1, 10, 10)
                        g2.dispose()
                    }
                }
            }

            inputArea.addFocusListener(object : FocusListener {
                override fun focusGained(e: FocusEvent?) {
                    val defaultHighlightColor = JBColor.namedColor("selection.background", JBColor.BLUE)
                    borderColor = defaultHighlightColor
                }

                override fun focusLost(e: FocusEvent?) {
                    borderColor = JBColor.GRAY
                }
            })
            inputArea.addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    if (e.keyCode == KeyEvent.VK_ENTER && !e.isShiftDown) {
                        e.consume()
                    }
                }

                override fun keyReleased(e: KeyEvent) {
                    if (e.keyCode == KeyEvent.VK_ENTER) {
                        if (e.isShiftDown) {
                            inputArea.append("\n")
                        } else {
                            val question = inputArea.text.trim()
                            inputArea.text = ""
                            inputArea.isEnabled = false
                            conversation.addMessage(Message(Role.USER, question))

                            GlobalScope.launch(Dispatchers.EDT) {
                                // TODO add conversation service and run simple pipeline for slash command detection
                                val answer = ollama.chat(conversation).trim()
                                conversation.addMessage(Message(Role.ASSISTANT, answer))

                                inputArea.isEnabled = true
                                inputArea.requestFocusInWindow()
                            }
                        }
                    }
                }
            })

            addToCenter(JBScrollPane(conversationPanel).apply {
                horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            })

            addToBottom(BorderLayoutPanel().apply {
                addToCenter(JPanel(BorderLayout()).apply {
                    border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
                    add(inputArea, BorderLayout.CENTER)
                })
            })
        }
    }
}
