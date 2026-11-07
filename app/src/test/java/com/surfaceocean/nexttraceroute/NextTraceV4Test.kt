package com.surfaceocean.nexttraceroute

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class NextTraceV4Test {
    private val grant = V4Grant("test-credential-only", System.currentTimeMillis() + 60_000)

    private suspend fun withServer(handler: (Socket, List<String>) -> Unit, test: suspend (NextTraceV4Client) -> Unit) {
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { server ->
            val worker = thread(isDaemon = true) {
                runCatching {
                    server.accept().use { socket ->
                        socket.soTimeout = 3000
                        val reader = socket.getInputStream().bufferedReader()
                        val headers = mutableListOf<String>()
                        while (true) {
                            val line = reader.readLine() ?: break
                            if (line.isEmpty()) break
                            headers.add(line)
                        }
                        handler(socket, headers)
                    }
                }
            }
            try { test(NextTraceV4Client(OkHttpClient(), "http://127.0.0.1:${server.localPort}")) }
            finally { server.close(); worker.join(4000) }
        }
    }

    private fun respond(socket: Socket, body: String, status: Int = 200, extra: String = "") {
        val bytes = body.toByteArray()
        socket.getOutputStream().apply {
            write("HTTP/1.1 $status Result\r\nContent-Length: ${bytes.size}\r\n$extra\r\n".toByteArray())
            write(bytes); flush()
        }
    }

    @Test fun sendsGetAndHeaderAndAcceptsMatchingGeo() = runBlocking {
        var headers = emptyList<String>()
        withServer({ socket, received ->
            headers = received
            respond(socket, """{"ip":"192.0.2.1","asnumber":"64512","domain":"example.test"}""")
        }) { api ->
            val data = api.lookup("192.0.2.1", grant)!!
            assertEquals("64512", data.get("asnumber").asString)
            assertEquals("example.test", data.get("domain").asString)
            assertTrue(headers.first().startsWith("GET /v4/ipGeo?ip=192.0.2.1 "))
            assertTrue(headers.any { it == "X-NextTrace-Token: ${grant.token}" })
            assertFalse(headers.first().contains(grant.token))
            assertFalse(headers.any { it.startsWith("Authorization:") })
        }
    }

    @Test fun rejectsUnrelatedGeoReply() = runBlocking {
        withServer({ socket, _ -> respond(socket, """{"ip":"192.0.2.2","asnumber":"64512"}""") }) { api ->
            assertNull(api.lookup("192.0.2.1", grant))
        }
    }

    @Test fun authorizationAndQuotaErrorsKeepStatusAndRedactBody() = runBlocking {
        for (status in listOf(401, 429)) {
            withServer({ socket, _ -> respond(socket, grant.token, status) }) { api ->
                try { api.lookup("192.0.2.1", grant); fail("Expected HTTP error") }
                catch (error: V4ApiException) {
                    assertEquals(status, error.status)
                    assertFalse(error.toString().contains(grant.token))
                }
            }
        }
    }

    @Test fun redirectsAreNotFollowed() = runBlocking {
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { destination ->
            destination.soTimeout = 250
            withServer({ socket, _ -> respond(socket, "", 302, "Location: http://127.0.0.1:${destination.localPort}/leak\r\n") }) { api ->
                try { api.lookup("192.0.2.1", grant); fail("Expected redirect rejection") }
                catch (error: V4ApiException) { assertEquals(302, error.status) }
            }
            try { destination.accept().close(); fail("Credential reached redirect destination") }
            catch (_: java.net.SocketTimeoutException) { /* no connection made */ }
        }
    }

    @Test fun refusesOversizedResponses() = runBlocking {
        withServer({ socket, _ -> respond(socket, "x".repeat(65_537)) }) { api ->
            try { api.lookup("192.0.2.1", grant); fail("Expected size rejection") }
            catch (_: IOException) { }
        }
    }

    @Test fun cancellationClosesBodySocket() = runBlocking {
        val started = CountDownLatch(1)
        val closed = CountDownLatch(1)
        withServer({ socket, _ ->
            socket.getOutputStream().apply {
                write("HTTP/1.1 200 OK\r\nContent-Length: 100\r\n\r\n{".toByteArray()); flush()
            }
            started.countDown()
            if (socket.getInputStream().read() == -1) closed.countDown()
        }) { api ->
            val job = async(Dispatchers.IO) { api.lookup("192.0.2.1", grant) }
            assertTrue(started.await(2, TimeUnit.SECONDS))
            withTimeout(1000) { job.cancelAndJoin() }
            assertTrue(closed.await(2, TimeUnit.SECONDS))
        }
    }

    @Test fun verifiesOfficialStatusAndQuota() = runBlocking {
        val expires = Instant.ofEpochMilli(grant.expiresAtMillis).toString()
        withServer({ socket, _ -> respond(socket, """{"status":"active","has_quota":true,"expires_at":"$expires"}""") }) { api ->
            assertEquals(grant.expiresAtMillis, api.verify(grant).expiresAtMillis)
        }
        withServer({ socket, _ -> respond(socket, """{"status":"quota_exhausted","has_quota":false}""") }) { api ->
            try { api.verify(grant); fail("Expected quota failure") }
            catch (e: V4ApiException) { assertEquals(429, e.status) }
        }
    }

    @Test fun validatesGrantExpiryAndHeaderCharactersWithoutLeakingToken() {
        val future = Instant.ofEpochMilli(grant.expiresAtMillis)
        assertNotNull(parseV4Grant("""{"token":"test-only","expires_at":"$future"}"""))
        assertNull(parseV4Grant("""{"token":"invalid\r\nheader","expires_at":"$future"}"""))
        val expired = """{"token":"test-only","expires_at":"2000-01-01T00:00:00Z"}"""
        assertNull(parseV4Grant(expired))
        assertFalse(parseV4Grant(expired, requireUnexpired = false)!!.usable())
        assertFalse(grant.toString().contains(grant.token))
    }

    @Test fun confinesBrowserMainFrameAndResources() {
        assertTrue(isV4AuthorizationPage(V4_TOKEN_PAGE))
        assertFalse(isV4AuthorizationPage("https://api.nxtrace.org.evil.test/v4/api-tokens"))
        assertFalse(isV4AuthorizationPage("https://api.nxtrace.org" + '@' + "evil.test/v4/api-tokens"))
        assertFalse(isV4AuthorizationPage("http://api.nxtrace.org/v4/api-tokens"))
        assertFalse(isV4AuthorizationPage("https://api.nxtrace.org:444/v4/api-tokens"))
        assertFalse(isV4AuthorizationPage("https://api.nxtrace.org/v4/api-tokens?redirect=https://example.test"))
        assertFalse(isV4WebResource("file:///data/data/example"))
        assertTrue(isV4WebResource("https://challenges.cloudflare.com/turnstile/v0/api.js"))
        assertTrue(isV4WebResource("about:srcdoc"))
    }
}
