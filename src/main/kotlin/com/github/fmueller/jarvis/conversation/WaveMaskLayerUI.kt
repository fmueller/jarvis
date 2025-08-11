package com.github.fmueller.jarvis.conversation

import java.awt.AlphaComposite
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.LinearGradientPaint
import java.awt.Rectangle
import java.awt.TexturePaint
import java.awt.image.BufferedImage
import javax.swing.JComponent
import javax.swing.Timer
import javax.swing.plaf.LayerUI
import kotlin.math.max

class WaveMaskLayerUI : LayerUI<JComponent>() {

    private val timer = Timer(50) { tick() }
    private var offset = 0f

    var baseAlpha: Float = 1f
        set(value) {
            field = value
            firePropertyChange("alpha", 0, 1)
        }

    fun start() {
        if (!timer.isRunning) {
            timer.start()
        }
    }

    fun stop() {
        if (timer.isRunning) {
            timer.stop()
            offset = 0f
        }
        firePropertyChange("wave", 0, 1)
    }

    override fun paint(g: Graphics, c: JComponent) {
        val w = c.width
        val h = c.height
        if (w <= 0 || h <= 0) {
            return
        }

        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g2 = img.createGraphics()
        super.paint(g2, c)
        g2.dispose()

        val period = max(1f, c.font.size.toFloat() * 10f)
        if (timer.isRunning) {
            offset %= period
        } else if (baseAlpha >= 1f) {
            (g as Graphics2D).apply {
                composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, baseAlpha)
                drawImage(img, 0, 0, null)
            }
            return
        }

        val gradient = createGradient(period.toInt())
        val mask = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val gMask = mask.createGraphics()
        val tp = TexturePaint(gradient, Rectangle((-offset).toInt(), 0, period.toInt(), 1))
        gMask.paint = tp
        gMask.fillRect(0, 0, w, h)
        gMask.dispose()

        val gImg = img.createGraphics()
        gImg.composite = AlphaComposite.DstIn
        gImg.drawImage(mask, 0, 0, null)
        gImg.dispose()

        val gFinal = g as Graphics2D
        gFinal.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, baseAlpha)
        gFinal.drawImage(img, 0, 0, null)
    }

    private fun tick() {
        offset += 2f
        firePropertyChange("wave", 0, 1)
    }

    private fun createGradient(width: Int): BufferedImage {
        val img = BufferedImage(width, 1, BufferedImage.TYPE_INT_ARGB)
        val fractions = floatArrayOf(0f, 0.5f, 1f)
        val colors = arrayOf(
            java.awt.Color(0f, 0f, 0f, 0.15f),
            java.awt.Color(0f, 0f, 0f, 1f),
            java.awt.Color(0f, 0f, 0f, 0.15f)
        )
        val gp = LinearGradientPaint(0f, 0f, width.toFloat(), 0f, fractions, colors)
        val g2 = img.createGraphics()
        g2.paint = gp
        g2.fillRect(0, 0, width, 1)
        g2.dispose()
        return img
    }
}

