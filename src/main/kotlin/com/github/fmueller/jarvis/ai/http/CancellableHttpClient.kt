package com.github.fmueller.jarvis.ai.http

import dev.langchain4j.http.client.HttpClientBuilder
import okhttp3.Call

class CancellableHttpClient {

    @Volatile
    private var currentCall: Call? = null

    fun getHttpClientBuilder(): HttpClientBuilder {
        return OkHttpClientBuilder(this)
    }

    fun setCurrentCall(call: Call) {
        currentCall?.cancel()
        currentCall = call
    }

    fun cancel() {
        currentCall?.cancel()
        currentCall = null
    }
}
