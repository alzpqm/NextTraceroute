package com.surfaceocean.nexttraceroute

import com.google.gson.JsonObject

/** Instrumentation-only data source; absent from the release implementation. */
internal object GeoLookupFixture {
    var lookup: ((String) -> JsonObject?)? = null
}

internal fun geoLookupFixture(ip: String): JsonObject? = GeoLookupFixture.lookup?.invoke(ip)
internal fun hasGeoLookupFixture() = GeoLookupFixture.lookup != null
