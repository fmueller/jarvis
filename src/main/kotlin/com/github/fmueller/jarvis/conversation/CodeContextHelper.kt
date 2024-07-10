package com.github.fmueller.jarvis.conversation

import com.intellij.lang.Language
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager

object CodeContextHelper {

    fun getCodeContext(project: Project): CodeContext? {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        if (editor != null) {
            val selection = editor.selectionModel
            if (selection.hasSelection()) {
                val virtualFile = FileDocumentManager.getInstance().getFile(editor.document)
                val language = if (virtualFile != null) {
                    val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                    psiFile?.language
                } else null

                return CodeContext(Code(selection.selectedText!!, language ?: Language.ANY))
            }
        }

        return null
    }
}
