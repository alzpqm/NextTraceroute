package com.surfaceocean.nexttraceroute

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal const val V4_TOKEN_PAGE = "https://api.nxtrace.org/v4/api-tokens"
internal const val V4_ORIGIN = "https://api.nxtrace.org"

/** Deliberately not a data class: credentials must never appear in toString(). */
internal class V4Grant(val token: String, val expiresAtMillis: Long) {
    fun usable(now: Long = System.currentTimeMillis()) = validV4Token(token) && expiresAtMillis > now
    override fun toString() = "V4Grant([redacted])"
}

internal fun validV4Token(token: String) = token.length in 1..4096 && token.all { it.code in 33..126 }

internal fun parseV4Grant(json: String, requireUnexpired: Boolean = true, now: Long = System.currentTimeMillis()): V4Grant? {
    if (json.length > 8192) return null
    return runCatching {
        val obj = JsonParser.parseString(json).asJsonObject
        val token = obj.get("token").asString
        val expires = Instant.parse(obj.get("expires_at").asString).toEpochMilli()
        if (!validV4Token(token) || expires <= 0 || (requireUnexpired && expires <= now)) null
        else V4Grant(token, expires)
    }.getOrNull()
}

internal class V4ApiException(val status: Int) : IOException("NextTrace v4 request failed (HTTP $status)")

internal class NextTraceV4Client(
    baseClient: OkHttpClient = OkHttpClient(),
    private val origin: String = V4_ORIGIN
) {
    private val client = baseClient.newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS).readTimeout(5, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).build()

    private fun request(path: String, grant: V4Grant, ip: String? = null): Request {
        if (!grant.usable()) throw V4ApiException(401)
        val url = (origin + path).toHttpUrl().newBuilder().apply {
            if (ip != null) addQueryParameter("ip", ip)
        }.build()
        return Request.Builder().url(url).header("Accept", "application/json")
            .header("User-Agent", "NextTrace v$NEXTTRACE_CORE_VERSION/linux/android NextTracerouteAndroid/" + BuildConfig.VERSION_NAME)
            .header("X-NextTrace-Token", grant.token).build()
    }

    suspend fun lookup(ip: String, grant: V4Grant): JsonObject? {
        require(identifyTraceTarget(ip) in listOf(IPV4_IDENTIFIER, IPV6_IDENTIFIER))
        val body = client.newCall(request("/v4/ipGeo", grant, ip)).awaitV4Body()
        return parseGeoResponse(body, ip)
    }

    /** Only official status JSON can confirm an imported browser credential. */
    suspend fun verify(grant: V4Grant): V4Grant {
        val text = client.newCall(request("/v4/api-tokens/status", grant)).awaitV4Body()
        val obj = runCatching { JsonParser.parseString(text).asJsonObject }.getOrNull()
            ?: throw IOException("Invalid NextTrace authorization response")
        val status = obj.get("status")?.takeIf { it.isJsonPrimitive }?.asString
        if (status == "quota_exhausted" || obj.get("has_quota")?.toString() == "false") throw V4ApiException(429)
        if (status != "active") throw V4ApiException(401)
        val expires = runCatching { Instant.parse(obj.get("expires_at").asString).toEpochMilli() }
            .getOrDefault(grant.expiresAtMillis)
        if (expires <= System.currentTimeMillis()) throw V4ApiException(401)
        return V4Grant(grant.token, expires)
    }
}

private suspend fun Call.awaitV4Body(): String = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isActive) continuation.resumeWithException(IOException("NextTrace v4 connection failed"))
        }

        override fun onResponse(call: Call, response: Response) {
            val result = runCatching {
                response.use {
                    // Never expose backend error bodies: they may echo a credential.
                    if (!it.isSuccessful) throw V4ApiException(it.code)
                    val buffer = java.io.ByteArrayOutputStream()
                    it.body.byteStream().use { input ->
                        val chunk = ByteArray(4096)
                        while (true) {
                            val count = input.read(chunk, 0, minOf(chunk.size, 65_537 - buffer.size()))
                            if (count == -1) break
                            buffer.write(chunk, 0, count)
                            if (buffer.size() > 65_536) throw IOException("NextTrace v4 response too large")
                        }
                    }
                    buffer.toString(Charsets.UTF_8.name())
                }
            }
            if (continuation.isActive) {
                result.fold({ continuation.resume(it) }, {
                    continuation.resumeWithException(if (it is V4ApiException) it else IOException("Invalid NextTrace v4 response"))
                })
            }
        }
    })
}
