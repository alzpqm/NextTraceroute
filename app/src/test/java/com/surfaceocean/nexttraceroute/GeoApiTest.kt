package com.surfaceocean.nexttraceroute

import org.junit.Assert.*
import org.junit.Test

class GeoApiTest {
    @Test fun ignoresMalformedAndUnrelatedReplies() {
        for (text in listOf("", "null", "[]", "not json", "{}", "{\"error\":\"unavailable\"}",
            "{\"ip\":[]}", "{\"ip\":\"8.8.8.8\"}")) {
            assertNull(text, parseGeoResponse(text, "1.1.1.1"))
        }
    }

    @Test fun missingOrNullFieldsDoNotCrashCallback() {
        val data = parseGeoResponse("""{"ip":"1.1.1.1","asnumber":null,"city":{},"lat":null,"lng":"unknown"}""", "1.1.1.1")!!
        assertEquals("", data.get("asnumber").asString)
        assertEquals("", data.get("city").asString)
        assertEquals("", data.get("whois").asString)
        assertEquals(0.0, data.get("lat").asDouble, 0.0)
        assertEquals(0.0, data.get("lng").asDouble, 0.0)
    }

    @Test fun acceptsNumericAsnAndEquivalentIpv6() {
        val data = parseGeoResponse("""{"ip":"2001:db8:0:0:0:0:0:1","asnumber":64500,"lat":"22.3","lng":114.2}""", "2001:db8::1")!!
        assertEquals("64500", data.get("asnumber").asString)
        assertEquals(22.3, data.get("lat").asDouble, 0.001)
        assertEquals(114.2, data.get("lng").asDouble, 0.001)
    }

    @Test fun discardsInvalidCoordinates() {
        val data = parseGeoResponse("""{"ip":"1.1.1.1","lat":"NaN","lng":181}""", "1.1.1.1")!!
        assertEquals(0.0, data.get("lat").asDouble, 0.0)
        assertEquals(0.0, data.get("lng").asDouble, 0.0)
    }
}
