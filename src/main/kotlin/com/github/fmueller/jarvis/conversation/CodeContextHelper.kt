package com.github.fmueller.jarvis.conversation

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project

object CodeContextHelper {

    fun getCodeContext(project: Project): CodeContext? {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        if (editor != null) {
            val selection = editor.selectionModel
            if (selection.hasSelection()) {
                // TODO: detect the language of the selected code
                return CodeContext(Code(selection.selectedText!!))
            }
        }

        return null
    }
}
