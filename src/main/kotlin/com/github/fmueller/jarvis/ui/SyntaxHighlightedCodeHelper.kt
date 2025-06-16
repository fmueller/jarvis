package com.github.fmueller.jarvis.ui

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.testFramework.LightVirtualFile
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
        val language = Language.findLanguageByID(languageId)
            ?: Language.findLanguageByID(languageId.uppercase())
            ?: Language.findLanguageByID("IgnoreLang")
            ?: Language.findLanguageByID("IgnoreLang".lowercase())
            ?: Language.findLanguageByID("IgnoreLang".uppercase())
            ?: Language.ANY

        val fileType = language.associatedFileType ?: return null

        val fileName = "${EDITOR_PREFIX}.${Random.nextLong()}.${
            LocalDateTime.now().format(formatter)
        }.${fileType.defaultExtension}"

        val psiFile = PsiFileFactory.getInstance(project)
            .createFileFromText(fileName, language, code.trim())

        val virtualFile = psiFile.virtualFile as LightVirtualFile
        val document = psiFile.viewProvider.document

        val editor = EditorFactory.getInstance().createEditor(document, project, fileType, true) as EditorEx
        editor.setFile(virtualFile)

        val highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(project, fileType)
        editor.highlighter = highlighter

        DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
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
