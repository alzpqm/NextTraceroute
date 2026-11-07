package com.surfaceocean.nexttraceroute

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class GeoLocalizationDeviceTest {
    @get:Rule val compose = createComposeRule()

    private val samples = listOf(
        """{"ip":"192.0.2.1","country":"中国","city":"Macau","domain":"Example.net"}""",
        """{"ip":"192.0.2.1","country":"China","prov":"Hong Kong","domain":"Example.net"}""",
        """{"ip":"192.0.2.1","country":"Singapore","city":"Singapore","domain":"Example.net"}"""
    )

    private fun checkRows(appLanguage: String, systemLocale: Locale, expected: List<String>) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".debug"))
        val prefs = context.getSharedPreferences(DISPLAY_LANGUAGE_PREFS, Context.MODE_PRIVATE)
        val oldLanguage = prefs.getString(DISPLAY_LANGUAGE_KEY, null)
        val oldLocale = Locale.getDefault()
        try {
            Locale.setDefault(systemLocale)
            prefs.edit().putString(DISPLAY_LANGUAGE_KEY, appLanguage).commit()
            val resources = localizedContext(context).resources
            val language = resolveGeoLanguage("Default", resources.configuration.locales[0].toLanguageTag())
            assertEquals(if (appLanguage == "en-GB") "Follow display language" else "跟隨介面", resources.getString(R.string.language_system))
            val rows = samples.map { formatGeoLocation(parseGeoResponse(it, "192.0.2.1")!!, language) }
            assertEquals(expected, rows)
            var copied = ""
            compose.setContent {
                Column {
                    for (row in rows) TraceResultRow(
                        rowIndex = 1, items = listOf(row),
                        textColor = Color.Black, numberColor = Color.Black,
                        asColor = Color.Black, pingColor = Color.Black,
                        onLongPress = { copied = it }, onTap = { _, _ -> }
                    )
                }
            }
            for (row in expected) compose.onNodeWithText(row).assertIsDisplayed()
            compose.onNodeWithText(expected[0]).performTouchInput { longClick() }
            compose.runOnIdle { assertEquals(expected[0], copied) }
        } finally {
            Locale.setDefault(oldLocale)
            prefs.edit().apply {
                if (oldLanguage == null) remove(DISPLAY_LANGUAGE_KEY) else putString(DISPLAY_LANGUAGE_KEY, oldLanguage)
            }.commit()
        }
    }

    @Test fun chineseRowsAndCopiedTextFollowAppOnEnglishDevice() = checkRows("zh-Hant", Locale.UK,
        listOf("中國 澳門 Example.net", "中國 香港 Example.net", "新加坡 Example.net"))

    @Test fun englishRowsFollowAppOnChineseDevice() = checkRows("en-GB", Locale.TRADITIONAL_CHINESE,
        listOf("China Macao Example.net", "China Hong Kong Example.net", "Singapore Example.net"))
}
