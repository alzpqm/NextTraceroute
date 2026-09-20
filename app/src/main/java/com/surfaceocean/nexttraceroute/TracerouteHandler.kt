/*

NextTraceroute, an Android traceroute app using Nexttrace API
Copyright (C) 2024-2026 surfaceocean
Project: https://github.com/alzpqm/NextTraceroute
Upstream: https://github.com/nxtrace/NextTraceroute
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.

Disclaimer: The NextTrace API (hosted at nxtrace.org) used by default in this program is not managed by the program's developer.
We do not guarantee the performance, accuracy, or any other aspect of the NextTrace API,
nor do we endorse, approve, or guarantee the results returned by the NextTrace API. Users may customize the API server address themselves.

This project uses the libraries listed below. Detailed information can be found in the LICENSE file of this project.
The "dnsjava" library is licensed under the BSD 3-Clause License.
The "seancfoley/IPAddress" library is licensed under the Apache 2.0 License.
The "square/okhttp" library is licensed under the Apache 2.0 License.
The "gson" library is licensed under the Apache 2.0 License.
The "slf4j-android" library is licensed under the MIT License.
The "androidx" library is licensed under the Apache 2.0 License.
The "Compose Color Picker" library is licensed under the MIT License.

*/

package com.surfaceocean.nexttraceroute


import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import inet.ipaddr.AddressStringException
import inet.ipaddr.IPAddress
import inet.ipaddr.IPAddressString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.math.BigInteger
import java.net.InetAddress
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.random.Random


const val MAGIC_NEGATIVE_INT = -114514
const val IPV4_IDENTIFIER = "IPv4"
const val IPV6_IDENTIFIER = "IPv6"
const val HOSTNAME_IDENTIFIER = "Hostname"
const val ERROR_IDENTIFIER = "ERR"
const val MAGIC_UUID = "e3ee9949-bd5f-401c-8a57-395a98ed40ee"
const val NEXTTRACE_CORE_VERSION = "1.7.2"


val RESERVED_IPV4_CIDR = mapOf(
    "0.0.0.0/8" to "RFC1122",
    "100.64.0.0/10" to "RFC6598",
    "127.0.0.0/8" to "RFC1122",
    "169.254.0.0/16" to "RFC3927",
    "192.0.0.0/24" to "RFC6890",
    "192.0.2.0/24" to "RFC5737",
    "192.88.99.0/24" to "RFC3068",
    "198.18.0.0/15" to "RFC2544",
    "198.51.100.0/24" to "RFC5737",
    "203.0.113.0/24" to "RFC5737",
    "224.0.0.0/4" to "RFC5771",
    "255.255.255.255/32" to "RFC0919",
    "240.0.0.0/4" to "RFC1112",
    "10.0.0.0/8" to "RFC1918",
    "172.16.0.0/12" to "RFC1918",
    "192.168.0.0/16" to "RFC1918",
    "192.52.193.0/24" to "RFC7450",
    "6.0.0.0/8" to "DoD",
    "7.0.0.0/8" to "DoD",
    "11.0.0.0/8" to "DoD",
    "21.0.0.0/8" to "DoD",
    "22.0.0.0/8" to "DoD",
    "26.0.0.0/8" to "DoD",
    "28.0.0.0/8" to "DoD",
    "29.0.0.0/8" to "DoD",
    "30.0.0.0/8" to "DoD",
    "33.0.0.0/8" to "DoD",
    "55.0.0.0/8" to "DoD",
    "214.0.0.0/8" to "DoD",
    "215.0.0.0/8" to "DoD"

)

val RESERVED_IPV6_CIDR = mapOf(
    "fe80::/10" to "RFC4291",
    "ff00::/8" to "RFC4291",
    "fec0::/10" to "RFC3879",
    "fe00::/9" to "RFC4291",
    "64:ff9b::/96" to "RFC6052",
    "0::/96" to "RFC4291",
    "64:ff9b:1::/48" to "RFC6052",
    "2001:db8::/32" to "RFC3849",
    "2002::/16" to "RFC3056",
    "fc00::/7" to "RFC4193"
)


class TracerouteHandler {


    suspend fun testNativePing(
        v4Status: MutableState<Boolean>,
        v6Status: MutableState<Boolean>,
        errorText: MutableState<String>
    ) {
        fun commandExists(command: String): Boolean = try {
            val process = ProcessBuilder(command).redirectErrorStream(true).start()
            val finished = process.waitFor(2, TimeUnit.SECONDS)
            if (!finished) process.destroyForcibly()
            finished
        } catch (e: Exception) {
            Log.e("testNativePing", "$command is unavailable", e)
            false
        }

        val (ipv4, ipv6) = withContext(Dispatchers.IO) {
            commandExists("ping") to commandExists("ping6")
        }
        v4Status.value = ipv4
        v6Status.value = ipv6
        if (v4Status.value && v6Status.value) {
            errorText.value = ""
        } else if (v4Status.value) {
            errorText.value = "IPv6 native ping failed! Using linux api instead. (Unstable)"
        } else if (v6Status.value) {
            errorText.value = "IPv4 native ping failed! Using linux api instead. (Unstable)"
        } else {
            errorText.value =
                "IPv4 and IPv6 native ping failed! Using linux api instead. (Unstable)"
        }

    }

