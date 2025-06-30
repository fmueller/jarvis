package com.github.fmueller.jarvis.ai.http

import junit.framework.TestCase
import okhttp3.Call

class CancellableHttpClientTest : TestCase() {

    fun `test getHttpClientBuilder returns OkHttpClientBuilder`() {
        val client = CancellableHttpClient()
        val builder = client.getHttpClientBuilder()

        assertTrue("Builder should be instance of OkHttpClientBuilder", builder is OkHttpClientBuilder)
        assertNotNull("Builder should not be null", builder)
    }

    fun `test setCurrentCall cancels previous call`() {
        val client = CancellableHttpClient()

        val firstCall = createMockCall()
        val secondCall = createMockCall()

        client.setCurrentCall(firstCall)
        client.setCurrentCall(secondCall)

        assertTrue("First call should be canceled", firstCall.isCanceled())
    }

    fun `test cancel cancels current call and sets it to null`() {
        val client = CancellableHttpClient()
        val call = createMockCall()

        client.setCurrentCall(call)
        client.cancel()
        assertTrue("Call should be canceled", call.isCanceled())
        
        val newCall = createMockCall()
        client.setCurrentCall(newCall)
        assertFalse("New call should not be canceled yet", newCall.isCanceled())
    }
    
    private fun createMockCall(): Call {
        return object : Call {
            private var canceled = false
            
            override fun cancel() {
                canceled = true
            }
            
            override fun isCanceled(): Boolean = canceled
            
            override fun request() = throw UnsupportedOperationException("Not needed for test")
            override fun execute() = throw UnsupportedOperationException("Not needed for test")
            override fun enqueue(responseCallback: okhttp3.Callback) = throw UnsupportedOperationException("Not needed for test")
            override fun isExecuted(): Boolean = throw UnsupportedOperationException("Not needed for test")
            override fun timeout() = throw UnsupportedOperationException("Not needed for test")
            override fun clone(): Call = throw UnsupportedOperationException("Not needed for test")
        }
    }
}