package com.github.fmueller.jarvis.ai.http

import dev.langchain4j.http.client.HttpClient
import dev.langchain4j.http.client.HttpRequest
import dev.langchain4j.http.client.SuccessfulHttpResponse
import dev.langchain4j.http.client.sse.ServerSentEventListener
import dev.langchain4j.http.client.sse.ServerSentEventParser
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.firstOrNull

class OkHttpClientAdapter(
    private val okHttpClient: OkHttpClient,
    private val cancellableHttpClient: CancellableHttpClient
) : HttpClient {

    override fun execute(request: HttpRequest): SuccessfulHttpResponse {
        val call = okHttpClient.newCall(convertToOkHttpRequest(request))
        cancellableHttpClient.setCurrentCall(call)

        return try {
            val response = call.execute()
            convertToLangChain4jResponse(response)
        } catch (e: IOException) {
            throw RuntimeException("HTTP request failed", e)
        }
    }

    override fun execute(
        request: HttpRequest?,
        parser: ServerSentEventParser?,
        listener: ServerSentEventListener?
    ) {
        if (request == null || parser == null || listener == null) {
            throw IllegalArgumentException("Request, parser, and listener cannot be null")
        }

        val call = okHttpClient.newCall(convertToOkHttpRequest(request))
        cancellableHttpClient.setCurrentCall(call)

        try {
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    listener.onError(RuntimeException("SSE request failed", e))
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        listener.onError(RuntimeException("SSE request failed with status: ${response.code}"))
                        return
                    }

                    response.body?.let { responseBody ->
                        try {
                            responseBody.byteStream().use { inputStream ->
                                // The parser will read from the InputStream and call the listener methods directly
                                parser.parse(inputStream, listener)
                            }
                        } catch (e: Exception) {
                            if (!call.isCanceled()) {
                                listener.onError(RuntimeException("Error reading SSE stream", e))
                            }
                        }
                    } ?: run {
                        listener.onError(RuntimeException("No response body received"))
                    }
                }
            })
        } catch (e: Exception) {
            listener.onError(RuntimeException("Failed to execute SSE request", e))
        }
    }

    private fun convertToOkHttpRequest(request: HttpRequest): Request {
        val builder = Request.Builder().url(request.url())
        request.headers().forEach { (name, values) ->
            values.forEach { value ->
                builder.addHeader(name, value)
            }
        }

        when (request.method()) {
            dev.langchain4j.http.client.HttpMethod.GET -> builder.get()
            dev.langchain4j.http.client.HttpMethod.POST -> {
                val body = request.body()?.let { bodyContent ->
                    val contentType = request.headers()["Content-Type"]?.firstOrNull()
                        ?: "application/json"
                    bodyContent.toRequestBody(contentType.toMediaType())
                } ?: "".toRequestBody()
                builder.post(body)
            }
            dev.langchain4j.http.client.HttpMethod.DELETE -> builder.delete()
            else -> throw IllegalArgumentException("Unsupported HTTP method: ${request.method()}")
        }

        return builder.build()
    }

    private fun convertToLangChain4jResponse(response: Response): SuccessfulHttpResponse {
        val body = response.body?.string() ?: ""
        val headers = mutableMapOf<String, List<String>>()
        response.headers.forEach { (name, value) ->
            headers[name] = headers.getOrDefault(name, emptyList()) + value
        }

        return SuccessfulHttpResponse.builder()
            .statusCode(response.code)
            .headers(headers)
            .body(body)
            .build()
    }
}
