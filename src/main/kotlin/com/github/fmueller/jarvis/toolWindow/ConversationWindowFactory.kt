package com.github.fmueller.jarvis.toolWindow

import com.github.fmueller.jarvis.conversation.*
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.util.io.await
import com.intellij.util.ui.components.BorderLayoutPanel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
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

    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    class ConversationWindow(private val toolWindow: ToolWindow) {

        private val conversation = toolWindow.project.service<Conversation>()
        private val conversationPanel = ConversationPanel(conversation, toolWindow.project)

        init {
            GlobalScope.launch(Dispatchers.EDT) {
                conversation.isChatInProgress
                    .flatMapLatest { inProgress ->
                        flow {
                            sendToolbar.updateActionsAsync().await()
                            emit(inProgress)
                        }
                    }
                    .collect { inProgress ->
                        inputArea.isEnabled = !inProgress
                        if (!inProgress) {
                            inputArea.requestFocusInWindow()
                        }
                    }
            }
        }

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

        private val sendAction = object : AnAction(AllIcons.Actions.Execute) {

            init {
                templatePresentation.text = "Send"
            }

            override fun getActionUpdateThread(): ActionUpdateThread {
                return ActionUpdateThread.EDT
            }

            override fun actionPerformed(e: AnActionEvent) {
                if (conversation.isChatInProgress.value) {
                    conversation.cancelCurrentChat()
                    conversation.addMessage(Message.info("Response generation was cancelled"))
                    inputArea.isEnabled = true
                } else {
                    sendMessage()
                }
            }

            override fun update(e: AnActionEvent) {
                val inProgress = conversation.isChatInProgress.value
                e.presentation.icon = if (inProgress) AllIcons.Actions.Suspend else AllIcons.Actions.Execute
                e.presentation.isEnabled = !inProgress && inputArea.text.trim().isNotEmpty() || inProgress
                e.presentation.text = if (inProgress) "Stop" else "Send"
            }
        }

        private val sendToolbar = ActionManager.getInstance()
            .createActionToolbar("Jarvis.Chat.Send", DefaultActionGroup(sendAction), false)
            .apply {
                targetComponent = inputArea
                isReservePlaceAutoPopupIcon = false
            }

        private fun sendMessage() {
            val message = inputArea.text.trim()
            if (message.isEmpty()) {
                return
            }

            GlobalScope.launch(Dispatchers.EDT) {
                inputArea.text = ""
                inputArea.isEnabled = false
                conversation.chat(
                    Message(
                        Role.USER,
                        message,
                        CodeContextHelper.getCodeContext(toolWindow.project)
                    )
                )
            }
        }

        fun getContent() = BorderLayoutPanel().apply {
            addToCenter(conversationPanel.scrollableContainer)
            addToBottom(BorderLayoutPanel().apply {
                addToCenter(JPanel(BorderLayout()).apply {
                    border = BorderFactory.createEmptyBorder(10, 10, 10, 0)
                    add(inputArea, BorderLayout.CENTER)
                    add(sendToolbar.component, BorderLayout.EAST)
                })
            })
        }
    }
}
