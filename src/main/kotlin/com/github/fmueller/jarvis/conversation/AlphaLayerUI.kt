package com.github.fmueller.jarvis.conversation

import java.awt.AlphaComposite
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.JComponent
import javax.swing.plaf.LayerUI

/**
 * [LayerUI] that applies a uniform alpha composite to its component.
 *
 * Adjusting [alpha] allows for simple fade-in and fade-out transitions.
 */
class AlphaLayerUI : LayerUI<JComponent>() {

    var alpha: Float = 1f
        set(value) {
            field = value
            firePropertyChange("alpha", 0, 1)
        }

    override fun paint(g: Graphics, c: JComponent) {
        val g2 = g as Graphics2D
        val original = g2.composite
        g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha)
        super.paint(g2, c)
        g2.composite = original
    }
}

