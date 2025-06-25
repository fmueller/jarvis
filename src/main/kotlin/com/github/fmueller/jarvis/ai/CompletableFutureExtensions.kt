package com.github.fmueller.jarvis.ai

import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal suspend fun <T> CompletableFuture<T>.awaitCancellable(): T = suspendCancellableCoroutine { cont ->
    whenComplete { result, error ->
        if (error == null) {
            cont.resume(result)
        } else {
            cont.resumeWithException(error)
        }
    }
    cont.invokeOnCancellation { this.cancel(true) }
}
