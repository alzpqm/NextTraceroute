package com.surfaceocean.nexttraceroute

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.sync.Mutex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class GeoProcessingAuditTest {
    @get:Rule val compose = createComposeRule()

    @Test fun hopArrivingDuringLookupIsNotSkipped() {
        // A local socket holds the TLS handshake, keeping the real lookup suspended.
        // No production server, token, device address or successful TLS response is needed.
        val server = ServerSocket(0)
        val connected = CountDownLatch(1)
        val release = CountDownLatch(1)
        val worker = thread(name = "audit-local-server", isDaemon = true) {
            runCatching {
                server.accept().use {
                    connected.countDown()
                    release.await(10, TimeUnit.SECONDS)
                }
            }
        }
        val rows = MutableList(2) { MutableList(2) { MutableList(4) { mutableStateOf("") } } }
        rows[1][0][1].value = "1.1.1.1"
        val finished = mutableStateOf(false)
        val host = mutableStateOf("localhost:${server.localPort}")
        val backendIp = mutableStateOf("127.0.0.1")
        val token = mutableStateOf("synthetic-audit-token")
        val language = mutableStateOf("en")
        val insertion = mutableStateOf("1.1.1.1")
        val mutex = Mutex()
        val jobs = mutableListOf<Int>()
        val traceMap = mutableListOf<List<MutableMap<String, Any?>>>()
        val handler = TracerouteHandler()
        try {
            compose.setContent {
                handler.MainWSHandler(
                    threadMutex = mutex, tracerouteThreadsIntList = jobs,
                    scope = rememberCoroutineScope(),
                    apiHostName = host, preferredAPIIp = backendIp, apiToken = token,
                    gridDataList = rows, currentLanguage = language,
                    traceMapThreadsMapList = traceMap,
                    insertion = insertion, isAPIFinished = finished
                )
            }
            assertTrue("Local handshake never started", connected.await(10, TimeUnit.SECONDS))
            compose.runOnIdle { rows[0][0][1].value = "127.0.0.1" }
            compose.waitUntil(10_000) { finished.value }
            compose.runOnIdle { assertEquals("RFC1122", rows[0][0][2].value) }
        } finally {
            release.countDown()
            server.close()
            worker.join(1_000)
        }
    }
}
