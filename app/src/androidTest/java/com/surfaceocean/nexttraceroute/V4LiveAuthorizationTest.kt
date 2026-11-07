package com.surfaceocean.nexttraceroute

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Explicit opt-in: never issues a token or interacts with the verification widget. */
class V4LiveAuthorizationTest {
    @Test fun importedAuthorizationSupportsLiveGeoLookup() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("v4Live") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val grant = V4TokenStore(context).load()
        assertNotNull("Complete official verification in the Debug app first", grant)
        assertTrue("Imported authorisation must not be expired", grant!!.usable())
        val api = NextTraceV4Client()
        val verified = api.verify(grant)
        val geo = api.lookup("1.1.1.1", verified)
        assertNotNull("The live v4 response must match the requested address", geo)
        assertEquals("1.1.1.1", geo!!.get("ip").asString)
    }
}
