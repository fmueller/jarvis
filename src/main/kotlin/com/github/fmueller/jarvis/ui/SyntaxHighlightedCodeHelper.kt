package com.github.fmueller.jarvis.ui

import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

class SyntaxHighlightedCodeHelper(private val project: Project) {

    private val createdEditors = mutableListOf<Editor>()

    companion object {
        const val EDITOR_PREFIX = "SyntaxHighlightedCodeHelper.HighlightedCode."
        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss.SSS")
    }

    fun getHighlightedEditor(languageId: String, code: String): Editor? {
        val language = Language.findLanguageByID(languageId) ?: Language.ANY
        val fileType = language.associatedFileType ?: return null

        val trimmed = code.trim()
        val name = "${EDITOR_PREFIX}.${Random.nextLong()}.${
            LocalDateTime.now().format(formatter)
        }.${fileType.defaultExtension}"
        val virtualFile = LightVirtualFile(name, fileType, trimmed)
        val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: return null

        val editor = EditorFactory.getInstance()
            .createEditor(document, project, fileType, /*viewer*/ true) as EditorEx
        editor.setVirtualFile(virtualFile)

        val highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(project, fileType)
        editor.setHighlighter(highlighter)

        DaemonCodeAnalyzer.getInstance(project).restart(editor.virtualFile)

        createdEditors.add(editor)

        editor.settings.apply {
            isLineNumbersShown = false
            isFoldingOutlineShown = false
            isLineMarkerAreaShown = false
            isBlinkCaret = false
            isCaretRowShown = false
            isBlockCursor = false
            isShowIntentionBulb = false
            isIndentGuidesShown = false
            isAutoCodeFoldingEnabled = false
            isDndEnabled = false
            isVirtualSpace = false
            isAdditionalPageAtBottom = false
            additionalLinesCount = 0
            additionalColumnsCount = 0
        }
        return editor
    }

    fun disposeEditor(editor: Editor) {
        if (createdEditors.remove(editor)) {
            EditorFactory.getInstance().releaseEditor(editor)
        }
    }

    fun disposeAllEditors() {
        createdEditors.forEach { EditorFactory.getInstance().releaseEditor(it) }
        createdEditors.clear()
    }
}
