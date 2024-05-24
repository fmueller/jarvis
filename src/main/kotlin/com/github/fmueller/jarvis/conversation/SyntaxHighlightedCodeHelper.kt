package com.github.fmueller.jarvis.conversation;

import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ignore.lang.IgnoreLanguage
import com.intellij.psi.PsiFileFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SyntaxHighlightedCodeHelper(private val project: Project) {

    companion object {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss.SSS")
    }

    // TODO add disposal of created editors
    fun getHighlightedEditor(languageId: String, code: String): Editor? {
        // TODO test with some Kotlin code and see if the language is correctly detected
        val language = Language.findLanguageByID(languageId) ?: IgnoreLanguage.INSTANCE
        val fileType = language.associatedFileType ?: return null

        val file = PsiFileFactory.getInstance(project)
            .createFileFromText(
                "highlighted_code.created_at.${
                    LocalDateTime.now().format(formatter)
                }.${fileType.defaultExtension}", language, code.trim()
            )

        val editor = EditorFactory.getInstance().createEditor(file.viewProvider.document, project, fileType, true)

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
}
