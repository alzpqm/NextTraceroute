package com.surfaceocean.nexttraceroute

import inet.ipaddr.IPAddressString
import java.net.IDN
import java.util.Locale

/** Extract only the authority: path/query length or escaping cannot affect the host. */
fun normalizeTargetInput(input: String): String {
    val value = input.trim()
    if (value.isEmpty()) return ""

    val schemeEnd = value.indexOf(':')
    val hasScheme = schemeEnd in 1..32 && value.startsWith("://", schemeEnd) &&
        value[0].isLetter() && value.take(schemeEnd).all {
            it.isLetterOrDigit() || it in "+-."
        }
    val start = when {
        hasScheme -> schemeEnd + 3
        value.startsWith("//") -> 2
        else -> 0
    }
    val end = value.indexOfAny(charArrayOf('/', '?', '#'), start).let {
        if (it < 0) value.length else it
    }
    val authority = value.substring(start, end).substringAfterLast('@')
    val host = if (authority.startsWith('[')) {
        val close = authority.indexOf(']')
        if (close < 0 || !validPortSuffix(authority.substring(close + 1))) return ""
        authority.substring(1, close)
    } else if (authority.count { it == ':' } == 1) {
        if (!validPortSuffix(authority.substring(authority.indexOf(':')))) return ""
        authority.substringBefore(':')
    } else {
        authority
    }
    if (host.contains(':')) return host
    // Reject excessive input before passing it to IDN or any address parser.
    if (host.length > 254) return ""
    return runCatching {
        IDN.toASCII(host.removeSuffix("."), IDN.USE_STD3_ASCII_RULES)
            .lowercase(Locale.ROOT)
    }.getOrDefault("")
}

private fun validPortSuffix(suffix: String): Boolean = suffix.isEmpty() ||
    (suffix.startsWith(':') && suffix.length in 2..6 &&
        suffix.drop(1).all { it in '0'..'9' } &&
        suffix.drop(1).toIntOrNull() in 1..65535)

/** Bounded, non-recursive validation; never performs DNS resolution. */
fun identifyTraceTarget(input: String): String {
    if (input.isEmpty() || input.length > 253) return ERROR_IDENTIFIER
    if (':' in input) {
        if (input.length > 45 || input.any { it !in "0123456789abcdefABCDEF:." }) {
            return ERROR_IDENTIFIER
        }
        return if (runCatching { IPAddressString(input).toAddress()?.isIPv6 }.getOrNull() == true) {
            IPV6_IDENTIFIER
        } else ERROR_IDENTIFIER
    }
    val labels = input.split('.')
    if (input.all { it in '0'..'9' || it == '.' }) {
        return if (labels.size == 4 && labels.all {
                it.isNotEmpty() && it.length <= 3 &&
                    (it.length == 1 || it[0] != '0') && it.toIntOrNull() in 0..255
            } && input != "255.255.255.255") IPV4_IDENTIFIER else ERROR_IDENTIFIER
    }
    val validName = labels.all { label ->
        label.length in 1..63 && label.first() != '-' && label.last() != '-' &&
            label.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '-' }
    } && labels.last().any { it in 'a'..'z' || it in 'A'..'Z' }
    return if (validName) HOSTNAME_IDENTIFIER else ERROR_IDENTIFIER
}

/** Compare IPv6 compressed and expanded forms without resolving hostnames. */
internal fun sameTraceAddress(first: String, second: String): Boolean {
    if (identifyTraceTarget(first) !in listOf(IPV4_IDENTIFIER, IPV6_IDENTIFIER) ||
        identifyTraceTarget(second) !in listOf(IPV4_IDENTIFIER, IPV6_IDENTIFIER)) return false
    return IPAddressString(first).toAddress() == IPAddressString(second).toAddress()
}

/** A paste can be shortened immediately without rewriting partial URLs while typing. */
fun normalizePastedTarget(previous: String, input: String): String {
    val singleLine = input.filterNot { it == '\n' || it == '\r' }
    val prefix = previous.commonPrefixWith(singleLine).length
    val suffix = previous.drop(prefix).commonSuffixWith(singleLine.drop(prefix)).length
    val insertedLength = singleLine.length - prefix - suffix
    if (insertedLength > 1 &&
        (singleLine.contains("://") || singleLine.startsWith("//"))) {
        val host = normalizeTargetInput(singleLine)
        if (identifyTraceTarget(host) != ERROR_IDENTIFIER) return host
    }
    return singleLine
}
