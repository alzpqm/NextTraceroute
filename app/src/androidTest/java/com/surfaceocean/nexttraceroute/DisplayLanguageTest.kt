package com.surfaceocean.nexttraceroute

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class DisplayLanguageRule : TestRule {
    override fun apply(base: Statement, description: Description) = object : Statement() {
        override fun evaluate() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            check(context.packageName.endsWith(".debug"))
            val preferences = context.getSharedPreferences(DISPLAY_LANGUAGE_PREFS, Context.MODE_PRIVATE)
            val original = preferences.getString(DISPLAY_LANGUAGE_KEY, null)
            preferences.edit().putString(DISPLAY_LANGUAGE_KEY, "zh-Hant").commit()
            try {
                base.evaluate()
            } finally {
                preferences.edit().apply {
                    if (original == null) remove(DISPLAY_LANGUAGE_KEY) else putString(DISPLAY_LANGUAGE_KEY, original)
                }.commit()
            }
        }
    }
}

class DisplayLanguageTest {
    @get:Rule(order = 0) val language = DisplayLanguageRule()
    @get:Rule(order = 1) val settings = AuditSettingsRule()
    @get:Rule(order = 2) val compose = createAndroidComposeRule<MainActivity>()

    @Test fun chineseFirstAndBritishEnglishPersistAcrossRecreation() {
        compose.onNodeWithText("開始").assertExists()
        compose.onNodeWithContentDescription("更多選項").performClick()
        compose.onNodeWithText("設定").performClick()
        compose.onNodeWithText("介面語言").assertExists()
        compose.onNodeWithText("中文").performClick()
        compose.onNodeWithText("英文").performClick()
        compose.onNodeWithText("儲存").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Start").fetchSemanticsNodes().isNotEmpty() }
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Start").assertExists()
        val locale = compose.activity.resources.configuration.locales[0]
        assertEquals("en", locale.language)
        assertEquals("GB", locale.country)
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Display language").assertExists()
        compose.onNodeWithText("English").performClick()
        compose.onNodeWithText("Chinese").performClick()
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("開始").fetchSemanticsNodes().isNotEmpty() }
        assertEquals("zh-Hant", displayLanguage(compose.activity))
    }
}
