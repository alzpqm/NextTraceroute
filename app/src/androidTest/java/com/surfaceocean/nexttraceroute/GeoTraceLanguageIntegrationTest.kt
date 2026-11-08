package com.surfaceocean.nexttraceroute

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

private fun captureFixtureResult(language: String) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    if (InstrumentationRegistry.getArguments().getString("geoScreenshots") != "true") return
    val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
    try {
        File(instrumentation.targetContext.cacheDir, "geo-fixture-$language.png").outputStream().use {
            check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it))
        }
    } finally { bitmap.recycle() }
}

class GeoFixtureRule : TestRule {
    val lookups = AtomicInteger()
    override fun apply(base: Statement, description: Description) = object : Statement() {
        override fun evaluate() {
            check(GeoLookupFixture.lookup == null)
            GeoLookupFixture.lookup = { ip ->
                lookups.incrementAndGet()
                val data = JsonParser.parseString("""{"country":"奥地利","country_en":"Austria","prov":"维也纳州","prov_en":"Vienna","city":"维也纳","city_en":"","domain":"example.net","asnumber":"64500"}""").asJsonObject
                data.addProperty("ip", ip)
                parseGeoResponse(data.toString(), ip)
            }
            try { base.evaluate() } finally { GeoLookupFixture.lookup = null }
        }
    }
}

class GeoTraceLanguageIntegrationTest {
    @get:Rule(order = 0) val language = DisplayLanguageRule()
    @get:Rule(order = 1) val settings = AuditSettingsRule(mapOf("currentLanguage" to "zh"))
    @get:Rule(order = 2) val fixture = GeoFixtureRule()
    @get:Rule(order = 3) val compose = createAndroidComposeRule<MainActivity>()

    private fun traceAndCheck() {
        compose.onNodeWithText("開始").assertExists()
        compose.onNode(hasSetTextAction()).performTextReplacement("127.0.0.1")
        compose.onNodeWithText("開始").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("複製結果").fetchSemanticsNodes().isNotEmpty() }
        assertTrue("MainWSHandler fixture was not used", fixture.lookups.get() > 0)
        compose.onNodeWithText("奧地利 维也纳州 维也纳 example.net").assertIsDisplayed()
        compose.onNodeWithText("AS64500").assertExists()
        captureFixtureResult("zh")
    }

    @Test fun savedChineseSettingSurvivesColdActivityRecreationAndRealTrace() {
        compose.activityRule.scenario.recreate()
        traceAndCheck()
    }

    @Test fun staleEnglishApiSettingCannotOverrideChineseInterface() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, "settings.json").writeText(Gson().toJson(mapOf(
            "currentLanguage" to "en", "isTraceMapEnabled" to false,
            "maxTraceTTL" to "1", "traceTimeout" to "1", "traceCount" to "1"
        )))
        compose.activityRule.scenario.recreate()
        traceAndCheck()
    }
}

class EnglishGeoTraceLanguageIntegrationTest {
    @get:Rule(order = 0) val language = DisplayLanguageRule("en-GB")
    @get:Rule(order = 1) val settings = AuditSettingsRule(mapOf("currentLanguage" to "zh"))
    @get:Rule(order = 2) val fixture = GeoFixtureRule()
    @get:Rule(order = 3) val compose = createAndroidComposeRule<MainActivity>()

    @Test fun englishTraceNeverAppendsUntranslatedChineseCity() {
        compose.activityRule.scenario.recreate()
        compose.onNode(hasSetTextAction()).performTextReplacement("127.0.0.1")
        compose.onNodeWithText("Start").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Copy result").fetchSemanticsNodes().isNotEmpty() }
        assertTrue(fixture.lookups.get() > 0)
        compose.onNodeWithText("Austria Vienna example.net").assertIsDisplayed()
        captureFixtureResult("en")
    }
}
