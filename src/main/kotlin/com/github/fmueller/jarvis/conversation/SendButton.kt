package com.github.fmueller.jarvis.conversation

import com.intellij.icons.AllIcons
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import javax.swing.JButton

/**
 * Button that sends a message when idle and stops generation when a response is in progress.
 */
class SendButton(
    private val onSend: () -> Unit,
    private val onStop: () -> Unit
) : JButton() {

    private var isSending = false

    init {
        icon = AllIcons.Actions.Execute
        toolTipText = "Send message"
        preferredSize = Dimension(32, 32)
        border = JBUI.Borders.empty(4)
        isContentAreaFilled = false
        isFocusPainted = false
        isEnabled = true

        addActionListener {
            if (isSending) {
                onStop()
            } else {
                onSend()
            }
        }
    }

    fun setSending(sending: Boolean) {
        isSending = sending
        if (sending) {
            icon = AllIcons.Actions.Suspend
            toolTipText = "Stop generating response"
        } else {
            icon = AllIcons.Actions.Execute
            toolTipText = "Send message"
        }
    }
}
