package com.github.fmueller.jarvis.ui

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoFilter
import com.intellij.codeInsight.highlighting.HighlightErrorFilter
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile

class IgnoreCodeErrorsFilter : HighlightErrorFilter(), HighlightInfoFilter {

    override fun accept(highlightInfo: HighlightInfo, file: PsiFile?): Boolean {
        return !(file?.name?.startsWith(SyntaxHighlightedCodeHelper.EDITOR_PREFIX) ?: false)
    }

    override fun shouldHighlightErrorElement(element: PsiErrorElement): Boolean {
        return false
    }
}
