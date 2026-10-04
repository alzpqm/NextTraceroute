package com.surfaceocean.nexttraceroute

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class TraceMapRequestTest {
    private val client = OkHttpClient.Builder().callTimeout(3, TimeUnit.SECONDS).build()

    private suspend fun withServer(response: (Socket) -> Unit, test: suspend (String) -> Unit) {
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { server ->
            val worker = thread(isDaemon = true, name = "trace-map-test") {
                runCatching {
                    server.accept().use { socket ->
                        socket.soTimeout = 3_000
                        val input = socket.getInputStream().bufferedReader()
                        while (!input.readLine().isNullOrEmpty()) { /* consume the GET headers */ }
                        response(socket)
                    }
                }
            }
            try {
                test("http://127.0.0.1:${server.localPort}/tracemap/api")
            } finally {
                server.close()
                worker.join(4_000)
            }
        }
    }

    @Test fun readsAndClosesACompleteResponse() = runBlocking {
        withServer({ socket ->
            val body = "https://example.com/map"
            socket.getOutputStream().apply {
                write("HTTP/1.1 200 OK\r\nContent-Length: ${body.length}\r\n\r\n$body".toByteArray())
                flush()
            }
        }) { url ->
            assertEquals("https://example.com/map",
                client.newCall(Request.Builder().url(url).build()).awaitTraceMapUrl())
        }
    }

    @Test fun cancellingDuringBodyReadClosesTheConnection() = runBlocking {
        val bodyStarted = CountDownLatch(1)
        val peerClosed = CountDownLatch(1)
        withServer({ socket ->
            socket.getOutputStream().apply {
                write("HTTP/1.1 200 OK\r\nContent-Length: 100\r\n\r\nhttps://".toByteArray())
                flush()
            }
            bodyStarted.countDown()
            if (socket.getInputStream().read() == -1) peerClosed.countDown()
        }) { url ->
            val job = async(Dispatchers.IO) {
                client.newCall(Request.Builder().url(url).build()).awaitTraceMapUrl()
            }
            assertTrue(bodyStarted.await(2, TimeUnit.SECONDS))
            withTimeout(1_000) { job.cancelAndJoin() }
            assertTrue("Cancelling the coroutine must also close the socket",
                peerClosed.await(2, TimeUnit.SECONDS))
        }
    }

    @Test fun rejectsOversizedResponseBodies() = runBlocking {
        withServer({ socket ->
            val body = "x".repeat(8_193)
            socket.getOutputStream().apply {
                write("HTTP/1.1 200 OK\r\nContent-Length: ${body.length}\r\n\r\n$body".toByteArray())
                flush()
            }
        }) { url ->
            try {
                client.newCall(Request.Builder().url(url).build()).awaitTraceMapUrl()
                fail("Oversized bodies must be rejected")
            } catch (error: IOException) {
                assertEquals("TraceMap response is too large", error.message)
            }
        }
    }
}
