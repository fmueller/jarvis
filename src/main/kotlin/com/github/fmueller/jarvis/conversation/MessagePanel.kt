package com.github.fmueller.jarvis.conversation

import com.github.fmueller.jarvis.ui.SyntaxHighlightedCodeHelper
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.HTMLEditorKitBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import org.jdesktop.swingx.VerticalLayout
import java.awt.BorderLayout
import java.awt.Font
import java.util.regex.Pattern
import com.intellij.ui.HideableDecorator
import javax.swing.BorderFactory
import javax.swing.JEditorPane
import javax.swing.JPanel

class MessagePanel(initialMessage: Message, project: Project) : JPanel(), Disposable {

    private companion object {
        private val codeBlockPattern = Pattern.compile("```(\\w+)?\\n(.*?)\\n```", Pattern.DOTALL)
        private val assistantBgColor = { UIUtil.getPanelBackground() }
        private val userBgColor = { UIUtil.getTextFieldBackground() }
    }

    sealed interface ParsedContent
    data class Content(val markdown: String) : ParsedContent
    data class Code(val languageId: String, val content: String) : ParsedContent
    data class Reasoning(val markdown: String, val isInProgress: Boolean = false) : ParsedContent

    private val highlightedCodeHelper = SyntaxHighlightedCodeHelper(project)

    // visibility for testing
    val parsed = mutableListOf<ParsedContent>()

    private var _message: Message = initialMessage
    var message: Message
        get() = _message
        set(value) {
            _message = value
            updatePanel()
        }

    init {
        resetUI()
        updatePanel()
    }

    override fun updateUI() {
        super.updateUI()
        resetUI()
        updatePanel()
    }

    override fun dispose() {
        parsed.clear()
        highlightedCodeHelper.disposeAllEditors()
    }

    @Suppress("SENSELESS_COMPARISON") // message can be null
    private fun updatePanel() {
        if (message == null) {
            return
        }

        val newParsedContent = parse(message.contentWithClosedTrailingCodeBlock())
        synchronized(treeLock) {
            for (i in newParsedContent.indices) {
                if (i >= parsed.size) {
                    val newContent = newParsedContent.subList(i, newParsedContent.size)
                    parsed.addAll(newContent)
                    render(newContent)
                    break
                }

                val old = parsed[i]
                val new = newParsedContent[i]

                if (i == newParsedContent.lastIndex && isUpdatableParsedContent(old, new)) {
                    when (old) {
                        is Content -> getComponent(componentCount - 1).let {
                            (it as JEditorPane).text = markdownToHtml((new as Content).markdown)
                        }

                        is Code -> {
                            val lastComponent = getComponent(componentCount - 1)
                            if (lastComponent is JBScrollPane && lastComponent.viewport.view is Editor) {
                                val editor = lastComponent.viewport.view as Editor
                                highlightedCodeHelper.disposeEditor(editor)
                            }
                            remove(componentCount - 1)
                            addHighlightedCode((new as Code).languageId, new.content)
                        }

                        is Reasoning -> {
                            remove(componentCount - 1)
                            addReasoning((new as Reasoning).markdown, new.isInProgress)
                        }
                    }
                    break
                }

                if (isDifferentParsedContent(old, new)) {
                    val newContent = newParsedContent.subList(i, newParsedContent.size)
                    parsed.subList(i, parsed.size).clear()
                    parsed.addAll(newContent)
                    removeAllComponentsAfter(i + 1)
                    render(newContent)
                    break
                }
            }
        }
    }

    private fun isUpdatableParsedContent(old: ParsedContent, new: ParsedContent): Boolean {
        if (old::class != new::class) {
            return false
        }

        return when (old) {
            is Content -> (new as Content).markdown.startsWith(old.markdown)
            is Code -> (new as Code).content.startsWith(old.content) && new.languageId == old.languageId
            is Reasoning -> (new as Reasoning).markdown.startsWith(old.markdown)
        }
    }

    private fun isDifferentParsedContent(old: ParsedContent, new: ParsedContent): Boolean {
        if (old::class != new::class) {
            return true
        }

        return when (old) {
            is Content -> (new as Content).markdown != old.markdown
            is Code -> (new as Code).content != old.content || new.languageId != old.languageId
            is Reasoning -> (new as Reasoning).markdown != old.markdown
        }
    }

