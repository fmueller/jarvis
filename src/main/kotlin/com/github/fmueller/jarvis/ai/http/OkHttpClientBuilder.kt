package com.github.fmueller.jarvis.ai.http

import dev.langchain4j.http.client.HttpClient
import dev.langchain4j.http.client.HttpClientBuilder
import okhttp3.OkHttpClient
import java.time.Duration
import java.util.concurrent.TimeUnit

class OkHttpClientBuilder(private val cancellableHttpClient: CancellableHttpClient) : HttpClientBuilder {

    private var connectTimeout: Duration = Duration.ofSeconds(30)
    private var readTimeout: Duration = Duration.ofSeconds(5)

    override fun connectTimeout(): Duration = connectTimeout

    override fun connectTimeout(timeout: Duration?): HttpClientBuilder {
        timeout?.let { this.connectTimeout = it }
        return this
    }

    override fun readTimeout(): Duration = readTimeout

    override fun readTimeout(timeout: Duration?): HttpClientBuilder {
        timeout?.let { this.readTimeout = it }
        return this
    }

    override fun build(): HttpClient? {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(connectTimeout.toMillis(), TimeUnit.MILLISECONDS)
            .readTimeout(readTimeout.toMillis(), TimeUnit.MILLISECONDS)
            .writeTimeout(readTimeout.toMillis(), TimeUnit.MILLISECONDS)
            .build()

        return OkHttpClientAdapter(okHttpClient, cancellableHttpClient)
    }
}
