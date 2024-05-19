package com.github.fmueller.jarvis.conversation

import com.intellij.util.ui.HTMLEditorKitBuilder
import com.intellij.util.ui.UIUtil
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import org.jdesktop.swingx.VerticalLayout
import java.awt.Color
import javax.swing.BorderFactory
import javax.swing.JEditorPane
import javax.swing.JPanel

class ConversationPanel : JPanel() {

    private val assistantBgColor = UIUtil.getPanelBackground().darker(0.95)
    private val userBgColor = UIUtil.getPanelBackground().darker(0.85)

    init {
        layout = VerticalLayout(1)
        background = UIUtil.getPanelBackground().darker(0.75)
    }

    // TODO recalculate colors on theme change and don't set darker panel background for bright themes
    fun update(conversation: Conversation) {
        removeAll()
        conversation.getMessages().forEach { m ->
            add(JEditorPane().apply {
                editorKit = HTMLEditorKitBuilder.simple()
                text = markdownToHtml("**${if (m.role == Role.ASSISTANT) "Jarvis" else "You"}**\n\n${m.content}")
                isEditable = false
                background = if (m.role == Role.ASSISTANT) assistantBgColor else userBgColor
                border = BorderFactory.createEmptyBorder(5, 5, 10, 5)
            })
        }
        revalidate()
        repaint()
    }

    private fun markdownToHtml(text: String): String {
        val options = MutableDataSet()
        options.set(
            Parser.EXTENSIONS,
            listOf(TablesExtension.create(), StrikethroughExtension.create())
        )
        options.set(HtmlRenderer.SOFT_BREAK, "<br />")
        val parser: Parser = Parser.builder(options).build()
        return HtmlRenderer.builder(options).build().render(parser.parse(text))
    }

    private fun Color.darker(factor: Double): Color {
        val r = (red * factor).toInt().coerceIn(0, 255)
        val g = (green * factor).toInt().coerceIn(0, 255)
        val b = (blue * factor).toInt().coerceIn(0, 255)
        return Color(r, g, b, alpha)
    }
}