    private fun resetUI() {
        if (message == null) {
            return
        }

        parsed.clear()
        removeAll()
        highlightedCodeHelper.disposeAllEditors()

        layout = VerticalLayout(5)
        background = when (message.role) {
            Role.ASSISTANT -> assistantBgColor()
            Role.USER -> userBgColor()
            Role.INFO -> assistantBgColor()
        }
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, JBUI.CurrentTheme.ToolWindow.borderColor()),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        )

        add(JBLabel(
            when (message.role) {
                Role.ASSISTANT -> "Jarvis"
                Role.USER -> "You"
                Role.INFO -> "Info"
            }
        ).apply {
            font = font.deriveFont(Font.BOLD)
            border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
        })
    }

    private fun parse(markdown: String): List<ParsedContent> {
        val parsed = mutableListOf<ParsedContent>()

        var remaining = markdown
        if (remaining.startsWith("<think>")) {
            val end = remaining.indexOf("</think>")
            if (end != -1) {
                val reasoning = remaining.substring("<think>".length, end)
                parsed.add(Reasoning(reasoning, false))
                remaining = remaining.substring(end + "</think>".length)
            } else {
                val reasoning = remaining.removePrefix("<think>")
                parsed.add(Reasoning(reasoning, true))
                remaining = ""
            }
        }

        val matcher = codeBlockPattern.matcher(remaining)
        var lastEnd = 0
        while (matcher.find()) {
            // Add preceding non-code content
            if (matcher.start() > lastEnd) {
                val nonCodeMarkdown = remaining.substring(lastEnd, matcher.start())
                parsed.add(Content(nonCodeMarkdown))
            }

            // Add syntax-highlighted code
            val languageId = matcher.group(1)?.lowercase()
            val code = matcher.group(2)
            parsed.add(Code(languageId ?: "plaintext", code))

            lastEnd = matcher.end()
        }

        // Add any remaining non-code content
        if (lastEnd < remaining.length) {
            val remainingNonCodeMarkdown = remaining.substring(lastEnd)
            parsed.add(Content(remainingNonCodeMarkdown))
        }

        return parsed
    }

    private fun render(parsed: List<ParsedContent>) {
        parsed.forEach {
            when (it) {
                is Content -> addNonCodeContent(it.markdown)
                is Code -> addHighlightedCode(it.languageId, it.content)
                is Reasoning -> addReasoning(it.markdown, it.isInProgress)
            }
        }
    }

    private fun addNonCodeContent(markdown: String) {
        val globalScheme = EditorColorsManager.getInstance().globalScheme
        val functionDeclaration = TextAttributesKey.createTextAttributesKey("DEFAULT_FUNCTION_DECLARATION")
        val codeColor =
            globalScheme.getAttributes(functionDeclaration).foregroundColor ?: globalScheme.defaultForeground
        val outerPanelBackground = background
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
                            background-color: rgb(${outerPanelBackground.red}, ${outerPanelBackground.green}, ${outerPanelBackground.blue});
                            color: rgb(${codeColor.red}, ${codeColor.green}, ${codeColor.blue});
                            font-size: 0.9em;
                        }
                    """.trimIndent()
                )
            }
            text = markdownToHtml(markdown)
            isEditable = false
            background = outerPanelBackground
            border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
        }
        add(editorPane)
    }

    private fun addReasoning(markdown: String, isInProgress: Boolean = false) {
        val outer = JPanel().apply { layout = BorderLayout() }
        val content = JPanel().apply { layout = BorderLayout() }
        val decorator = HideableDecorator(outer, "Reasoning", false)
        decorator.setContentComponent(content)
        decorator.setOn(isInProgress) // Open the decorator if reasoning is still in progress

        val editorPane = JEditorPane().apply {
            editorKit = HTMLEditorKitBuilder.simple()
            text = markdownToHtml(markdown)
            isEditable = false
            background = outer.background
            border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
        }
        content.add(editorPane, BorderLayout.CENTER)
        add(outer)
    }

    private fun addHighlightedCode(languageId: String, code: String) {
        val editor = highlightedCodeHelper.getHighlightedEditor(languageId, code)
        if (editor != null) {
            editor.contentComponent.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
            val outerPanelBackground = background
            add(JBScrollPane(editor.component).apply {
                viewport.view.background = outerPanelBackground
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

    private fun removeAllComponentsAfter(index: Int) {
        for (i in componentCount - 1 downTo index) {
            remove(i)
        }
    }
}
