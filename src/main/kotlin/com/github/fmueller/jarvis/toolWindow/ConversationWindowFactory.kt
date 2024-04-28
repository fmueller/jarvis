package com.github.fmueller.jarvis.toolWindow

import com.github.fmueller.jarvis.services.OllamaService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent

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

        fun getContent() = BorderLayoutPanel().apply {
            val label =
                JBLabel("I am Jarvis, your personal coding assistant. I try to be helpful, but I am not perfect.")
            val responseArea = JBTextArea()

            addToTop(label)
            addToCenter(responseArea)
            addToBottom(JBTextArea().apply {
                text = "Please enter your question"
                addKeyListener(object : KeyAdapter() {
                    override fun keyPressed(e: KeyEvent) {
                        if (e.keyCode == KeyEvent.VK_ENTER) {
                            val question = text
                            text = ""
                            responseArea.text = ollama.ask(question)
                        }
                    }
                })
            })
        }
    }
}
