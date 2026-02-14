package com.github.fmueller.jarvis.conversation

import com.github.fmueller.jarvis.ui.EditorPaneHelper
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.jetbrains.annotations.VisibleForTesting
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

class ReasoningMessagePanel : JPanel(), Disposable {

    private val currentPane = createPane()
    private val previousPane = createPane()

    private val currentLayerUI = AlphaLayerUI()
    private val previousLayerUI = AlphaLayerUI()

    private val currentLayer = JLayer(currentPane, currentLayerUI)
    private val previousLayer = JLayer(previousPane, previousLayerUI)

    private var fadeTimer: Timer? = null
    private var maxHeight = 0
    private var lastFadeStartMs = 0L
    private var isExpanded = false
    private var timelineContainer: JBScrollPane? = null
    private var isFinished = false

    @VisibleForTesting
    var displayedText: String = ""
        private set

    init {
        layout = OverlayLayout(this)
        border = BorderFactory.createEmptyBorder()
        add(previousLayer)
        add(currentLayer)
    }

    fun setExpanded(expanded: Boolean) {
        val wasExpanded = isExpanded
        isExpanded = expanded

        // If expansion state changed and we're finished, rebuild layout
        if (wasExpanded != expanded && isFinished) {
            if (expanded) {
                // Build timeline layout
                buildTimelineLayout(displayedText)
            } else {
                // Switch back to minimal view
                timelineContainer?.let { remove(it) }
                timelineContainer = null
                currentLayer.isVisible = true
                previousLayer.isVisible = true
                revalidate()
                repaint()
            }
        }
    }

    fun update(reasoning: Message.Reasoning) {
        isFinished
        isFinished = !reasoning.isInProgress

        // If finished and expanded, show timeline layout
        if (isFinished && isExpanded) {
            buildTimelineLayout(reasoning.markdown)
            return
        }

        // Otherwise, use the existing fade logic for in-progress or collapsed view
        val text = if (reasoning.isInProgress) {
            extractLastFullParagraph(reasoning.markdown)
        } else {
            reasoning.markdown
        }

        // Hide timeline if switching back to fade view
        if (timelineContainer != null) {
            remove(timelineContainer)
            timelineContainer = null
            // Show the fade layers again
            currentLayer.isVisible = true
            previousLayer.isVisible = true
        }

        if (displayedText == text) {
            return
        }

        if (displayedText.isEmpty()) {
            currentPane.text = text
            displayedText = text
            updateHeight()
            updateTextColor(reasoning.isInProgress)
            return
        }

        // Debounce: skip animation if last update was < 150ms ago
        val now = System.currentTimeMillis()
        val shouldSkipAnimation = (now - lastFadeStartMs) < 150

        previousPane.text = currentPane.text
        previousLayerUI.alpha = 1f

        currentPane.text = text
        currentLayerUI.alpha = if (reasoning.isInProgress) 0f else 1f
        displayedText = text
        updateHeight()
        updateTextColor(reasoning.isInProgress)

        if (shouldSkipAnimation) {
            // Snap to new content without animation
            previousLayerUI.alpha = 0f
            currentLayerUI.alpha = 1f
            previousPane.text = ""
        } else {
            lastFadeStartMs = now
            startFade()
        }
    }

    private fun updateTextColor(isInProgress: Boolean) {
        val color = if (isInProgress) {
            UIUtil.getLabelDisabledForeground()
        } else {
            UIUtil.getLabelForeground()
        }
        currentPane.foreground = color
        previousPane.foreground = color
    }

    private fun startFade() {
        fadeTimer?.stop()
        // Target duration: 140-160ms, timer granularity: 16ms, alpha step: ~0.1
        fadeTimer = Timer(16) {
            val step = 0.1f
            previousLayerUI.alpha = (previousLayerUI.alpha - step).coerceAtLeast(0f)
            currentLayerUI.alpha = (currentLayerUI.alpha + step).coerceAtMost(1f)
            repaint()
            if (previousLayerUI.alpha <= 0f && currentLayerUI.alpha >= 1f) {
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

    private fun buildTimelineLayout(markdown: String) {
        // Hide the fade layers
        currentLayer.isVisible = false
        previousLayer.isVisible = false

        // Remove existing timeline if any
        timelineContainer?.let { remove(it) }

        // Split into paragraphs
        val paragraphs = markdown.split("\n\n").filter { it.trim().isNotEmpty() }

        if (paragraphs.isEmpty()) return

        // Create timeline container
        val container = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = UIUtil.getPanelBackground()
            border = BorderFactory.createEmptyBorder()
        }

        paragraphs.forEachIndexed { index, paragraph ->
            val rowPanel = JPanel(BorderLayout()).apply {
                background = UIUtil.getPanelBackground()
                border = BorderFactory.createEmptyBorder(0, 0, JBUI.scale(8), 0)
            }

            // Create timeline indicator
            val indicator = TimelineIndicator(
                showTopLine = index > 0,
                showBottomLine = true
            )

            val textPane = EditorPaneHelper.createMarkdownPane(paragraph, background).apply {
                border = BorderFactory.createEmptyBorder(0, JBUI.scale(8), 0, JBUI.scale(8))
            }

            rowPanel.add(indicator, BorderLayout.WEST)
            rowPanel.add(textPane, BorderLayout.CENTER)
            container.add(rowPanel)
        }

        // Wrap the timeline in a scroll pane to ensure all content is accessible
        val scrollPane = JBScrollPane(container).apply {
            border = null
            background = UIUtil.getPanelBackground()
            viewport.background = UIUtil.getPanelBackground()
            horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        }

        timelineContainer = scrollPane
        add(scrollPane)
        displayedText = markdown

        revalidate()

        ApplicationManager.getApplication().invokeLater {
            scrollPane.verticalScrollBar.value = 0
            scrollPane.horizontalScrollBar.value = 0
            repaint()
        }
    }

    private fun extractLastFullParagraph(markdown: String): String {
        val paragraphs = markdown.split("\n\n")
        return paragraphs.dropLast(1).lastOrNull()?.trim() ?: ""
    }

    override fun dispose() {
        fadeTimer?.stop()
        // Nothing to clean up
    }
}
