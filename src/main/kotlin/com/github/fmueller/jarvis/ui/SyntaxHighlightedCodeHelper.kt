package com.github.fmueller.jarvis.ui

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.testFramework.LightVirtualFile
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

class SyntaxHighlightedCodeHelper(private val project: Project) {

    private val createdEditors = mutableListOf<Editor>()

    companion object {
        const val EDITOR_PREFIX = "SyntaxHighlightedCodeHelper.HighlightedCode"
        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss.SSS")
    }

    fun getHighlightedEditor(languageId: String, code: String): Editor? {
        if (!ApplicationManager.getApplication().isDispatchThread) {
            return null
        }

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

        val (psiFile, virtualFile, document) = ApplicationManager.getApplication()
            .runReadAction<Triple<PsiFile, LightVirtualFile, Document>> {
                val psiFile = PsiFileFactory.getInstance(project).createFileFromText(fileName, language, code.trim())
                Triple(psiFile, psiFile.virtualFile as LightVirtualFile, psiFile.viewProvider.document)
            }

        val editor = EditorFactory.getInstance().createEditor(document, project, fileType, true) as EditorEx
        createdEditors.add(editor)
        editor.setFile(virtualFile)

        val highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(project, fileType)
        editor.highlighter = highlighter
        editor.isViewer = true
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

        DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
        return editor
    }

    fun disposeEditor(editor: Editor) {
        if (!editor.isDisposed && createdEditors.remove(editor)) {
            EditorFactory.getInstance().releaseEditor(editor)
        }
    }

    fun disposeAllEditors() {
        createdEditors.forEach {
            if (!it.isDisposed) {
                EditorFactory.getInstance().releaseEditor(it)
            }
        }
        createdEditors.clear()
    }
}
