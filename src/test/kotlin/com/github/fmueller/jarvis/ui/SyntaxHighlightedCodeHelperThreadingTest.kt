package com.github.fmueller.jarvis.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.runInEdtAndWait
import java.util.concurrent.Callable

class SyntaxHighlightedCodeHelperThreadingTest : BasePlatformTestCase() {
    fun `test getHighlightedEditor can run outside read action`() {
        val helper = SyntaxHighlightedCodeHelper(project)
        val future = ApplicationManager.getApplication().executeOnPooledThread(Callable<EditorEx?> {
            var editor: EditorEx? = null
            runInEdtAndWait {
                editor = helper.getHighlightedEditor("xml", "<some>content</some>") as EditorEx?
            }
            editor
        })
        val editor = future.get()
        assertNotNull(editor)
        helper.disposeEditor(editor!!)
    }
}
