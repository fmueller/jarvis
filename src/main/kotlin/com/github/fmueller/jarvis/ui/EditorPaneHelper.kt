package com.github.fmueller.jarvis.ui

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.util.ui.HTMLEditorKitBuilder
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import java.awt.Color
import javax.swing.BorderFactory
import javax.swing.JEditorPane

object EditorPaneHelper {

    private val options = MutableDataSet()
    private val parser: Parser

    init {
        options.set(
            Parser.EXTENSIONS,
            listOf(TablesExtension.create(), StrikethroughExtension.create())
        )
        options.set(HtmlRenderer.SOFT_BREAK, "<br />")
        parser = Parser.builder(options).build()
    }

    fun createMarkdownPane(markdown: String, outerPanelBackground: Color): JEditorPane {
        val globalScheme = EditorColorsManager.getInstance().globalScheme
        val functionDeclaration = TextAttributesKey.createTextAttributesKey("DEFAULT_FUNCTION_DECLARATION")
        val defaultForeground = globalScheme.defaultForeground
        val codeColor = globalScheme.getAttributes(functionDeclaration).foregroundColor ?: defaultForeground
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
                        body {
                            color: rgb(${defaultForeground.red}, ${defaultForeground.green}, ${defaultForeground.blue});
                        }
                    """.trimIndent()
                )
            }
            text = markdownToHtml(markdown)
            isEditable = false
            background = outerPanelBackground
            border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
        }
        return editorPane
    }

    private fun markdownToHtml(text: String): String {
        return HtmlRenderer.builder(options).build().render(parser.parse(text))
    }
}
