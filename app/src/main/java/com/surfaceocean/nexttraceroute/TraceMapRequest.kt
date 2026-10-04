package com.surfaceocean.nexttraceroute

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Keep cancellation attached until the response body has been consumed and closed. */
internal suspend fun Call.awaitTraceMapUrl(): String? = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            try {
                val result = response.use {
                    if (!it.isSuccessful) return@use null
                    val source = it.body.source()
                    // The endpoint returns a URL; a large/unbounded body is not a valid result.
                    source.request(8_193)
                    if (source.buffer.size > 8_192) throw IOException("TraceMap response is too large")
                    source.readUtf8().trim().takeIf { text -> text.isNotBlank() }
                }
                if (continuation.isActive) continuation.resume(result)
            } catch (error: Exception) {
                if (continuation.isActive) continuation.resumeWithException(error)
            }
        }
    })
}
