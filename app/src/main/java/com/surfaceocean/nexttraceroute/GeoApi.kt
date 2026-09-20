package com.surfaceocean.nexttraceroute

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import inet.ipaddr.IPAddressString
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.atomic.AtomicReference

internal fun parseGeoResponse(text: String, expectedIp: String): JsonObject? {
    if (text.length > 65_536) return null
    return runCatching {
        val element = JsonParser.parseString(text)
        if (!element.isJsonObject) return null
        val raw = element.asJsonObject
        val ipElement = raw.get("ip")
        if (ipElement == null || !ipElement.isJsonPrimitive || !ipElement.asJsonPrimitive.isString) return null
        val ip = ipElement.asString
        if (identifyTraceTarget(ip) !in listOf(IPV4_IDENTIFIER, IPV6_IDENTIFIER) ||
            IPAddressString(ip).toAddress() != IPAddressString(expectedIp).toAddress()) return null
        JsonObject().apply {
            addProperty("ip", ip)
            for (key in listOf("asnumber", "whois", "country", "country_en", "prov", "prov_en",
                "city", "city_en", "domain", "owner", "isp")) {
                val value = raw.get(key)
                addProperty(key, if (value?.isJsonPrimitive == true &&
                    (value.asJsonPrimitive.isString || value.asJsonPrimitive.isNumber)) value.asString else "")
            }
            for ((key, limit) in listOf("lat" to 90.0, "lng" to 180.0)) {
                val value = raw.get(key)
                val number = if (value?.isJsonPrimitive == true) value.asString.toDoubleOrNull() else null
                addProperty(key, number?.takeIf { it.isFinite() && it in -limit..limit } ?: 0.0)
            }
        }
    }.getOrNull()
}

/** Callback threads only deliver values; the owning coroutine updates UI state. */
internal class GeoApiSession(client: OkHttpClient, request: Request) : AutoCloseable {
    private data class Pending(val ip: String, val result: CompletableDeferred<JsonObject?>)
    private val pending = AtomicReference<Pending?>(null)
    private val opened = CompletableDeferred<Boolean>()
    private val socket = client.newWebSocket(request, object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) { opened.complete(true) }
        override fun onMessage(webSocket: WebSocket, text: String) {
            val waiting = pending.get() ?: return
            val result = parseGeoResponse(text, waiting.ip) ?: return
            waiting.result.complete(result)
        }
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { failed() }
        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(code, null)
            failed()
        }
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { failed() }
    })

    private fun failed() {
        opened.complete(false)
        pending.getAndSet(null)?.result?.complete(null)
    }

    suspend fun lookup(ip: String): JsonObject? {
        if (withTimeoutOrNull(5_000) { opened.await() } != true) return null
        val waiting = Pending(ip, CompletableDeferred())
        check(pending.compareAndSet(null, waiting)) { "Only one Geo lookup may be active" }
        return try {
            if (!socket.send(ip)) null else withTimeoutOrNull(3_000) { waiting.result.await() }
        } finally {
            pending.compareAndSet(waiting, null)
        }
    }

    override fun close() {
        failed()
        socket.cancel()
    }
}
