package com.github.fmueller.jarvis.ai.http

import dev.langchain4j.http.client.HttpMethod
import dev.langchain4j.http.client.HttpRequest
import dev.langchain4j.http.client.SuccessfulHttpResponse
import dev.langchain4j.http.client.sse.ServerSentEvent
import dev.langchain4j.http.client.sse.ServerSentEventListener
import dev.langchain4j.http.client.sse.ServerSentEventParser
import junit.framework.TestCase
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException
import java.io.InputStream
import kotlin.reflect.KClass

class OkHttpClientAdapterTest : TestCase() {

    private lateinit var mockOkHttpClient: MockOkHttpClient
    private lateinit var cancellableHttpClient: CancellableHttpClient
    private lateinit var adapter: OkHttpClientAdapter

    override fun setUp() {
        super.setUp()

        mockOkHttpClient = MockOkHttpClient()
        cancellableHttpClient = CancellableHttpClient()
        adapter = OkHttpClientAdapter(mockOkHttpClient, cancellableHttpClient)
    }

    fun `test execute GET request`() {
        val responseBody = """{"message": "success"}"""
        mockOkHttpClient.nextResponse = createMockResponse(200, responseBody)

        val request = HttpRequest.builder()
            .url("https://example.com/api")
            .method(HttpMethod.GET)
            .headers(mapOf("Accept" to listOf("application/json")))
            .build()

        val response = adapter.execute(request)

        val capturedRequest = mockOkHttpClient.lastRequest
        assertNotNull("Request should not be null", capturedRequest)
        assertEquals("https://example.com/api", capturedRequest?.url.toString())
        assertEquals("GET", capturedRequest?.method)
        assertEquals("application/json", capturedRequest?.header("Accept"))

        assertEquals(200, response.statusCode())
        assertEquals(responseBody, response.body())
        assertEquals("application/json", response.headers()["Content-Type"]?.firstOrNull())
    }

    fun `test execute POST request with body`() {
        val responseBody = """{"id": 123}"""
        mockOkHttpClient.nextResponse = createMockResponse(201, responseBody)

        val requestBody = """{"name": "test"}"""
        val request = HttpRequest.builder()
            .url("https://example.com/api/create")
            .method(HttpMethod.POST)
            .headers(mapOf("Content-Type" to listOf("application/json")))
            .body(requestBody)
            .build()

        val response = adapter.execute(request)

        val capturedRequest = mockOkHttpClient.lastRequest
        assertNotNull("Request should not be null", capturedRequest)
        assertEquals("https://example.com/api/create", capturedRequest?.url.toString())
        assertEquals("POST", capturedRequest?.method)
        assertEquals("application/json", capturedRequest?.header("Content-Type"))

        assertEquals(201, response.statusCode())
        assertEquals(responseBody, response.body())
    }

    fun `test execute handles IOException`() {
        mockOkHttpClient.shouldThrowIOException = true

        val request = HttpRequest.builder()
            .url("https://example.com/api")
            .method(HttpMethod.GET)
            .build()

        try {
            adapter.execute(request)
            fail("Should have thrown RuntimeException")
        } catch (e: RuntimeException) {
            assertEquals("HTTP request failed", e.message)
            assertTrue("Cause should be IOException", e.cause is IOException)
        }
    }

    fun `test execute SSE request`() {
        val sseContent = "data: event1\n\ndata: event2\n\n"
        mockOkHttpClient.nextSseResponse = createMockResponse(200, sseContent)

        val request = HttpRequest.builder()
            .url("https://example.com/api/events")
            .method(HttpMethod.GET)
            .headers(mapOf("Accept" to listOf("text/event-stream")))
            .build()

        val mockParser = MockServerSentEventParser()
        val mockListener = MockServerSentEventListener()

        adapter.execute(request, mockParser, mockListener)

        assertNotNull("SSE request should not be null", mockOkHttpClient.lastSseRequest)
        assertEquals("https://example.com/api/events", mockOkHttpClient.lastSseRequest?.url.toString())
        assertEquals("GET", mockOkHttpClient.lastSseRequest?.method)
        assertEquals("text/event-stream", mockOkHttpClient.lastSseRequest?.header("Accept"))

        assertTrue("Parser should have been called", mockParser.wasCalled)
        assertNotNull("Parser input stream should not be null", mockParser.lastInputStream)

        assertNotNull("Current call should be set", mockOkHttpClient.lastCall)
    }

