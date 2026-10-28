package com.surfaceocean.nexttraceroute

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.xbill.DNS.ARecord
import org.xbill.DNS.DClass
import org.xbill.DNS.Flags
import org.xbill.DNS.Message
import org.xbill.DNS.Name
import org.xbill.DNS.PTRRecord
import org.xbill.DNS.Section
import org.xbill.DNS.Type
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class DohTransportTest {
    private fun serveConnections(count: Int, handle: (Socket) -> Unit, test: (String) -> Unit) {
        ServerSocket(0, count, InetAddress.getByName("127.0.0.1")).use { server ->
            server.soTimeout = 5_000
            val workers = mutableListOf<Thread>()
            val acceptor = thread(isDaemon = true, name = "doh-test-acceptor") {
                repeat(count) {
                    val socket = try { server.accept() } catch (_: Exception) { return@thread }
                    workers.add(thread(isDaemon = true, name = "doh-test-client") {
                        socket.use { handle(it) }
                    })
                }
            }
            try {
                test("http://127.0.0.1:${server.localPort}/dns-query")
            } finally {
                server.close()
                acceptor.join(6_000)
                workers.forEach { it.join(5_000) }
            }
        }
    }

    private fun readRequest(socket: Socket): String {
        val input = socket.getInputStream().bufferedReader()
        val requestLine = input.readLine()
        while (!input.readLine().isNullOrEmpty()) { }
        return requestLine
    }

    @Test fun cancellingDohDuringResponseBodyDoesNotWaitForNetworkTimeout() = runBlocking {
        val started = CountDownLatch(2)
        val closed = CountDownLatch(2)
        serveConnections(2, { socket ->
            readRequest(socket)
            socket.getOutputStream().apply {
                write("HTTP/1.1 200 OK\r\nContent-Length: 100\r\n\r\nx".toByteArray())
                flush()
            }
            started.countDown()
            socket.soTimeout = 3_000
            try {
                if (socket.getInputStream().read() == -1) closed.countDown()
            } catch (_: SocketTimeoutException) {
                // The client did not close this connection before the deadline.
            } catch (_: SocketException) {
                closed.countDown()
            }
        }) { url ->
            val job = async(Dispatchers.IO) { resolveTraceAddresses("example.test", "doh", "", url) }
            assertTrue("Both DNS response bodies should start", started.await(5, TimeUnit.SECONDS))
            job.cancel()
            assertTrue("Cancellation should stop both requests promptly",
                runBlocking { withTimeoutOrNull(1_200) { job.join(); true } } == true)
            assertTrue("Cancellation should close both underlying connections", closed.await(2, TimeUnit.SECONDS))
        }
    }

    @Test fun oversizedDohBodyIsRejectedBeforeItsEnd() = runBlocking {
        val sent = CountDownLatch(1)
        val release = CountDownLatch(1)
        serveConnections(1, { socket ->
            readRequest(socket)
            socket.getOutputStream().apply {
                write("HTTP/1.1 200 OK\r\nContent-Length: 1000000\r\n\r\n".toByteArray())
                write(ByteArray(65_536))
                flush()
            }
            sent.countDown()
            release.await(5, TimeUnit.SECONDS)
        }) { url ->
            val job = async(Dispatchers.IO) { resolveTraceHostname("192.0.2.1", "doh", "", url) }
            try {
                assertTrue("Oversized response should start", sent.await(5, TimeUnit.SECONDS))
                assertEquals("*", runBlocking { withTimeoutOrNull(1_500) { job.await() } })
            } finally {
                release.countDown()
            }
        }
    }

    @Test fun normalDohResponseStillResolvesAnAddress() = runBlocking {
        serveConnections(2, { socket ->
            val requestLine = readRequest(socket)
            val encoded = requestLine.substringAfter("dns=").substringBefore(' ')
            val query = Message(Base64.getUrlDecoder().decode(encoded))
            val response = Message(query.header.id).apply {
                header.setFlag(Flags.QR.toInt())
                addRecord(query.question, Section.QUESTION)
                if (query.question.type == Type.A) {
                    addRecord(ARecord(query.question.name, DClass.IN, 60,
                        InetAddress.getByName("192.0.2.7")), Section.ANSWER)
                }
            }.toWire()
            socket.getOutputStream().apply {
                write("HTTP/1.1 200 OK\r\nContent-Length: ${response.size}\r\n\r\n".toByteArray())
                write(response)
                flush()
            }
        }) { url ->
            assertEquals(listOf("192.0.2.7"), runBlocking {
                resolveTraceAddresses("example.test", "doh", "", url)
            })
        }
    }

    @Test fun normalDohResponseStillResolvesAReverseHostname() = runBlocking {
        serveConnections(1, { socket ->
            val requestLine = readRequest(socket)
            val encoded = requestLine.substringAfter("dns=").substringBefore(' ')
            val query = Message(Base64.getUrlDecoder().decode(encoded))
            val response = Message(query.header.id).apply {
                header.setFlag(Flags.QR.toInt())
                addRecord(query.question, Section.QUESTION)
                addRecord(PTRRecord(query.question.name, DClass.IN, 60,
                    Name.fromString("router.example.test.")), Section.ANSWER)
            }.toWire()
            socket.getOutputStream().apply {
                write("HTTP/1.1 200 OK\r\nContent-Length: ${response.size}\r\n\r\n".toByteArray())
                write(response)
                flush()
            }
        }) { url ->
            assertEquals("router.example.test.", runBlocking {
                resolveTraceHostname("192.0.2.1", "doh", "", url)
            })
        }
    }
}
