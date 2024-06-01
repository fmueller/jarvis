package com.github.fmueller.jarvis.conversation

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.UIManager
import javax.swing.border.AbstractBorder

class InputArea : JBTextArea() {

    var placeholderText: String? = null
        set(value) {
            field = value
            setToolTipText(value)
            repaint()
        }

    private var borderColor = JBColor.GRAY

    init {
        lineWrap = true
        wrapStyleWord = true
        font = UIManager.getFont("Label.font")
        border = object : AbstractBorder() {
            override fun getBorderInsets(c: Component?): Insets {
                return JBUI.insets(9)
            }

            override fun paintBorder(c: Component?, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
                val g2 = g.create() as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.color = borderColor
                g2.drawRoundRect(x, y, width - 1, height - 1, 10, 10)
                g2.dispose()
            }
        }

        addFocusListener(object : FocusListener {
            override fun focusGained(e: FocusEvent?) {
                borderColor = JBColor.namedColor("selection.background", JBColor.BLUE)
            }

            override fun focusLost(e: FocusEvent?) {
                borderColor = JBColor.GRAY
            }
        })

        addKeyListener(object : KeyAdapter() {

            // do not remove this method, its purpose is to prevent the default behavior of the Enter key
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER && !e.isShiftDown) {
                    e.consume()
                }
            }

            override fun keyReleased(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER && e.isShiftDown) {
                    append("\n")
                }
            }
        })
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        if (text.isEmpty() && placeholderText != null) {
            val g2 = g as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.color = JBColor.GRAY

            val fm = g2.fontMetrics
            val centeredY = (height - fm.height) / 2 + fm.ascent

            g2.drawString(placeholderText!!, 10, centeredY)
        }
    }
}