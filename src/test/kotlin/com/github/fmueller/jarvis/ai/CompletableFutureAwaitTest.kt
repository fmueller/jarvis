package com.github.fmueller.jarvis.ai

import junit.framework.TestCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture

class CompletableFutureAwaitTest : TestCase() {
    fun `test awaitCancellable cancels future`() = runBlocking {
        val future = CompletableFuture<String>()
        val job: Job = launch {
            try {
                future.awaitCancellable()
            } catch (_: Exception) {
                // ignore
            }
        }
        delay(100)
        job.cancelAndJoin()
        assertTrue(future.isCancelled)
    }
}
