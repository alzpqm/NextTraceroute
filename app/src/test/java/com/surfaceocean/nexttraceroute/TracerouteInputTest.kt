package com.surfaceocean.nexttraceroute

import androidx.compose.runtime.mutableStateOf
import org.junit.Assert.assertEquals
import org.junit.Test

class TracerouteInputTest {
    @Test
    fun normalizeTargetInput_extractsHostsFromUrlsAndHostPortPairs() {
        assertEquals("example.com", normalizeTargetInput(" https://example.com/path?q=1 "))
        assertEquals("example.com", normalizeTargetInput("example.com:443/path"))
        assertEquals("2001:db8::1", normalizeTargetInput("https://[2001:db8::1]:8443/path"))
    }

    @Test
    fun normalizeTargetInput_preservesDirectTargets() {
        assertEquals("1.1.1.1", normalizeTargetInput("1.1.1.1"))
        assertEquals("2001:4860:4860::8888", normalizeTargetInput("2001:4860:4860::8888"))
        assertEquals("", normalizeTargetInput("   "))
    }

    @Test
    fun identifyInput_rejectsInvalidTargets() {
        val handler = TracerouteHandler()

        assertEquals(IPV4_IDENTIFIER, handler.identifyInput("1.1.1.1"))
        assertEquals(IPV6_IDENTIFIER, handler.identifyInput("2001:4860:4860::8888"))
        assertEquals(HOSTNAME_IDENTIFIER, handler.identifyInput("api.nxtrace.org"))
        assertEquals(ERROR_IDENTIFIER, handler.identifyInput("https://example.com"))
        assertEquals(ERROR_IDENTIFIER, handler.identifyInput("bad host"))
    }

    @Test
    fun buildTraceHistory_ignoresEmptyRowsAndReturnsEmptyForNoResult() {
        val emptyGrid = listOf(
            listOf(
                listOf(mutableStateOf("")),
                listOf(mutableStateOf(""))
            )
        )
        assertEquals("", buildTraceHistory("1.1.1.1", "", emptyGrid))

        val populatedGrid = listOf(
            listOf(
                listOf(mutableStateOf("1"), mutableStateOf("1.1.1.1")),
                listOf(mutableStateOf("Cloudflare"))
            ),
            emptyGrid.first()
        )
        assertEquals(
            "Traceroute Result:\nIP:1.1.1.1\nDomain:one.one.one.one\n1, 1.1.1.1\nCloudflare",
            buildTraceHistory("1.1.1.1", "one.one.one.one", populatedGrid)
        )
    }
}
