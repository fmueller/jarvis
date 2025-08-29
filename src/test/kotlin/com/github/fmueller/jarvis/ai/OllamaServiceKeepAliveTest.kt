package com.github.fmueller.jarvis.ai

import dev.langchain4j.model.ollama.OllamaStreamingChatModel
import junit.framework.TestCase
import java.lang.reflect.Proxy

class OllamaServiceKeepAliveTest : TestCase() {

    fun `test keep alive is five minutes`() {
        val assistantField = OllamaService::class.java.getDeclaredField("assistant")
        assistantField.isAccessible = true
        val assistant = assistantField.get(OllamaService)
        val handler = Proxy.getInvocationHandler(assistant)
        val outerField = handler.javaClass.getDeclaredField("this$0")
        outerField.isAccessible = true
        val defaultAiServices = outerField.get(handler)
        val contextField = defaultAiServices.javaClass.superclass.getDeclaredField("context")
        contextField.isAccessible = true
        val context = contextField.get(defaultAiServices)
        val streamingChatModelField = context.javaClass.getDeclaredField("streamingChatModel")
        streamingChatModelField.isAccessible = true
        val streamingChatModel = streamingChatModelField.get(context) as OllamaStreamingChatModel
        val keepAlive = streamingChatModel.defaultRequestParameters().keepAlive()
        assertEquals(300, keepAlive)
    }
}
