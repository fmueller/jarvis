package com.github.fmueller.jarvis.toolWindow

import com.github.fmueller.jarvis.ai.OllamaService
import com.github.fmueller.jarvis.conversation.Conversation
import com.github.fmueller.jarvis.conversation.ConversationPanel
import com.github.fmueller.jarvis.conversation.InputArea
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

        private val conversation = Conversation(toolWindow.project.service<OllamaService>())
        private val conversationPanel = ConversationPanel(conversation, toolWindow.project)

        @OptIn(DelicateCoroutinesApi::class)
        fun getContent() = BorderLayoutPanel().apply {
            val inputArea = InputArea().apply { placeholderText = "Ask Jarvis a question or type /? for help" }

            inputArea.addKeyListener(object : KeyAdapter() {

                override fun keyReleased(e: KeyEvent) {
                    if (e.keyCode == KeyEvent.VK_ENTER && !e.isShiftDown) {

                        val message = inputArea.text
                        inputArea.text = ""
                        inputArea.isEnabled = false

                        GlobalScope.launch(Dispatchers.EDT) {
                            conversation.chat(message)
                            inputArea.isEnabled = true
                            inputArea.requestFocusInWindow()
                        }
                    }
                }
            })

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
