package com.github.fmueller.jarvis.ui

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class SyntaxHighlightedCodeHelperTest : BasePlatformTestCase() {

    fun `test editor has syntax highlighting`() {
        val helper = SyntaxHighlightedCodeHelper(project)
        val editor = helper.getHighlightedEditor("kotlin", "fun foo() {}")
        assertNotNull(editor)
        editor as EditorEx

        val defaultColor = EditorColorsManager.getInstance().globalScheme.defaultForeground
        val index = editor.document.text.indexOf("fun")
        val iterator = editor.highlighter.createIterator(index)
        val keywordColor = iterator.textAttributes.foregroundColor

        assertNotNull(keywordColor)
        assertTrue(keywordColor != defaultColor)

        helper.disposeEditor(editor)
    }
}
