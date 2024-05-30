package com.github.fmueller.jarvis.ui

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoFilter
import com.intellij.codeInsight.highlighting.HighlightErrorFilter
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile

class IgnoreCodeErrorsFilter : HighlightErrorFilter(), HighlightInfoFilter {

    override fun accept(highlightInfo: HighlightInfo, file: PsiFile?): Boolean {
        // TODO consider using a more specific check for the file type, e.g. mark created editors with a custom property
        return !(file?.name?.startsWith("highlighted_code.created_at.") ?: false)
    }

    override fun shouldHighlightErrorElement(element: PsiErrorElement): Boolean {
        return false
    }
}
