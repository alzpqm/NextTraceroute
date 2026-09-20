package com.surfaceocean.nexttraceroute

import org.junit.Assert.assertEquals
import org.junit.Test

class TargetRegressionTest {
    @Test fun pastedReplacementCanBeShorterThanPreviousInput() {
        assertEquals("example.com", normalizePastedTarget("x".repeat(200), "https://example.com/test"))
        assertEquals("https://example.com", normalizePastedTarget("https://example.co", "https://example.com"))
        assertEquals("example.com", normalizePastedTarget("", "example.com\n"))
    }
    private val handler = TracerouteHandler()

    @Test fun recognizesEquivalentIpv6DestinationAddresses() {
        org.junit.Assert.assertTrue(sameTraceAddress("2001:db8::1", "2001:db8:0:0:0:0:0:1"))
        org.junit.Assert.assertFalse(sameTraceAddress("*", "2001:db8::1"))
        org.junit.Assert.assertFalse(sameTraceAddress("1.1.1.1", "1.0.0.1"))
    }

    @Test
    fun cdnSpeedTestUrlExtractsHostRegardlessOfQueryLength() {
        val host = "ipv4-c004-hkg001-smartone-isp.1.oca.nflxvideo.net"
        for (query in listOf("c=hk&n=17924&v=31", "t=" + "a".repeat(100_000))) {
            val target = normalizeTargetInput("https://$host/speedtest?$query")
            assertEquals(host, target)
            assertEquals(HOSTNAME_IDENTIFIER, handler.identifyInput(target))
        }
    }

    @Test
    fun authorityIsExtractedEvenWhenPathIsNotUriEscaped() {
        assertEquals("example.com", normalizeTargetInput("https://example.com:443/a b?q=%"))
        assertEquals("example.com", normalizeTargetInput("//example.com/path"))
        assertEquals("2001:db8::1", normalizeTargetInput("https://[2001:db8::1]:443/a b"))
        assertEquals("example.com", normalizeTargetInput("https://EXAMPLE.com./path"))
    }

    @Test
    fun rejectsOversizedNamesWithoutRecursiveRegex() {
        assertEquals(ERROR_IDENTIFIER, handler.identifyInput("a".repeat(64) + ".com"))
        assertEquals(ERROR_IDENTIFIER, handler.identifyInput("a.".repeat(4_000) + "com"))
        assertEquals(ERROR_IDENTIFIER, handler.identifyInput("a".repeat(100_000)))
        assertEquals(HOSTNAME_IDENTIFIER, handler.identifyInput("a".repeat(63) + ".com"))
    }

    @Test
    fun validatesIpv6RatherThanOnlyItsCharacterPattern() {
        assertEquals(ERROR_IDENTIFIER, handler.identifyInput("1:2:3:4:5:6:7:8::"))
        assertEquals(IPV6_IDENTIFIER, handler.identifyInput("::ffff:192.0.2.1"))
        assertEquals(ERROR_IDENTIFIER, handler.identifyInput("[2001:db8::1]garbage"))
        assertEquals(ERROR_IDENTIFIER, handler.identifyInput("999.1.1.1"))
    }
}
