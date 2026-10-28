package com.surfaceocean.nexttraceroute

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.xbill.DNS.AAAARecord
import org.xbill.DNS.ARecord
import org.xbill.DNS.CNAMERecord
import org.xbill.DNS.DClass
import org.xbill.DNS.Message
import org.xbill.DNS.Name
import org.xbill.DNS.Rcode
import org.xbill.DNS.Record
import org.xbill.DNS.Section
import org.xbill.DNS.SimpleResolver
import org.xbill.DNS.Type
import org.xbill.DNS.PTRRecord
import org.xbill.DNS.ReverseMap
import java.net.InetAddress
import java.io.IOException
import java.time.Duration
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val dnsHttpClient = OkHttpClient.Builder().callTimeout(4, TimeUnit.SECONDS).build()

/** RFC 8484 limits an application/dns-message body to 65,535 bytes. */
internal suspend fun Call.awaitDnsMessage(): Message = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            try {
                val message = response.use {
                    if (!it.isSuccessful) throw IOException("DNS service returned HTTP ${it.code}")
                    val source = it.body.source()
                    source.request(65_536)
                    if (source.buffer.size > 65_535) throw IOException("DNS response is too large")
                    Message(source.readByteArray())
                }
                if (continuation.isActive) continuation.resume(message)
            } catch (error: Exception) {
                if (continuation.isActive) continuation.resumeWithException(error)
            }
        }
    })
}

/** A/AAAA queries return immutable results; Compose state is updated once on Main. */
suspend fun resolveTraceAddresses(
    name: String,
    mode: String,
    server: String,
    dohUrl: String
): List<String> = coroutineScope {
    val queries = listOf(Type.A, Type.AAAA).map { type ->
        async(Dispatchers.IO) {
            try {
                val context = currentCoroutineContext()
                resolveAddressRecords(name, type) { queryName, queryType ->
                    context.ensureActive()
                    val query = Message.newQuery(Record.newRecord(queryName, queryType, DClass.IN))
                    val response = if (mode == "doh") {
                        val wire = Base64.getUrlEncoder().withoutPadding().encodeToString(query.toWire())
                        val url = dohUrl.toHttpUrl().newBuilder().addQueryParameter("dns", wire).build()
                        dnsHttpClient.newCall(Request.Builder().url(url)
                            .header("Accept", "application/dns-message").build()).awaitDnsMessage()
                    } else {
                        SimpleResolver(server).apply {
                            tcp = mode == "tcp"
                            setTimeout(Duration.ofSeconds(4))
                        }.send(query)
                    }
                    if (response.rcode == Rcode.NOERROR) response.getSection(Section.ANSWER) else emptyList()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
    queries.flatMap { it.await() }.distinct()
}

/** Follow aliases with a fixed bound and cycle detection, including CNAME-only DoH answers. */
internal suspend fun resolveAddressRecords(
    host: String,
    type: Int,
    query: suspend (Name, Int) -> List<Record>
): List<String> {
    var name = Name.fromString(host.removeSuffix(".") + ".")
    val visited = mutableSetOf<Name>()
    repeat(8) {
        if (!visited.add(name)) return emptyList()
        val answer = query(name, type)
        var current = name
        val answerNames = mutableSetOf<Name>()
        while (answerNames.add(current)) {
            val addresses = answer.filter { it.name == current }.mapNotNull {
                when {
                    type == Type.A && it is ARecord -> it.address.hostAddress
                    type == Type.AAAA && it is AAAARecord -> it.address.hostAddress
                    else -> null
                }
            }
            if (addresses.isNotEmpty()) return addresses.distinct()
            val alias = answer.filterIsInstance<CNAMERecord>().firstOrNull { it.name == current }
                ?: break
            current = alias.target
        }
        if (current == name) return emptyList()
        name = current
    }
    return emptyList()
}

internal suspend fun resolveTraceHostname(ip: String, mode: String, server: String, dohUrl: String): String =
    withContext(Dispatchers.IO) {
        try {
            val name = ReverseMap.fromAddress(InetAddress.getByName(ip))
            val query = Message.newQuery(Record.newRecord(name, Type.PTR, DClass.IN))
            val response = if (mode == "doh") {
                val wire = Base64.getUrlEncoder().withoutPadding().encodeToString(query.toWire())
                val url = dohUrl.toHttpUrl().newBuilder().addQueryParameter("dns", wire).build()
                dnsHttpClient
                    .newCall(Request.Builder().url(url).header("Accept", "application/dns-message").build())
                    .awaitDnsMessage()
            } else {
                SimpleResolver(server).apply {
                    tcp = mode == "tcp"
                    setTimeout(Duration.ofSeconds(4))
                }.send(query)
            }
            response.getSection(Section.ANSWER).filterIsInstance<PTRRecord>()
                .firstOrNull { it.name == name }?.target?.toString() ?: "*"
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            "*"
        }
    }
