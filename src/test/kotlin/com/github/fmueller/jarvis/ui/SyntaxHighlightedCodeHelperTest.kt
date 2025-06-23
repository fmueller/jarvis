package com.github.fmueller.jarvis.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.runInEdtAndGet
import com.intellij.testFramework.runInEdtAndWait
import java.util.concurrent.TimeUnit

class SyntaxHighlightedCodeHelperTest : BasePlatformTestCase() {

    fun `test editor has syntax highlighting`() {
        val helper = SyntaxHighlightedCodeHelper(project)
        val editor = runInEdtAndGet {
            helper.getHighlightedEditor("xml", "<some>content</some>")
        }
        assertNotNull(editor)
        editor as EditorEx

        val defaultColor = EditorColorsManager.getInstance().globalScheme.defaultForeground
        val index = editor.document.text.indexOf("some")
        val iterator = editor.highlighter.createIterator(index)
        val keywordColor = iterator.textAttributes.foregroundColor

        assertNotNull(keywordColor)
        assertTrue(keywordColor != defaultColor)

        helper.disposeEditor(editor)
    }

    fun `test editor works with unsupported language`() {
        val helper = SyntaxHighlightedCodeHelper(project)
        val editor = runInEdtAndGet {
            helper.getHighlightedEditor("abc", "<some>content</some>")
        }
        assertNotNull(editor)
        helper.disposeEditor(editor!!)
    }

    fun `test getHighlightedEditor with empty content`() {
        val helper = SyntaxHighlightedCodeHelper(project)
        val editor = runInEdtAndGet {
            helper.getHighlightedEditor("xml", "")
        }
        assertNotNull(editor)
        helper.disposeEditor(editor!!)
    }

    fun `test disposeEditor with existing editor`() {
        val helper = SyntaxHighlightedCodeHelper(project)
        val editor = runInEdtAndGet {
            helper.getHighlightedEditor("xml", "test")
        }
        assertNotNull(editor)
        helper.disposeEditor(editor!!)
    }

    fun `test disposeAllEditors`() {
        val helper = SyntaxHighlightedCodeHelper(project)
        runInEdtAndWait {
            helper.getHighlightedEditor("xml", "test")
        }
        runInEdtAndWait {
            helper.getHighlightedEditor("xml", "anotherTest")
        }
        helper.disposeAllEditors()
    }

    fun `test getHighlightedEditor returns null on non-EDT`() {
        val helper = SyntaxHighlightedCodeHelper(project)
        val future = ApplicationManager.getApplication().executeOnPooledThread<Editor?> {
            helper.getHighlightedEditor("xml", "test")
        }
        val editor = future.get(5, TimeUnit.SECONDS)
        assertNull(editor)
    }
}
