package com.github.fmueller.jarvis.toolWindow

import com.github.fmueller.jarvis.services.OllamaService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.HTMLEditorKitBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import java.awt.*
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.BorderFactory
import javax.swing.JEditorPane
import javax.swing.JPanel
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

        fun getContent() = BorderLayoutPanel().apply {

            val conversationPanel = JEditorPane().apply {
                editorKit = HTMLEditorKitBuilder.simple()
                text = markdownToHtml(
                    """
                    ## Hello!

                    I am Jarvis, your personal coding assistant. I try to be helpful, but I am not perfect.
                    """.trimIndent()
                )
                isEditable = false
            }

            var borderColor = JBColor.GRAY
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
                    if (e.keyCode == KeyEvent.VK_ENTER) {
                        val question = inputArea.text
                        inputArea.text = ""
                        conversationPanel.text = markdownToHtml(ollama.ask(question))
                    }
                }
            })

            addToCenter(conversationPanel)

            addToBottom(BorderLayoutPanel().apply {
                addToCenter(JPanel(BorderLayout()).apply {
                    border = BorderFactory.createEmptyBorder(4, 4, 4, 4) // Margin
                    add(inputArea, BorderLayout.CENTER)
                })
            })
        }

        private fun markdownToHtml(text: String) = run {
            val options = MutableDataSet()
            options.set(
                Parser.EXTENSIONS,
                listOf(TablesExtension.create(), StrikethroughExtension.create())
            )
            options.set(HtmlRenderer.SOFT_BREAK, "<br />\n")
            val parser: Parser = Parser.builder(options).build()
            HtmlRenderer.builder(options).build().render(parser.parse(text))
        }
    }
}
