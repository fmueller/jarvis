package com.github.fmueller.jarvis.ai

import dev.langchain4j.data.message.*
import dev.langchain4j.model.TokenCountEstimator
import kotlin.math.roundToInt

/**
 * A simple tokenizer that estimates the number of tokens in a text based on its length.
 */
class SimpleTokenizer : TokenCountEstimator {

    override fun estimateTokenCountInText(text: String?): Int {
        return ((text?.length ?: 0) * 0.3f).roundToInt()
    }

    override fun estimateTokenCountInMessage(message: ChatMessage?): Int {
        return estimateTokenCountInText(getText(message)) + 2
    }

    @SuppressWarnings("deprecation")
    private fun getText(message: ChatMessage?): String {
        return when (message) {
            is AiMessage -> message.text()
            is SystemMessage -> message.text()
            is UserMessage -> if (message.hasSingleText()) message.singleText() else ""
            is ToolExecutionResultMessage -> message.text()
            else -> ""
        }
    }

    override fun estimateTokenCountInMessages(messages: Iterable<ChatMessage?>?): Int {
        return messages?.sumOf { estimateTokenCountInMessage(it) } ?: 0
    }
}