    private fun extractRttValues(inputString: String): String {
        //val regex = """(?i)rtt[^0-9]*(\d+(\.\d+)?)(/(\d+(\.\d+)?))*""".toRegex()
        val regex = "(?i)^.*rtt.*=\\s*(.*)$".toRegex(setOf(RegexOption.MULTILINE))
        val matchResult = regex.find(inputString)
        //var finalResult=""
        return if (matchResult != null) {
            matchResult.groupValues[1].trim()
        } else {
            "*"
        }
    }

    private fun nativeGetHopIPv4(inputText: String): String {
        val linePattern = "(?i).*exceeded.*".toRegex()
        val unreachablePattern = "(?i).*unreachable.*".toRegex()
        val fromPattern = "(?i)from\\s+([\\d.]+)".toRegex()
        //val fromPattern = ("(?i)from\\s+([^\\s]*)\\s").toRegex()
        val lines = inputText.lines().filter { linePattern.containsMatchIn(it) }
        for (line in lines) {
            val match1 = fromPattern.find(line)
            if (match1 != null) {
                return match1.groupValues[1].trim()
            }
        }
        //drop unreachable
        val notUnreachableLines =
            inputText.lines().filter { !unreachablePattern.containsMatchIn(it) }
        for (line in notUnreachableLines) {
            val match2 = fromPattern.find(line)
            if (match2 != null) {
                return match2.groupValues[1].trim()
            }
        }
        return "*"

    }

    private fun nativeGetHopIPv6(inputText: String): String {
        val linePattern = "(?i).*exceeded.*".toRegex()
        val unreachablePattern = "(?i).*unreachable.*".toRegex()
        //val fromPattern = ("(?i)from\\s+([0-9a-fA-F]{1,4}(?::[0-9a-fA-F]{0,4}){1,7}|::|([0-9a-fA-F]{1,4}:){1,6}:|:(:[0-9a-fA-F]{1,4}){1,6})").toRegex()
        val fromPattern = ("(?i)from\\s+([^\\s]*)\\s").toRegex()
        val lines = inputText.lines().filter { linePattern.containsMatchIn(it) }
        for (line in lines) {
            val match1 = fromPattern.find(line)
            if (match1 != null) {
                return match1.groupValues[1].trim()
            }
        }
        //drop unreachable
        val notUnreachableLines =
            inputText.lines().filter { !unreachablePattern.containsMatchIn(it) }
        for (line in notUnreachableLines) {
            val match2 = fromPattern.find(line)
            if (match2 != null) {
                var match2Result = match2.groupValues[1].trim()
                if (match2Result.isNotEmpty()) {
                    match2Result = match2Result.dropLast(1)
                }
                return match2Result
            }
        }
        return "*"

    }


    private suspend fun rho(challenge: BigInteger): BigInteger {
        val two = BigInteger.valueOf(2L)
        if (challenge.mod(two) == BigInteger.ZERO) {
            return two
        }

        var x = challenge
        var y = challenge
        val c = BigInteger.ONE
        var g = BigInteger.ONE

        while (g == BigInteger.ONE) {
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            x = x.multiply(x).add(c).mod(challenge)
            y = y.multiply(y).add(c).mod(challenge)
            y = y.multiply(y).add(c).mod(challenge)
            g = (x.subtract(y)).abs().gcd(challenge)
        }
        return g
    }

    //Pollard's Rho algorithm
    private suspend fun powHandler(challenge: BigInteger): MutableList<BigInteger> {
        require(challenge.signum() > 0 && challenge.bitLength() <= 128)
        val one = BigInteger.ONE
        if (challenge == one) {
            return mutableListOf()
        }
        val factor = rho(challenge)
        if (factor == challenge) {
            return listOf(challenge).toMutableList()
        }
        return (powHandler(factor) + powHandler(challenge.divide(factor))).sorted().toMutableList()
    }

    private fun geoSession(host: String, ip: String, token: String): GeoApiSession {
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .pingInterval(5, TimeUnit.SECONDS)
            .dns { InetAddress.getAllByName(ip).toList() }
            .build()
        val request = Request.Builder().url("wss://$host/v3/ipGeoWs")
            .header("User-Agent", "NextTrace v$NEXTTRACE_CORE_VERSION/linux/android NextTracerouteAndroid/" + BuildConfig.VERSION_NAME)
            .header("Authorization", "Bearer $token")
            .build()
        return GeoApiSession(client, request)
    }

