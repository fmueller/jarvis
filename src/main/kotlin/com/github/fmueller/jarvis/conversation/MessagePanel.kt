package com.github.fmueller.jarvis.conversation;

import com.github.fmueller.jarvis.ui.ColorHelper.darker
import com.github.fmueller.jarvis.ui.SyntaxHighlightedCodeHelper
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.HTMLEditorKitBuilder
import com.intellij.util.ui.UIUtil
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import org.jdesktop.swingx.VerticalLayout
import java.awt.Font
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
        background = bgColor
        layout = VerticalLayout(5)
        border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        add(JBLabel(if (message.role == Role.ASSISTANT) "Jarvis" else "You").apply {
            font = font.deriveFont(Font.BOLD)
            border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
        })
        parseAndRenderMarkdown(message.content)
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
        val globalScheme = EditorColorsManager.getInstance().globalScheme
        val functionDeclaration = TextAttributesKey.createTextAttributesKey("DEFAULT_FUNCTION_DECLARATION")
        val codeColor =
            globalScheme.getAttributes(functionDeclaration).foregroundColor ?: globalScheme.defaultForeground
        val editorPane = JEditorPane().apply {
            editorKit = HTMLEditorKitBuilder.simple().apply {
                styleSheet.addRule(
                    """
                        p {
                            margin: 4px 0;
                        }
                        ul, ol {
                            margin-top: 4px;
                            margin-bottom: 8px;
                        }
                        h1, h2, h3, h4, h5, h6 {
                            margin-top: 8px;
                            margin-bottom: 0;
                        }
                        code {
                            background-color: rgb(${bgColor.red}, ${bgColor.green}, ${bgColor.blue});
                            color: rgb(${codeColor.red}, ${codeColor.green}, ${codeColor.blue});
                            font-size: 0.9em;
                        }
                    """.trimIndent()
                )
            }
            text = markdownToHtml(markdown)
            isEditable = false
            background = bgColor
            border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
        }
        add(editorPane)
    }

    private fun addHighlightedCode(languageId: String, code: String) {
        val editor = syntaxHelper.getHighlightedEditor(languageId, code)
        if (editor != null) {
            editor.contentComponent.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
            add(JBScrollPane(editor.component).apply {
                background = bgColor
                border = BorderFactory.createEmptyBorder(10, 5, 10, 5)
            })
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
