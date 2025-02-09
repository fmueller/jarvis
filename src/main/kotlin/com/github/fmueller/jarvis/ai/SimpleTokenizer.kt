package com.github.fmueller.jarvis.ai

import dev.langchain4j.agent.tool.ToolExecutionRequest
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.Tokenizer
import kotlin.math.roundToInt

/**
 * A simple tokenizer that estimates the number of tokens in a text based on its length.
 */
class SimpleTokenizer : Tokenizer {

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
            else -> ""
        }
    }

    override fun estimateTokenCountInMessages(messages: Iterable<ChatMessage?>?): Int {
        return messages?.sumOf { estimateTokenCountInMessage(it) } ?: 0
    }

    override fun estimateTokenCountInToolSpecifications(toolSpecifications: Iterable<ToolSpecification?>?): Int {
        return toolSpecifications?.sumOf {
            10 + estimateTokenCountInText(it?.description() ?: "") + estimateTokenCountInText(it?.name() ?: "")
        } ?: 0
    }

    override fun estimateTokenCountInToolExecutionRequests(toolExecutionRequests: Iterable<ToolExecutionRequest?>?): Int {
        return toolExecutionRequests?.sumOf {
            5 + estimateTokenCountInText(it?.id() ?: "") + estimateTokenCountInText(
                it?.name() ?: ""
            ) + estimateTokenCountInText(it?.arguments() ?: "")
        } ?: 0
    }
}