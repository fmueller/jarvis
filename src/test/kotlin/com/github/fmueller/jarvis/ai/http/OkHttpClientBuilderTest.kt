package com.github.fmueller.jarvis.ai.http

import dev.langchain4j.http.client.HttpClient
import junit.framework.TestCase
import java.time.Duration

class OkHttpClientBuilderTest : TestCase() {

    private lateinit var cancellableHttpClient: CancellableHttpClient
    private lateinit var builder: OkHttpClientBuilder

    override fun setUp() {
        super.setUp()
        cancellableHttpClient = CancellableHttpClient()
        builder = OkHttpClientBuilder(cancellableHttpClient)
    }

    fun `test default timeouts`() {
        assertEquals(Duration.ofSeconds(30), builder.connectTimeout())
        assertEquals(Duration.ofSeconds(5), builder.readTimeout())
    }

    fun `test setting connect timeout`() {
        val timeout = Duration.ofSeconds(60)
        val result = builder.connectTimeout(timeout)

        assertSame(builder, result)

        assertEquals(timeout, builder.connectTimeout())
    }

    fun `test setting read timeout`() {
        val timeout = Duration.ofSeconds(10)
        val result = builder.readTimeout(timeout)

        assertSame(builder, result)

        assertEquals(timeout, builder.readTimeout())
    }

    fun `test setting null timeouts has no effect`() {
        val originalConnectTimeout = builder.connectTimeout()
        val originalReadTimeout = builder.readTimeout()

        builder.connectTimeout(null)
        builder.readTimeout(null)

        assertEquals(originalConnectTimeout, builder.connectTimeout())
        assertEquals(originalReadTimeout, builder.readTimeout())
    }

    fun `test build returns OkHttpClientAdapter`() {
        val client = builder.build()

        assertNotNull("Client should not be null", client)
        assertTrue("Client should be instance of HttpClient", client is HttpClient)
        assertTrue("Client should be instance of OkHttpClientAdapter", client is OkHttpClientAdapter)
    }

    fun `test build with custom timeouts`() {
        val connectTimeout = Duration.ofSeconds(45)
        val readTimeout = Duration.ofSeconds(15)

        builder.connectTimeout(connectTimeout)
        builder.readTimeout(readTimeout)

        val client = builder.build()

        assertNotNull("Client should not be null", client)
        assertTrue("Client should be instance of OkHttpClientAdapter", client is OkHttpClientAdapter)
        
        // Note: We can't directly test the internal OkHttpClient's timeouts
        // as they're private to the OkHttpClientAdapter, but we've verified
        // that the builder correctly stores the timeout values
    }
}