package com.github.fmueller.jarvis.conversation

import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JComponent

/**
 * A component that draws a timeline indicator with a dot and connecting line segments.
 * Used in the reasoning panel's timeline layout to show paragraph progression.
 */
class TimelineIndicator(
    private val showTopLine: Boolean = true,
    private val showBottomLine: Boolean = true
) : JComponent() {

    companion object {
        val DOT_DIAMETER = JBUI.scale(6)
        private val LINE_WIDTH = JBUI.scale(1)
        val COMPONENT_WIDTH = JBUI.scale(16)
    }

    init {
        preferredSize = Dimension(COMPONENT_WIDTH, 0) // Height will be determined by parent
        minimumSize = Dimension(COMPONENT_WIDTH, DOT_DIAMETER)
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val color = UIUtil.getLabelDisabledForeground()
        g2.color = color

        val centerX = width / 2
        val dotRadius = DOT_DIAMETER / 2
        val dotY = dotRadius // Align with beginning of line

        // Draw top line segment if needed
        if (showTopLine && dotY > 0) {
            val lineX = centerX - LINE_WIDTH / 2
            g2.fillRect(lineX, 0, LINE_WIDTH, dotY - dotRadius)
        }

        // Draw the dot
        g2.fillOval(centerX - dotRadius, dotY - dotRadius, DOT_DIAMETER, DOT_DIAMETER)

        // Draw bottom line segment if needed
        if (showBottomLine && dotY + dotRadius < height) {
            val lineX = centerX - LINE_WIDTH / 2
            g2.fillRect(lineX, dotY + dotRadius, LINE_WIDTH, height - (dotY + dotRadius))
        }
    }
}