    @Composable
    fun MainWSHandler(
        threadMutex: Mutex,
        tracerouteThreadsIntList: MutableList<Int>,
        scope: CoroutineScope,
        apiHostName: MutableState<String>,
        preferredAPIIp: MutableState<String>,
        apiToken: MutableState<String>,
        gridDataList: MutableList<MutableList<MutableList<MutableState<String>>>>,
        currentLanguage: MutableState<String>,
        traceMapThreadsMapList: MutableList<List<MutableMap<String, Any?>>>,
        insertion: MutableState<String>,
        isAPIFinished: MutableState<Boolean>
    ) {
        LaunchedEffect(Unit) {
            scope.launch(Dispatchers.Main.immediate) {
                val id = Random.nextInt(1, Int.MAX_VALUE)
                threadMutex.withLock { tracerouteThreadsIntList.add(id) }
                try {
                    val ready = withTimeoutOrNull(25_000) {
                        while (apiToken.value.isEmpty() || preferredAPIIp.value.isEmpty()) delay(100)
                        true
                    } == true
                    if (!ready) return@launch
                    geoSession(apiHostName.value, preferredAPIIp.value, apiToken.value).use { session ->
                        while (true) {
                            val targetIndex = gridDataList.indexOfFirst { it[0][1].value == insertion.value }
                                .let { if (it < 0) gridDataList.lastIndex else it }
                            val rows = gridDataList.take(targetIndex + 1)
                            for ((index, row) in rows.withIndex()) {
                                val ip = row[0][1].value
                                if (ip.isBlank() || ip == "*" || row[0][2].value.isNotEmpty()) continue
                                val reserved = reservedIPFilter(ip)
                                val data = if (reserved.isNotEmpty()) {
                                    parseGeoResponse(Gson().toJson(mapOf("ip" to ip, "asnumber" to reserved,
                                        "whois" to reserved)), ip)
                                } else session.lookup(ip)
                                // Cancellation is checked before touching shared state, even for cached replies.
                                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                                if (data == null) {
                                    row[0][2].value = "*"
                                    continue
                                }
                                val asNumber = data.get("asnumber").asString
                                val chinese = currentLanguage.value == "zh" ||
                                    (currentLanguage.value == "Default" && Locale.getDefault().language.startsWith("zh"))
                                row[0][2].value = when {
                                    reserved.isNotEmpty() -> reserved
                                    asNumber.isNotBlank() -> "AS$asNumber"
                                    else -> "*"
                                }
                                row[0][3].value = data.get("whois").asString.ifBlank { "*" }
                                val suffix = if (chinese) "" else "_en"
                                row[1][0].value = if (reserved.isNotEmpty()) reserved else
                                    listOf("country$suffix", "prov$suffix", "city$suffix", "domain")
                                        .map { data.get(it).asString }.filter { it.isNotBlank() }
                                        .joinToString(" ").ifBlank { "*" }
                                val geo = mutableMapOf<String, Any?>()
                                for (key in listOf("ip", "asnumber", "country", "country_en", "prov", "prov_en",
                                    "city", "city_en", "owner", "isp", "domain", "whois")) geo[key] = data.get(key).asString
                                geo["lat"] = data.get("lat").asDouble
                                geo["lng"] = data.get("lng").asDouble
                                geo["district"] = ""
                                geo["prefix"] = ""
                                geo["router"] = emptyMap<String, Any>()
                                geo["source"] = ""
                                traceMapThreadsMapList.add(listOf(mutableMapOf<String, Any?>(
                                    "Success" to true, "Address" to mapOf("IP" to ip, "zone" to ""),
                                    "Hostname" to "", "TTL" to index + 1, "Error" to null,
                                    "Geo" to geo, "Lang" to (if (chinese) "cn" else "en"), "MPLS" to null
                                )))
                            }
                            if (rows.all { it[0][1].value.isNotBlank() }) break
                            delay(200)
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Log.e("mainWSHandler", "Geo lookup failed", error)
                } finally {
                    withContext(NonCancellable + Dispatchers.Main.immediate) {
                        isAPIFinished.value = true
                        threadMutex.withLock {
                            tracerouteThreadsIntList.indices.forEach { index ->
                                if (tracerouteThreadsIntList[index] == id) tracerouteThreadsIntList[index] = 0
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun APIDNSHandler(
        threadMutex: Mutex, tracerouteThreadsIntList: MutableList<Int>, scope: CoroutineScope,
        tracerouteDNSServer: MutableState<String>,
        apiHostName: MutableState<String>, apiDNSName: MutableState<String>,
        preferredAPIIp: MutableState<String>, apiDNSList: MutableList<String>,
        apiToken: MutableState<String>, currentDOHServer: MutableState<String>,
        currentDNSMode: MutableState<String>
    ) {
        val dnsJobs = remember { mutableListOf(0) }
        val dnsMutex = remember { Mutex() }
        ResolveHandler(dnsMutex, dnsJobs, scope, apiDNSName.value, tracerouteDNSServer,
            multipleIps = apiDNSList, multipleIpStateMode = false,
            currentDOHServer = currentDOHServer, currentDNSMode = currentDNSMode)
        LaunchedEffect(Unit) {
            scope.launch(Dispatchers.Main.immediate) {
                val id = Random.nextInt(1, Int.MAX_VALUE)
                threadMutex.withLock { tracerouteThreadsIntList.add(id) }
                try {
                    val ready = withTimeoutOrNull(20_000) {
                        delay(100)
                        while (dnsJobs.any { it != 0 } || apiToken.value.isEmpty()) delay(100)
                        true
                    } == true
                    if (!ready) return@launch
                    for (ip in apiDNSList.toList()) {
                        geoSession(apiHostName.value, ip, apiToken.value).use { session ->
                            if (session.lookup("1.1.1.1") != null) {
                                preferredAPIIp.value = ip
                                return@launch
                            }
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Log.e("APIDNSHandler", "API connection failed", error)
                } finally {
                    withContext(NonCancellable + Dispatchers.Main.immediate) {
                        threadMutex.withLock {
                            tracerouteThreadsIntList.indices.forEach { index ->
                                if (tracerouteThreadsIntList[index] == id) tracerouteThreadsIntList[index] = 0
                            }
                        }
                    }
                }
            }
        }
    }


    @Composable
    fun APIPOWHandler(
        testAPIText: MutableState<String>,
        threadMutex: Mutex, tracerouteThreadsIntList: MutableList<Int>, scope: CoroutineScope,
        tracerouteDNSServer: MutableState<String>,
        apiHostNamePOW: MutableState<String>, apiDNSNamePOW: MutableState<String>,
        preferredAPIIpPOW: MutableState<String>, apiDNSListPOW: MutableList<String>,
        apiToken: MutableState<String>, currentDOHServer: MutableState<String>,
        currentDNSMode: MutableState<String>
    ) {
        LaunchedEffect(Unit) {
            scope.launch(Dispatchers.Main.immediate) {
                val uniqueID = Random.nextInt(1, Int.MAX_VALUE)
                threadMutex.withLock { tracerouteThreadsIntList.add(uniqueID) }
                try {
                    val host = apiHostNamePOW.value
                    val ips = resolveTraceAddresses(apiDNSNamePOW.value, currentDNSMode.value,
                        tracerouteDNSServer.value, currentDOHServer.value)
                    apiDNSListPOW.addAll(ips)
                    val result = withTimeoutOrNull(20_000) {
                        for (ip in ips.take(4)) {
                            val token = requestApiToken(host, ip)
                            if (token != null) return@withTimeoutOrNull ip to token
                        }
                        null
                    }
                    if (result != null) {
                        preferredAPIIpPOW.value = result.first
                        apiToken.value = result.second
                    } else {
                        testAPIText.value = "IP location service unavailable. Route tracing can continue."
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Log.e("APIPowHandler", "Location service unavailable", error)
                    testAPIText.value = "IP location service unavailable. Route tracing can continue."
                } finally {
                    withContext(NonCancellable + Dispatchers.Main.immediate) {
                        threadMutex.withLock {
                            tracerouteThreadsIntList.replaceAll { if (it == uniqueID) 0 else it }
                        }
                    }
                }
            }
        }
    }

    private suspend fun requestApiToken(host: String, ip: String): String? = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .dns { InetAddress.getAllByName(ip).toList() }
                .callTimeout(5, TimeUnit.SECONDS).build()
            val userAgent = "NextTrace v$NEXTTRACE_CORE_VERSION/linux/android NextTracerouteAndroid/" + BuildConfig.VERSION_NAME
            val request = Request.Builder().url("https://$host/v3/challenge/request_challenge")
                .header("User-Agent", userAgent).build()
            val challengeJson = client.newCall(request).execute().use { response ->
                check(response.isSuccessful)
                JsonParser.parseString(response.body.string()).asJsonObject
            }
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            val challengeObject = challengeJson.getAsJsonObject("challenge")
            val challenge = BigInteger(challengeObject.get("challenge").asString)
            val factors = withTimeoutOrNull(3_000) { powHandler(challenge) } ?: return@withContext null
            if (factors.size != 2) return@withContext null
            val answer = mapOf(
                "challenge" to mapOf(
                    "request_id" to challengeObject.get("request_id").asString,
                    "challenge" to challenge.toString()),
                "answer" to factors.map { it.toString() },
                "request_time" to challengeJson.get("request_time").asString)
            val body = Gson().toJson(answer).toRequestBody("application/json".toMediaType())
            val submit = Request.Builder().url("https://$host/v3/challenge/submit_answer")
                .header("User-Agent", userAgent).post(body).build()
            client.newCall(submit).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val token = JsonParser.parseString(response.body.string()).asJsonObject.get("token")
                token?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
                    ?.asString?.takeIf { it.isNotBlank() }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }

    @Composable
    fun InsertHandler(
        threadMutex: Mutex,
        tracerouteThreadsIntList: MutableList<Int>,
        insertion: MutableState<String>,
        insertErrorText: MutableState<String>,
        gridDataList: MutableList<MutableList<MutableList<MutableState<String>>>>,
        scope: CoroutineScope,
        tracerouteDNSServer: MutableState<String>,
        count: MutableState<String>,
        maxTTL: MutableIntState,
        timeout: MutableState<String>,
        multipleIps: MutableList<MutableState<String>>,
        context: Context,
        isDNSInProgress: MutableState<Boolean>, testAPIText: MutableState<String>,
        currentDOHServer: MutableState<String>,
        currentDNSMode: MutableState<String>,
        isTraceMapEnabled: MutableState<Boolean>,
        traceMapURL: MutableState<String>,
        apiHostName: MutableState<String>,
        preferredAPIIp: MutableState<String>,
        traceMapThreadsMapList: MutableList<List<MutableMap<String, Any?>>>,
        isSearchBarEnabled: MutableState<Boolean>,
        isAPIFinished: MutableState<Boolean>,
        apiToken: MutableState<String>,
        currentLanguage: MutableState<String>,
        apiHostNamePOW: MutableState<String>,
        apiDNSNamePOW: MutableState<String>,
        preferredAPIIpPOW: MutableState<String>,
        apiDNSListPOW: MutableList<String>,
        apiDNSList: MutableList<String>,
        apiDNSName: MutableState<String>,


        ) {

        val inputType = remember(insertion.value) { identifyInput(insertion.value) }
        //pingPlaceText.value= testNativePing()
        val lastHopCursor = remember(insertion.value) { mutableIntStateOf(114514) }
        val nativeStatus = remember(insertion.value, maxTTL.intValue) {
            MutableList(maxTTL.intValue) { mutableIntStateOf(0) }
        }
        val nativePingSemaphore = remember(insertion.value) { Semaphore(4) }
        if (inputType == IPV4_IDENTIFIER || inputType == IPV6_IDENTIFIER) {
            for ((index, item) in gridDataList.withIndex()) {
                LaunchedEffect(Unit) {
                    scope.launch(Dispatchers.Main.immediate) {
                        val uniqueID = Random.nextInt(1, Int.MAX_VALUE)
                        threadMutex.withLock { tracerouteThreadsIntList.add(uniqueID) }
                        try {
                            nativePingSemaphore.withPermit {
                                if (index > lastHopCursor.intValue) return@withPermit
                                val output = nativePingHandler(insertion.value, count.value,
                                    (index + 1).toString(), timeout.value)
                                val hop = if (inputType == IPV4_IDENTIFIER) nativeGetHopIPv4(output)
                                    else nativeGetHopIPv6(output)
                                if (sameTraceAddress(hop, insertion.value)) {
                                    lastHopCursor.intValue = minOf(index, lastHopCursor.intValue)
                                } else if (index < lastHopCursor.intValue) {
                                    item[0][0].value = (index + 1).toString()
                                    item[0][1].value = hop.ifBlank { "*" }
                                }
                            }
                        } finally {
                            withContext(NonCancellable + Dispatchers.Main.immediate) {
                                nativeStatus[index].intValue = 1
                                threadMutex.withLock {
                                    tracerouteThreadsIntList.replaceAll { if (it == uniqueID) 0 else it }
                                }
                            }
                        }
                    }
                }
            }


        } else if (inputType == HOSTNAME_IDENTIFIER) {
            val dnsThreadsList = remember { mutableListOf(0) }
            ResolveHandler(
                threadMutex = threadMutex, tracerouteThreadsIntList = dnsThreadsList,
                scope = scope,
                name = insertion.value, tracerouteDNSServer = tracerouteDNSServer,
                multipleIpsState = multipleIps, multipleIpStateMode = true,
                currentDNSMode = currentDNSMode,
                currentDOHServer = currentDOHServer, //testAPIText = testAPIText
            )
            LaunchedEffect(Unit) {
                scope.launch(Dispatchers.Main.immediate) {
                    val uniqueID = Random.nextInt(1, Int.MAX_VALUE)
                    threadMutex.withLock {
                        tracerouteThreadsIntList.add(uniqueID)
                    }
                    isDNSInProgress.value = true
                    // Let the resolver register, then wait for its bounded queries to finish.
                    delay(100)
                    while (dnsThreadsList.any { it != 0 }) delay(100)
                    isDNSInProgress.value = false
                    if (multipleIps.isEmpty()) {

                        insertErrorText.value =
                            "No DNS response yet! Check hostname and DNS setting!"
                        isSearchBarEnabled.value = true
                    }
                    threadMutex.withLock {
                        tracerouteThreadsIntList.indices.forEach { index ->
                            if (tracerouteThreadsIntList[index] == uniqueID) {
                                tracerouteThreadsIntList[index] = 0
                            }
                        }
                        tracerouteThreadsIntList.add(0)
                    }
                }
            }
        } else {
            Toast.makeText(context, "Invalid input! Wait 2 Seconds", Toast.LENGTH_LONG).show()
        }

        if (inputType == IPV4_IDENTIFIER || inputType == IPV6_IDENTIFIER) {

            LaunchedEffect(Unit) {
                scope.launch(Dispatchers.Main.immediate) {
                    val uniqueID = Random.nextInt(1, Int.MAX_VALUE)
                    threadMutex.withLock {
                        tracerouteThreadsIntList.add(uniqueID)
                    }
                    var nativeV4HaveZero = true
                    while (nativeV4HaveZero) {
                        delay(timeMillis = 500)
                        nativeV4HaveZero = false
                        for (i in nativeStatus) {
                            if (i.intValue == 0) {
                                nativeV4HaveZero = true
                            }
                        }
                        delay(timeMillis = 500)
                    }
                    if (lastHopCursor.intValue != 114514) {
                        gridDataList[lastHopCursor.intValue][0][1].value = insertion.value
                        gridDataList[lastHopCursor.intValue][0][0].value =
                            (lastHopCursor.intValue + 1).toString()
                    }
                    threadMutex.withLock {
                        tracerouteThreadsIntList.indices.forEach { index ->
                            if (tracerouteThreadsIntList[index] == uniqueID) {
                                tracerouteThreadsIntList[index] = 0
                            }
                        }
                        tracerouteThreadsIntList.add(0)
                    }
                }
            }

            //api handler
            APIPOWHandler(
                scope = scope, threadMutex = threadMutex,
                tracerouteThreadsIntList = tracerouteThreadsIntList,
                tracerouteDNSServer = tracerouteDNSServer,
                preferredAPIIpPOW = preferredAPIIpPOW, testAPIText = testAPIText,
                apiHostNamePOW = apiHostNamePOW,
                apiDNSNamePOW = apiDNSNamePOW,
                apiDNSListPOW = apiDNSListPOW,
                apiToken = apiToken,
                currentDOHServer = currentDOHServer,
                currentDNSMode = currentDNSMode
            )
            APIDNSHandler(
                scope = scope, threadMutex = threadMutex,
                tracerouteThreadsIntList = tracerouteThreadsIntList,
                tracerouteDNSServer = tracerouteDNSServer,
                preferredAPIIp = preferredAPIIp, //testAPIText = testText,
                apiHostName = apiHostName,
                apiDNSName = apiDNSName,
                apiDNSList = apiDNSList,
                apiToken = apiToken,
                currentDNSMode = currentDNSMode,
                currentDOHServer = currentDOHServer
            )


            MainWSHandler(
                threadMutex = threadMutex,
                tracerouteThreadsIntList = tracerouteThreadsIntList,
                scope = scope,
                apiHostName = apiHostName,
                preferredAPIIp = preferredAPIIp,
                apiToken = apiToken,
                gridDataList = gridDataList,
                currentLanguage = currentLanguage,
                traceMapThreadsMapList = traceMapThreadsMapList,
                isAPIFinished = isAPIFinished,
                insertion = insertion,
            )


            //tracemap handler
            if (isTraceMapEnabled.value) {
                LaunchedEffect(Unit) {
                    scope.launch(Dispatchers.IO) {
                        val uniqueID = Random.nextInt(1, Int.MAX_VALUE)
                        threadMutex.withLock { tracerouteThreadsIntList.add(uniqueID) }


                        while (true) {
                            delay(timeMillis = 500)
                            if (isAPIFinished.value) {
                                break
                            }
                        }
                        //ggbang
                        //testAPIText.value=traceMapThreadsMapList.size.toString()
                        if (traceMapThreadsMapList.isEmpty()) {
                            threadMutex.withLock {
                                tracerouteThreadsIntList.indices.forEach { index ->
                                    if (tracerouteThreadsIntList[index] == uniqueID) {
                                        tracerouteThreadsIntList[index] = 0
                                    }
                                }
                                tracerouteThreadsIntList.add(0)
                            }
                            return@launch
                        }
                        val tempList = mutableListOf<List<MutableMap<String, Any?>>>()
                        for (i in traceMapThreadsMapList) {
                            var isDuplicate = false
                            for (j in tempList) {
                                if (i[0]["Address"] == j[0]["Address"]) {
                                    isDuplicate = true
                                }
                            }
                            if (!isDuplicate) {
                                tempList.add(i)
                            }
                        }
                        tempList.sortBy { it[0]["TTL"] as? Int ?: 0 }
                        try {
                            val clientBuilder = OkHttpClient.Builder()
                                .connectTimeout(5, TimeUnit.SECONDS)
                                .readTimeout(5, TimeUnit.SECONDS)
                                .writeTimeout(5, TimeUnit.SECONDS)
                            if (preferredAPIIp.value.isNotBlank()) {
                                clientBuilder.dns {
                                    InetAddress.getAllByName(preferredAPIIp.value).toList()
                                }
                            }
                            val client = clientBuilder.build()
                            val submitTraceMapList = mapOf(
                                "Hops" to tempList,
                                "TraceMapUrl" to ""
                            )
                            val submitTraceMapJson =
                                GsonBuilder().serializeNulls().create().toJson(submitTraceMapList)
                            //testAPIText.value=submitTraceMapJson
                            val submitTraceMapType = "application/json".toMediaType()
                            val submitTraceMapBody =
                                submitTraceMapJson.toRequestBody(submitTraceMapType)
                            val submitTraceMapHeaders = mapOf(
                                "Host" to apiHostName.value,
                                "User-Agent" to "NextTrace v$NEXTTRACE_CORE_VERSION/linux/android NextTracerouteAndroid/" + BuildConfig.VERSION_NAME,
                                "Content-Length" to submitTraceMapBody.contentLength().toString(),
                                "Content-Type" to "application/json"
                            )
                            val submitTraceMapURL = "https://" + apiHostName.value + "/tracemap/api"
                            val submitTraceMapRequest = Request.Builder()
                                .url(submitTraceMapURL).post(submitTraceMapBody)
                            submitTraceMapHeaders.forEach { (key, value) ->
                                submitTraceMapRequest.addHeader(key, value)
                            }
                            val submitBuilder = submitTraceMapRequest.build()
                            val submitTraceMapCall = client.newCall(submitBuilder).execute()
                            val receiveTraceMapData = submitTraceMapCall.body.string()
                            //testAPIText.value= submitTraceMapCall.code.toString()
                            if (submitTraceMapCall.isSuccessful) {
                                if (receiveTraceMapData != "") {
                                    withContext(Dispatchers.Main.immediate) {
                                        traceMapURL.value = receiveTraceMapData
                                    }
                                    //testAPIText.value=traceMapURL.value
                                }
                            }
                            submitTraceMapCall.close()
                        } catch (e: Exception) {
                            Log.e("InsertHandler", "", e)
                            threadMutex.withLock {
                                tracerouteThreadsIntList.indices.forEach { index ->
                                    if (tracerouteThreadsIntList[index] == uniqueID) {
                                        tracerouteThreadsIntList[index] = 0
                                    }
                                }
                                tracerouteThreadsIntList.add(0)
                            }
                            return@launch
                        }


                        threadMutex.withLock {
                            tracerouteThreadsIntList.indices.forEach { index ->
                                if (tracerouteThreadsIntList[index] == uniqueID) {
                                    tracerouteThreadsIntList[index] = 0
                                }
                            }
                            tracerouteThreadsIntList.add(0)
                        }

                    }
                }

            }


        }

    }

    @Composable
    fun ResolveHandler(
        threadMutex: Mutex, tracerouteThreadsIntList: MutableList<Int>,
        scope: CoroutineScope,
        name: String, tracerouteDNSServer: MutableState<String>,
        multipleIpsState: MutableList<MutableState<String>> = mutableListOf(),
        multipleIps: MutableList<String> = mutableListOf(),
        multipleIpStateMode: Boolean, currentDOHServer: MutableState<String>,
        currentDNSMode: MutableState<String>
    ) {
        val dnsServer = tracerouteDNSServer.value
        val dnsMode = currentDNSMode.value
        val dohServer = currentDOHServer.value
        LaunchedEffect(name, dnsServer, dnsMode, dohServer) {
            scope.launch(Dispatchers.Main.immediate) {
                val uniqueID = Random.nextInt(1, Int.MAX_VALUE)
                threadMutex.withLock { tracerouteThreadsIntList.add(uniqueID) }
                try {
                    val type = identifyInput(name)
                    val addresses = if (type == IPV4_IDENTIFIER || type == IPV6_IDENTIFIER) {
                        listOf(name)
                    } else {
                        resolveTraceAddresses(name, dnsMode, dnsServer, dohServer)
                    }
                    if (multipleIpStateMode) {
                        val existing = multipleIpsState.map { it.value }.toSet()
                        multipleIpsState.addAll(addresses.filterNot { it in existing }.map { mutableStateOf(it) })
                    } else {
                        multipleIps.addAll(addresses.filterNot { it in multipleIps })
                    }
                } finally {
                    threadMutex.withLock {
                        tracerouteThreadsIntList.indices.forEach { index ->
                            if (tracerouteThreadsIntList[index] == uniqueID) tracerouteThreadsIntList[index] = 0
                        }
                    }
                }
            }
        }
    }


    @Composable
    fun EachHopHandler(
        threadMutex: Mutex, tracerouteThreadsIntList: MutableList<Int>,
        singleHopCursor: MutableList<MutableState<String>>,
        gridDataList: MutableList<MutableList<MutableList<MutableState<String>>>>,
        scope: CoroutineScope, tracerouteDNSServer: MutableState<String>,
        count: MutableState<String>, timeout: MutableState<String>,
        currentDOHServer: MutableState<String>, currentDNSMode: MutableState<String>
    ) {
        val workers = remember { Semaphore(4) }
        for ((index, item) in gridDataList.withIndex()) {
            val ip = item[0][1].value
            LaunchedEffect(ip) {
                if (identifyInput(ip) !in listOf(IPV4_IDENTIFIER, IPV6_IDENTIFIER)) return@LaunchedEffect
                scope.launch(Dispatchers.Main.immediate) {
                    val uniqueID = Random.nextInt(1, Int.MAX_VALUE)
                    threadMutex.withLock { tracerouteThreadsIntList.add(uniqueID) }
                    try {
                        singleHopCursor[index].value = ip
                        workers.withPermit {
                            val ping = nativePingHandler(ip, count.value, "", timeout.value)
                            item[2][1].value = extractRttValues(ping)
                            item[2][0].value = resolveTraceHostname(
                                ip, currentDNSMode.value, tracerouteDNSServer.value, currentDOHServer.value)
                        }
                    } finally {
                        withContext(NonCancellable + Dispatchers.Main.immediate) {
                            threadMutex.withLock {
                                tracerouteThreadsIntList.replaceAll { if (it == uniqueID) 0 else it }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun nativePingHandler(ip: String, count: String, ttl: String, timeout: String): String {
        val binary = when (identifyInput(ip)) {
            IPV4_IDENTIFIER -> "ping"
            IPV6_IDENTIFIER -> "ping6"
            else -> return ERROR_IDENTIFIER
        }
        val packets = count.toIntOrNull()?.coerceIn(1, 10) ?: 1
        val seconds = timeout.toIntOrNull()?.coerceIn(1, 10) ?: 1
        val command = mutableListOf(binary, "-n", "-c", packets.toString(), "-W", seconds.toString())
        if (ttl.isNotEmpty()) {
            command.addAll(listOf("-t", (ttl.toIntOrNull()?.coerceIn(1, 255) ?: 1).toString()))
        }
        command.add(ip)
        return try {
            kotlinx.coroutines.runInterruptible(Dispatchers.IO) {
                val process = ProcessBuilder(command).redirectErrorStream(true).start()
                try {
                    if (!process.waitFor((packets * (seconds + 1) + 3).toLong(), TimeUnit.SECONDS)) {
                        ERROR_IDENTIFIER
                    } else {
                        process.inputStream.bufferedReader().use { it.readText() }
                    }
                } finally {
                    process.destroyForcibly()
                    process.inputStream.close()
                    process.errorStream.close()
                    process.outputStream.close()
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e("nativePingHandler", "Ping unavailable", error)
            ERROR_IDENTIFIER
        }
    }

    fun identifyInput(input: String): String = identifyTraceTarget(input)

    private fun reservedIPFilter(input: String): String {
        val address: IPAddress = try {
            IPAddressString(input).toAddress()
        } catch (e: AddressStringException) {
            Log.e("reservedIPFilterHandler", "", e)
            return ""
        }
        val currentCIDRMap = if (identifyInput(input) == IPV4_IDENTIFIER) {
            RESERVED_IPV4_CIDR
        } else if (identifyInput(input) == IPV6_IDENTIFIER) {
            RESERVED_IPV6_CIDR
        } else {
            return ""
        }
        for ((cidr, name) in currentCIDRMap) {
            val subnet = try {
                IPAddressString(cidr).toAddress()
            } catch (e: AddressStringException) {
                Log.e("reservedIPFilterHandler", "", e)
                return ""
            }
            if (subnet.contains(address)) {
                return name
            }
        }

        return ""
    }


}
