package com.github.fmueller.jarvis.toolWindow

import com.github.fmueller.jarvis.conversation.*
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.components.BorderLayoutPanel
import kotlinx.coroutines.*
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

    @OptIn(DelicateCoroutinesApi::class)
    class ConversationWindow(private val toolWindow: ToolWindow) {

        private val conversation = toolWindow.project.service<Conversation>()
        private val conversationPanel = ConversationPanel(conversation, toolWindow.project)

        private val sendButton: SendButton = SendButton(
            onSend = { sendMessage() },
            onStop = {
                conversation.cancelCurrentChat()
                conversation.addMessage(Message.info("Response generation was cancelled"))
                inputArea.isEnabled = true
                sendButton.setSending(false)
                inputArea.requestFocusInWindow()
            }
        )

        private val inputArea: InputArea = InputArea().apply {
            placeholderText = "Ask Jarvis a question or type /? for help"

            addKeyListener(object : KeyAdapter() {

                override fun keyReleased(e: KeyEvent) {
                    if (e.keyCode == KeyEvent.VK_ENTER && !e.isShiftDown) {
                        sendMessage()
                    }
                }
            })
        }

        private fun sendMessage() {
            val message = inputArea.text.trim()
            if (message.isEmpty()) {
                return
            }

            GlobalScope.launch(Dispatchers.EDT) {
                inputArea.text = ""
                inputArea.isEnabled = false
                sendButton.setSending(true)
                conversation.chat(
                    Message(
                        Role.USER,
                        message,
                        CodeContextHelper.getCodeContext(toolWindow.project)
                    )
                )
            }
        }

        init {
            GlobalScope.launch(Dispatchers.EDT) {
                conversation.isChatInProgress.collect { isInProgress ->
                    inputArea.isEnabled = !isInProgress
                    sendButton.setSending(isInProgress)
                    if (!isInProgress) {
                        inputArea.requestFocusInWindow()
                    }
                }
            }
        }

        fun getContent() = BorderLayoutPanel().apply {
            addToCenter(conversationPanel.scrollableContainer)
            addToBottom(BorderLayoutPanel().apply {
                addToCenter(JPanel(BorderLayout()).apply {
                    border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
                    add(inputArea, BorderLayout.CENTER)

                    add(JPanel(BorderLayout()).apply {
                        add(sendButton, BorderLayout.EAST)
                    }, BorderLayout.EAST)
                })
            })
        }
    }
}