    fun `test execute SSE handles error response`() {
        mockOkHttpClient.nextSseResponse = createMockResponse(500, "Server Error")

        val request = HttpRequest.builder()
            .url("https://example.com/api/events")
            .method(HttpMethod.GET)
            .build()

        val mockParser = MockServerSentEventParser()
        val mockListener = MockServerSentEventListener()

        adapter.execute(request, mockParser, mockListener)

        assertTrue("Listener should have received error", mockListener.receivedError)

        println("[DEBUG_LOG] Actual error message: ${mockListener.lastError?.message}")

        assertEquals("SSE request failed", mockListener.lastError?.message)
    }

    fun `test execute SSE with null parameters throws exception`() {
        try {
            adapter.execute(null, null, null)
            fail("Should have thrown IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Request, parser, and listener cannot be null", e.message)
        }
    }

    private fun createMockResponse(statusCode: Int, body: String): Response {
        return Response.Builder()
            .request(Request.Builder().url("https://example.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(statusCode)
            .message(if (statusCode >= 400) "Error" else "OK")
            .header("Content-Type", "application/json")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
    }

    private class MockOkHttpClient : OkHttpClient() {
        var nextResponse: Response? = null
        var nextSseResponse: Response? = null
        var lastRequest: Request? = null
        var lastSseRequest: Request? = null
        var lastCall: Call? = null
        var shouldThrowIOException = false

        override fun newCall(request: Request): Call {
            return if (request.header("Accept") == "text/event-stream") {
                lastSseRequest = request
                MockCall(request, nextSseResponse)
            } else {
                lastRequest = request
                MockCall(request, nextResponse)
            }
        }

        inner class MockCall(
            private val request: Request,
            private val response: Response?
        ) : Call {
            private var canceled = false
            private var executed = false
            private val tags = mutableMapOf<Any, Any>()

            init {
                lastCall = this
            }

            override fun execute(): Response {
                executed = true
                if (shouldThrowIOException) {
                    throw IOException("Simulated network error")
                }
                return response ?: throw IOException("No mock response provided")
            }

            override fun enqueue(callback: Callback) {
                if (canceled) {
                    return
                }

                executed = true

                if (shouldThrowIOException) {
                    callback.onFailure(this, IOException("Simulated network error"))
                    return
                }

                val resp = response ?: run {
                    callback.onFailure(this, IOException("No mock response provided"))
                    return
                }

                callback.onResponse(this, resp)
            }

            override fun cancel() {
                canceled = true
            }

            override fun isCanceled(): Boolean = canceled
            override fun isExecuted(): Boolean = executed
            override fun request(): Request = request
            override fun timeout() = throw UnsupportedOperationException("Not needed for test")
            override fun clone(): Call = MockCall(request, response)

            override fun <T : Any> tag(type: KClass<T>): T? = tags[type] as? T

            override fun <T> tag(type: Class<out T>): T? = tags[type] as? T

            override fun <T : Any> tag(type: KClass<T>, computeIfAbsent: () -> T): T {
                @Suppress("UNCHECKED_CAST")
                return tags.getOrPut(type) { computeIfAbsent() } as T
            }

            override fun <T : Any> tag(type: Class<T>, computeIfAbsent: () -> T): T {
                @Suppress("UNCHECKED_CAST")
                return tags.getOrPut(type) { computeIfAbsent() } as T
            }
        }
    }

    private class MockServerSentEventParser : ServerSentEventParser {
        var wasCalled = false
        var lastInputStream: InputStream? = null

        override fun parse(inputStream: InputStream, listener: ServerSentEventListener) {
            wasCalled = true
            lastInputStream = inputStream

            // Simulate parsing events from the input stream
        }
    }

    private class MockServerSentEventListener : ServerSentEventListener {
        var receivedError = false
        var lastError: Throwable? = null
        var openCalled = false
        var eventReceived = false

        override fun onOpen(response: SuccessfulHttpResponse) {
            openCalled = true
        }

        override fun onEvent(event: ServerSentEvent) {
            eventReceived = true
        }

        override fun onError(throwable: Throwable) {
            receivedError = true
            lastError = throwable
        }
    }
}
