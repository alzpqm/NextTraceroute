package com.surfaceocean.nexttraceroute

import org.junit.Assert.assertEquals
import org.junit.Test
import org.xbill.DNS.ARecord
import org.xbill.DNS.CNAMERecord
import org.xbill.DNS.DClass
import org.xbill.DNS.Name
import org.xbill.DNS.Type
import java.net.InetAddress

class TraceDnsTest {
    @Test fun followsCnameOnlyResponsesAndDeduplicatesAddresses() {
        val target = Name.fromString("example.com.")
        val alias = Name.fromString("cdn.example.com.")
        val record = ARecord(alias, DClass.IN, 60, InetAddress.getByName("192.0.2.1"))
        val result = resolveAddressRecords("example.com.", Type.A) { name, _ ->
            if (name == target) listOf(CNAMERecord(target, DClass.IN, 60, alias)) else listOf(record, record)
        }
        assertEquals(listOf("192.0.2.1"), result)
    }

    @Test fun acceptsCnameAndAddressInSameResponse() {
        val target = Name.fromString("example.com.")
        val alias = Name.fromString("cdn.example.com.")
        assertEquals(listOf("192.0.2.2"), resolveAddressRecords("example.com", Type.A) { _, _ ->
            listOf(CNAMERecord(target, DClass.IN, 60, alias),
                ARecord(alias, DClass.IN, 60, InetAddress.getByName("192.0.2.2")))
        })
    }

    @Test fun cnameCycleTerminates() {
        val target = Name.fromString("example.com.")
        val alias = Name.fromString("cdn.example.com.")
        var calls = 0
        assertEquals(emptyList<String>(), resolveAddressRecords("example.com", Type.A) { name, _ ->
            calls++
            listOf(CNAMERecord(name, DClass.IN, 60, if (name == target) alias else target))
        })
        assertEquals(2, calls)
    }

    @Test fun unrelatedAnswerIsNotShownAsTarget() {
        assertEquals(emptyList<String>(), resolveAddressRecords("example.com", Type.A) { _, _ ->
            listOf(ARecord(Name.fromString("other.example.com."), DClass.IN, 60, InetAddress.getByName("192.0.2.3")))
        })
    }
}
