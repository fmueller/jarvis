package com.github.fmueller.jarvis.ui

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class SyntaxHighlightedCodeHelperTest : BasePlatformTestCase() {

    fun `test editor has syntax highlighting`() {
        val helper = SyntaxHighlightedCodeHelper(project)
        val editor = helper.getHighlightedEditor("xml", "<some>content</some>")
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
        val editor = helper.getHighlightedEditor("abc", "<some>content</some>")
        assertNotNull(editor)
        helper.disposeEditor(editor as EditorEx)
    }
}
