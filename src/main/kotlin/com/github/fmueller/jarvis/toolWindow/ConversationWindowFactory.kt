package com.github.fmueller.jarvis.toolWindow

import com.github.fmueller.jarvis.conversation.*
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.components.BorderLayoutPanel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.BorderFactory
import javax.swing.JPanel

class ConversationWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val conversationWindow = ConversationWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(conversationWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class ConversationWindow(toolWindow: ToolWindow) {

        private val conversation = toolWindow.project.service<Conversation>()
        private val conversationPanel = ConversationPanel(conversation, toolWindow.project)

        @OptIn(DelicateCoroutinesApi::class)
        private val inputArea = InputArea().apply {
            placeholderText = "Ask Jarvis a question or type /? for help"

            addKeyListener(object : KeyAdapter() {

                override fun keyReleased(e: KeyEvent) {
                    if (e.keyCode == KeyEvent.VK_ENTER && !e.isShiftDown) {
                        val message = text.trim()
                        if (message.isEmpty()) {
                            return
                        }

                        GlobalScope.launch(Dispatchers.EDT) {
                            text = ""
                            isEnabled = false

                            conversation.chat(Message(Role.USER, message, CodeContextHelper.getCodeContext(toolWindow.project)))

                            isEnabled = true
                            requestFocusInWindow()
                        }
                    }
                }
            })
        }

        fun getContent() = BorderLayoutPanel().apply {
            addToCenter(conversationPanel.scrollableContainer)
            addToBottom(BorderLayoutPanel().apply {
                addToCenter(JPanel(BorderLayout()).apply {
                    border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
                    add(inputArea, BorderLayout.CENTER)
                })
            })
        }
    }
}
