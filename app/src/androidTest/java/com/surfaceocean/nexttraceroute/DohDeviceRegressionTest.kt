package com.surfaceocean.nexttraceroute

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.xbill.DNS.DClass
import org.xbill.DNS.Message
import org.xbill.DNS.Name
import org.xbill.DNS.Record
import org.xbill.DNS.Type
import java.io.IOException
import java.lang.reflect.Proxy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** Exercise the production DoH response bridge on Android without external network traffic. */
class DohDeviceRegressionTest {
    private fun fakeCall(
        body: ByteArray?, enqueued: CountDownLatch = CountDownLatch(1),
        cancelled: AtomicBoolean = AtomicBoolean(false)
    ): Call {
        val request = Request.Builder().url("https://example.invalid/dns-query").build()
        lateinit var call: Call
        call = Proxy.newProxyInstance(Call::class.java.classLoader, arrayOf(Call::class.java)) { _, method, args ->
            when (method.name) {
                "enqueue" -> {
                    enqueued.countDown()
                    if (body != null) {
                        val response = Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                            .code(200).message("OK").body(body.toResponseBody()).build()
                        (args!![0] as Callback).onResponse(call, response)
                    }
                    null
                }
                "cancel" -> { cancelled.set(true); null }
                "request" -> request
                "isCanceled" -> cancelled.get()
                "isExecuted" -> true
                else -> throw UnsupportedOperationException(method.name)
            }
        } as Call
        return call
    }

    @Test fun validDnsMessageIsParsedOnDevice() = runBlocking {
        val original = Message.newQuery(Record.newRecord(Name.fromString("example.test."), Type.A, DClass.IN))
        val decoded = fakeCall(original.toWire()).awaitDnsMessage()
        assertEquals(original.question.name, decoded.question.name)
        assertEquals(Type.A, decoded.question.type)
    }

    @Test fun oversizedDnsMessageIsRejectedOnDevice() = runBlocking {
        try {
            fakeCall(ByteArray(65_536)).awaitDnsMessage()
            fail("Oversized DNS responses must be rejected")
        } catch (error: IOException) {
            assertEquals("DNS response is too large", error.message)
        }
    }

    @Test fun coroutineCancellationCancelsCallOnDevice() = runBlocking {
        val enqueued = CountDownLatch(1)
        val cancelled = AtomicBoolean(false)
        val job = async(Dispatchers.IO) { fakeCall(null, enqueued, cancelled).awaitDnsMessage() }
        assertTrue("Call was not enqueued", enqueued.await(3, TimeUnit.SECONDS))
        withTimeout(2_000) { job.cancelAndJoin() }
        assertTrue("Cancelling the coroutine must cancel the call", cancelled.get())
    }
}
