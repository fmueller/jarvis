package com.github.fmueller.jarvis.conversation;

import com.github.fmueller.jarvis.conversation.ColorHelper.darker
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.HTMLEditorKitBuilder
import com.intellij.util.ui.UIUtil
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import org.jdesktop.swingx.VerticalLayout
import java.util.*
import java.util.regex.Pattern
import javax.swing.BorderFactory
import javax.swing.JEditorPane
import javax.swing.JPanel

class MessagePanel(message: Message, project: Project) : JPanel() {

    private companion object {
        private val codeBlockPattern = Pattern.compile("```(\\w+)?\\n(.*?)\\n```", Pattern.DOTALL)
        private val assistantBgColor = UIUtil.getPanelBackground().darker(0.95)
        private val userBgColor = UIUtil.getPanelBackground().darker(0.85)
    }

    private val syntaxHelper = SyntaxHighlightedCodeHelper(project)
    private val bgColor = if (message.role == Role.ASSISTANT) assistantBgColor else userBgColor

    init {
        // TODO use label in MarkdownContentPanel to show the role
        background = bgColor
        layout = VerticalLayout(5)
        border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        val markdown = "**${if (message.role == Role.ASSISTANT) "Jarvis" else "You"}**\n\n${message.content}"
        parseAndRenderMarkdown(markdown)
    }

    // TODO add some tests for the parsing logic
    private fun parseAndRenderMarkdown(markdown: String) {
        val matcher = codeBlockPattern.matcher(markdown)
        var lastEnd = 0
        while (matcher.find()) {
            // Add preceding non-code content
            if (matcher.start() > lastEnd) {
                val nonCodeMarkdown = markdown.substring(lastEnd, matcher.start())
                addNonCodeContent(nonCodeMarkdown)
            }

            // Add syntax-highlighted code
            val languageId = matcher.group(1)?.uppercase(Locale.getDefault())
            val code = matcher.group(2)
            addHighlightedCode(languageId ?: "plaintext", code)

            lastEnd = matcher.end()
        }

        // Add any remaining non-code content
        if (lastEnd < markdown.length) {
            val remainingNonCodeMarkdown = markdown.substring(lastEnd)
            addNonCodeContent(remainingNonCodeMarkdown)
        }
    }

    private fun addNonCodeContent(markdown: String) {
        // TODO add some formatting to the generated HTML for paragraphs, lists, inline code etc.
        val editorPane = JEditorPane().apply {
            editorKit = HTMLEditorKitBuilder.simple()
            text = markdownToHtml(markdown)
            isEditable = false
            background = bgColor
        }
        add(editorPane)
    }

    private fun addHighlightedCode(languageId: String, code: String) {
        // TODO add some nice borders to the code block and potentially some padding
        val editor = syntaxHelper.getHighlightedEditor(languageId, code)
        if (editor != null) {
            editor.contentComponent.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
            add(JBScrollPane(editor.component))
        } else {
            addNonCodeContent(code)
        }
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
}
