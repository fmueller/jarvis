package com.github.fmueller.jarvis.conversation

import com.intellij.openapi.Disposable
import org.jetbrains.annotations.VisibleForTesting
import java.awt.Dimension
import javax.swing.JEditorPane
import javax.swing.JLayer
import javax.swing.JPanel
import javax.swing.Timer
import javax.swing.OverlayLayout
import javax.swing.BorderFactory
import com.intellij.util.ui.UIUtil

class ReasoningMessagePanel : JPanel(), Disposable {

    private val currentPane = createPane()
    private val previousPane = createPane()

    private val currentLayerUI = WaveMaskLayerUI()
    private val previousLayerUI = WaveMaskLayerUI()

    private val currentLayer = JLayer(currentPane, currentLayerUI)
    private val previousLayer = JLayer(previousPane, previousLayerUI)

    private var fadeTimer: Timer? = null
    private var maxHeight = 0

    @VisibleForTesting
    var displayedText: String = ""
        private set

    init {
        layout = OverlayLayout(this)
        border = BorderFactory.createEmptyBorder()
        add(previousLayer)
        add(currentLayer)
    }

    fun update(reasoning: Message.Reasoning) {
        val text = if (reasoning.isInProgress) {
            extractLastFullParagraph(reasoning.markdown)
        } else {
            reasoning.markdown
        }

        if (displayedText == text) {
            if (reasoning.isInProgress) {
                currentLayerUI.start()
            } else {
                currentLayerUI.stop()
            }
            return
        }

        if (displayedText.isEmpty()) {
            currentPane.text = text
            displayedText = text
            updateHeight()
            if (reasoning.isInProgress) {
                currentLayerUI.start()
            }
            return
        }

        previousPane.text = currentPane.text
        previousLayerUI.baseAlpha = 1f
        previousLayerUI.stop()

        currentPane.text = text
        currentLayerUI.baseAlpha = 0f
        if (reasoning.isInProgress) {
            currentLayerUI.start()
        } else {
            currentLayerUI.stop()
            currentLayerUI.baseAlpha = 1f
        }
        displayedText = text
        updateHeight()
        startFade()
    }

    private fun startFade() {
        fadeTimer?.stop()
        fadeTimer = Timer(60) {
            val step = 0.05f
            previousLayerUI.baseAlpha = (previousLayerUI.baseAlpha - step).coerceAtLeast(0f)
            currentLayerUI.baseAlpha = (currentLayerUI.baseAlpha + step).coerceAtMost(1f)
            repaint()
            if (previousLayerUI.baseAlpha <= 0f && currentLayerUI.baseAlpha >= 1f) {
                fadeTimer?.stop()
                previousPane.text = ""
            }
        }.apply { start() }
    }

    private fun updateHeight() {
        val prefHeight = currentPane.preferredSize.height
        if (prefHeight > maxHeight) {
            maxHeight = prefHeight
        }
        preferredSize = Dimension(0, maxHeight)
        maximumSize = Dimension(Int.MAX_VALUE, maxHeight)
    }

    private fun createPane(): JEditorPane {
        return JEditorPane().apply {
            isEditable = false
            contentType = "text/plain"
            border = null
            background = UIUtil.getPanelBackground()
            foreground = UIUtil.getLabelDisabledForeground()
        }
    }

    private fun extractLastFullParagraph(markdown: String): String {
        val paragraphs = markdown.split("\n\n")
        return paragraphs.dropLast(1).lastOrNull()?.trim() ?: ""
    }

    override fun dispose() {
        fadeTimer?.stop()
        currentLayerUI.stop()
        previousLayerUI.stop()
    }
}

