package com.github.fmueller.jarvis.conversation

import com.intellij.icons.AllIcons
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import javax.swing.JButton

class StopButton(private val onStop: () -> Unit) : JButton() {

    init {
        icon = AllIcons.Actions.Suspend
        toolTipText = "Stop generating response"
        preferredSize = Dimension(32, 32)
        border = JBUI.Borders.empty(4)
        isContentAreaFilled = false
        isFocusPainted = false

        addActionListener {
            onStop()
        }
    }

    fun updateVisibility(visible: Boolean) {
        isVisible = visible
        isEnabled = visible
    }
}
