package com.github.fmueller.jarvis.conversation

import com.github.fmueller.jarvis.ui.SyntaxHighlightedCodeHelper
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.HTMLEditorKitBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import org.jdesktop.swingx.VerticalLayout
import org.jetbrains.annotations.VisibleForTesting
import java.awt.AlphaComposite
import java.awt.BorderLayout
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.util.regex.Pattern
import javax.swing.*
import kotlin.math.ceil

class MessagePanel(
    initialMessage: Message,
    private val project: Project,
    private val isReasoningPanel: Boolean = false,
    private val isTestMode: Boolean = false
) : JPanel(), Disposable {

    companion object {

        private const val UPDATE_DELAY_MS = 100
        private const val MIN_CONTENT_CHANGE = 5
        private const val MIN_UPDATE_INTERVAL_MS = 50
        private const val FADE_IN_DELAY_MS = 30
        private const val FADE_IN_STEP = 0.1f
        private const val TYPING_MS_PER_CHAR = UPDATE_DELAY_MS / MIN_CONTENT_CHANGE

        private val codeBlockPattern = Pattern.compile("`{2,}(\\w+)?\\n(.*?)\\n\\s*`{2,}", Pattern.DOTALL)

        private val assistantBgColor = { UIUtil.getPanelBackground() }
        private val userBgColor = { UIUtil.getTextFieldBackground() }

        fun create(
            initialMessage: Message,
            project: Project,
            isReasoningPanel: Boolean = false
        ): MessagePanel {
            return MessagePanel(initialMessage, project, isReasoningPanel, false)
        }

        fun createForTesting(
            initialMessage: Message,
            project: Project,
            isReasoningPanel: Boolean = false
        ): MessagePanel {
            return MessagePanel(initialMessage, project, isReasoningPanel, true)
        }
    }

    private val updateTimer = Timer(UPDATE_DELAY_MS) {
        performScheduledUpdate()
    }.apply { isRepeats = false }

    private var pendingMessage: Message? = null
    private var lastUpdateTime = 0L
    private var lastRenderedContentLength = 0
    @VisibleForTesting
    internal var currentAlpha = 1f
    private var fadeTimer: Timer? = null

    sealed interface ParsedContent
    data class Content(val markdown: String) : ParsedContent
    data class Code(val languageId: String, val content: String) : ParsedContent

    private val highlightedCodeHelper = SyntaxHighlightedCodeHelper(project)

    @VisibleForTesting
    val parsed = mutableListOf<ParsedContent>()

    private var reasoningPanel: JPanel? = null
    private var reasoningHeaderButton: JButton? = null
    private var reasoningContentPanel: JPanel? = null
    private var hasReasoningContent = false

    @VisibleForTesting
    var reasoningMessagePanel: MessagePanel? = null

    private var isReasoningExpanded: Boolean = false
        set(value) {
            field = value
            reasoningContentPanel?.isVisible = value
            reasoningHeaderButton?.icon = if (value) {
                com.intellij.icons.AllIcons.General.ArrowDown
            } else {
                com.intellij.icons.AllIcons.General.ArrowRight
            }
        }

    private var _message: Message = initialMessage
    var message: Message
        get() = _message
        set(value) {
            _message = value
            if (isTestMode) {
                updatePanel(value)
            } else {
                scheduleSmartUpdate(value)
            }
        }

    init {
        resetUI()
        updatePanel(message)
    }

    override fun updateUI() {
        super.updateUI()
        resetUI()
        updatePanel(message)
    }

    override fun paint(g: Graphics) {
        val g2 = g as Graphics2D
        val original = g2.composite
        if (currentAlpha < 1f) {
            g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, currentAlpha)
        }
        super.paint(g2)
        g2.composite = original
    }

    override fun dispose() {
        updateTimer.stop()
        fadeTimer?.stop()
        parsed.clear()
        highlightedCodeHelper.disposeAllEditors()
        reasoningMessagePanel?.dispose()
        hasReasoningContent = false
    }

    private fun scheduleSmartUpdate(newMessage: Message) {
        val now = System.currentTimeMillis()
        val timeSinceLastUpdate = now - lastUpdateTime
        val contentLengthDiff = newMessage.content.length - lastRenderedContentLength

        val shouldUpdateImmediately = contentLengthDiff >= MIN_CONTENT_CHANGE * 3 || timeSinceLastUpdate > UPDATE_DELAY_MS * 2
        if (shouldUpdateImmediately && timeSinceLastUpdate > MIN_UPDATE_INTERVAL_MS) {
            updateTimer.stop()
            performActualUpdate(newMessage)
        } else {
            // Schedule delayed update
            pendingMessage = newMessage
            if (!updateTimer.isRunning) {
                updateTimer.start()
            }
        }
    }

    private fun performScheduledUpdate() {
        pendingMessage?.let { msg ->
            performActualUpdate(msg)
            pendingMessage = null
        }
    }

    private fun performActualUpdate(messageToRender: Message) {
        lastUpdateTime = System.currentTimeMillis()
        lastRenderedContentLength = messageToRender.content.length
        SwingUtilities.invokeLater {
            updatePanel(messageToRender)
        }
    }

    /**
     * Displays the provided message immediately and fades it in if fading is faster than the normal typing animation.
     */
    internal fun fadeInFinalMessage(finalMessage: Message? = null) {
        updateTimer.stop()
        pendingMessage = null

        finalMessage?.let {
            _message = it
            updatePanel(it)
        }

        val targetMessage = finalMessage ?: _message
        val typingDurationMs = targetMessage.content.length * TYPING_MS_PER_CHAR
        val fadeSteps = ceil(1f / FADE_IN_STEP).toInt()
        val fadeDurationMs = fadeSteps * FADE_IN_DELAY_MS

        if (fadeDurationMs < typingDurationMs) {
            currentAlpha = 0f
            fadeTimer?.stop()
            fadeTimer = Timer(FADE_IN_DELAY_MS) {
                currentAlpha = (currentAlpha + FADE_IN_STEP).coerceAtMost(1f)
                repaint()
                if (currentAlpha >= 1f) {
                    fadeTimer?.stop()
                }
            }
            fadeTimer?.start()
        } else {
            currentAlpha = 1f
            fadeTimer?.stop()
            repaint()
        }
    }

    @Suppress("SENSELESS_COMPARISON") // message can be null
    private fun updatePanel(messageToUpdate: Message) {
        if (messageToUpdate == null) {
            return
        }

        val (reasoning, contentList) = parse(messageToUpdate)

        if (!isReasoningPanel) {
            if (reasoning != null) {
                hasReasoningContent = true
                reasoningPanel?.isVisible = true
                if (reasoningMessagePanel == null) {
                    reasoningMessagePanel = MessagePanel(Message.fromAssistant(reasoning.markdown), project, true, isTestMode)
                } else {
                    reasoningMessagePanel?.message = Message.fromAssistant(reasoning.markdown)
                }
                reasoningHeaderButton?.text = "Reasoning${if (reasoning.isInProgress) "..." else ""}"
                isReasoningExpanded = reasoning.isInProgress
            } else if (!hasReasoningContent) {
                reasoningPanel?.isVisible = false
            }
        }

        val newParsedContent = contentList
        synchronized(treeLock) {
            for (i in newParsedContent.indices) {
                if (i >= parsed.size) {
                    val newContent = newParsedContent.subList(i, newParsedContent.size)
                    parsed.addAll(newContent)
                    render(newContent)
                    break
                }

                val old = parsed[i]
                val new = newParsedContent[i]

                if (i == newParsedContent.lastIndex && isUpdatableParsedContent(old, new)) {
                    when (old) {
                        is Content -> {
                            val component = getComponent(componentCount - 1)
                            val editorPane = when (component) {
                                is JEditorPane -> component
                                is JBScrollPane if component.viewport.view is JEditorPane -> component.viewport.view as JEditorPane
                                else -> throw IllegalStateException(
                                    "Expected a JEditorPane (or a JBScrollPane wrapping one) but got ${component.javaClass.name}"
                                )
                            }
                            editorPane.text = markdownToHtml((new as Content).markdown)
                        }

                        is Code -> {
                            val lastComponent = getComponent(componentCount - 1)
                            if (lastComponent is JBScrollPane && lastComponent.viewport.view is Editor) {
                                val editor = lastComponent.viewport.view as Editor
                                highlightedCodeHelper.disposeEditor(editor)
                            }
                            remove(componentCount - 1)
                            addHighlightedCode((new as Code).languageId, new.content)
                        }

                    }
                    break
                }

                if (isDifferentParsedContent(old, new)) {
                    val newContent = newParsedContent.subList(i, newParsedContent.size)
                    parsed.subList(i, parsed.size).clear()
                    parsed.addAll(newContent)
                    removeAllComponentsAfter(i)
                    render(newContent)
                    break
                }
            }
        }
    }

    private fun isUpdatableParsedContent(old: ParsedContent, new: ParsedContent): Boolean {
        if (old::class != new::class) {
            return false
        }

        return when (old) {
            is Content -> (new as Content).markdown.startsWith(old.markdown)
            is Code -> (new as Code).content.startsWith(old.content) && new.languageId == old.languageId
        }
    }

    private fun isDifferentParsedContent(old: ParsedContent, new: ParsedContent): Boolean {
        if (old::class != new::class) {
            return true
        }

        return when (old) {
            is Content -> (new as Content).markdown != old.markdown
            is Code -> (new as Code).content != old.content || new.languageId != old.languageId
        }
    }

    @Suppress("SENSELESS_COMPARISON") // message can be null
    private fun resetUI() {
        if (message == null) {
            return
        }

        dispose()

        layout = VerticalLayout(5)
        background = when (message.role) {
            Role.ASSISTANT -> assistantBgColor()
            Role.USER -> userBgColor()
            Role.INFO -> assistantBgColor()
        }
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, JBUI.CurrentTheme.ToolWindow.borderColor()),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        )

        if (!isReasoningPanel) {
            add(
                JBLabel(
                    when (message.role) {
                        Role.ASSISTANT -> "Jarvis"
                        Role.USER -> "You"
                        Role.INFO -> "Info"
                    }
                ).apply {
                    font = font.deriveFont(Font.BOLD)
                    border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
                }
            )

            val outerPanel = JPanel().apply { layout = BorderLayout() }
            val contentPanel = JPanel().apply { layout = BorderLayout() }
            reasoningPanel = outerPanel
            reasoningContentPanel = contentPanel

            val headerButton = JButton("Reasoning").apply {
                isBorderPainted = false
                isContentAreaFilled = false
                isFocusPainted = false
                isOpaque = false

                horizontalAlignment = SwingConstants.LEFT
                border = BorderFactory.createEmptyBorder(4, 8, 4, 8)
                font = font.deriveFont(Font.BOLD, font.size.toFloat())

                icon = if (contentPanel.isVisible) {
                    com.intellij.icons.AllIcons.General.ArrowDown
                } else {
                    com.intellij.icons.AllIcons.General.ArrowRight
                }

                addActionListener {
                    isReasoningExpanded = !isReasoningExpanded
                }
            }
            reasoningHeaderButton = headerButton

            outerPanel.add(headerButton, BorderLayout.NORTH)
            outerPanel.add(contentPanel, BorderLayout.CENTER)

            reasoningMessagePanel = MessagePanel(Message.fromAssistant(""), project, true, isTestMode).apply {
                background = outerPanel.background
                border = BorderFactory.createEmptyBorder(0, 15, 0, 10)
            }
            contentPanel.add(reasoningMessagePanel!!, BorderLayout.CENTER)
            contentPanel.isVisible = false
            isReasoningExpanded = false

            add(outerPanel)
            outerPanel.isVisible = false
        }
    }

    private fun parse(message: Message): Pair<Message.Reasoning?, List<ParsedContent>> {
        val parsed = mutableListOf<ParsedContent>()

        val closed = message.contentWithClosedTrailingCodeBlock()
        val (reasoning, remaining) = message.copy(content = closed).parseReasoning()

        val matcher = codeBlockPattern.matcher(remaining)
        var lastEnd = 0
        while (matcher.find()) {
            // Add preceding non-code content
            if (matcher.start() > lastEnd) {
                val nonCodeMarkdown = remaining.substring(lastEnd, matcher.start())
                parsed.add(Content(nonCodeMarkdown))
            }

            // Add syntax-highlighted code
            val languageId = matcher.group(1)?.lowercase()
            val code = matcher.group(2)
            parsed.add(Code(languageId ?: "plaintext", code))

            lastEnd = matcher.end()
        }

        // Add any remaining non-code content
        if (lastEnd < remaining.length) {
            val remainingNonCodeMarkdown = remaining.substring(lastEnd)
            parsed.add(Content(remainingNonCodeMarkdown))
        }

        return Pair(reasoning, parsed)
    }

    private fun render(parsed: List<ParsedContent>) {
        parsed.forEach {
            when (it) {
                is Content -> addNonCodeContent(it.markdown)
                is Code -> addHighlightedCode(it.languageId, it.content)
            }
        }
    }

    private fun addNonCodeContent(markdown: String) {
        val globalScheme = EditorColorsManager.getInstance().globalScheme
        val functionDeclaration = TextAttributesKey.createTextAttributesKey("DEFAULT_FUNCTION_DECLARATION")
        val codeColor =
            globalScheme.getAttributes(functionDeclaration).foregroundColor ?: globalScheme.defaultForeground
        val outerPanelBackground = background
        val editorPane = JEditorPane().apply {
            editorKit = HTMLEditorKitBuilder.simple().apply {
                styleSheet.addRule(
                    """
                        p {
                            margin: 4px 0;
                        }
                        ul, ol {
                            margin-top: 4px;
                            margin-bottom: 8px;
                        }
                        h1, h2, h3, h4, h5, h6 {
                            margin-top: 8px;
                            margin-bottom: 0;
                        }
                        code {
                            background-color: rgb(${outerPanelBackground.red}, ${outerPanelBackground.green}, ${outerPanelBackground.blue});
                            color: rgb(${codeColor.red}, ${codeColor.green}, ${codeColor.blue});
                            font-size: 0.9em;
                        }
                    """.trimIndent()
                )
            }
            text = markdownToHtml(markdown)
            isEditable = false
            background = outerPanelBackground
            border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
        }
        add(editorPane)
    }

    private fun addHighlightedCode(languageId: String, code: String) {
        val editor = highlightedCodeHelper.getHighlightedEditor(languageId, code)
        if (editor != null) {
            editor.contentComponent.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
            val outerPanelBackground = background
            add(JBScrollPane(editor.component).apply {
                viewport.view.background = outerPanelBackground
                border = BorderFactory.createEmptyBorder(10, 5, 10, 5)
            })
        } else {
            addNonCodeContent(code)
        }
    }

    private fun markdownToHtml(text: String): String {
        val options = MutableDataSet()
        options.set(
            Parser.EXTENSIONS,
            listOf(TablesExtension.create(), StrikethroughExtension.create())
        )
        options.set(HtmlRenderer.SOFT_BREAK, "<br />")
        val parser: Parser = Parser.builder(options).build()
        return HtmlRenderer.builder(options).build().render(parser.parse(text))
    }

    private fun removeAllComponentsAfter(index: Int) {
        val fixedComponentsCount = if (isReasoningPanel) 0 else 2 // role label + reasoning panel
        val actualComponentIndex = fixedComponentsCount + index

        for (i in componentCount - 1 downTo actualComponentIndex) {
            val component = getComponent(i)
            // Skip removal of fixed components (reasoning panel and role label)
            if (!isReasoningPanel && (component == reasoningPanel || component is JBLabel)) {
                continue
            }

            remove(i)
        }
    }
}
