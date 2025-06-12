package com.github.fmueller.jarvis.ui

import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
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

        val file = PsiFileFactory.getInstance(project)
            .createFileFromText(
                "${EDITOR_PREFIX}.${Random.nextLong()}.${
                    LocalDateTime.now().format(formatter)
                }.${fileType.defaultExtension}", language, code.trim()
            )

        val editor = EditorFactory.getInstance().createEditor(file.viewProvider.document, project, fileType, true)
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
