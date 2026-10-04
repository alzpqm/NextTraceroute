package com.surfaceocean.nexttraceroute

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import com.google.gson.JsonParser
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File
import java.util.UUID

/** Debug-only fixtures; keep the installed release and its data untouched. */
class AuditSettingsRule(private val overrides: Map<String, Any> = emptyMap()) : TestRule {
    override fun apply(base: Statement, description: Description) = object : Statement() {
        override fun evaluate() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            check(context.packageName.endsWith(".debug"))
            val file = File(context.filesDir, "settings.json")
            val backup = File(context.filesDir, "settings.audit-backup")
            val absent = File(context.filesDir, "settings.audit-was-absent")
            check(!backup.exists() && !absent.exists()) { "Restore the previous audit fixture first" }
            val original = if (file.exists()) file.readBytes() else null
            if (original == null) absent.writeText("") else backup.writeBytes(original)
            try {
                file.writeText(Gson().toJson(mapOf<String, Any>(
                    "isTraceMapEnabled" to false,
                    "maxTraceTTL" to "4",
                    "traceTimeout" to "1",
                    "traceCount" to "1"
                ) + overrides))
                base.evaluate()
            } finally {
                if (original == null) file.delete() else file.writeBytes(original)
                backup.delete()
                absent.delete()
            }
        }
    }
}

class TraceDeviceAuditTest {
    @get:Rule(order = 0) val settings = AuditSettingsRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    private fun start(target: String) {
        compose.onNode(hasSetTextAction()).performTextReplacement(target)
        compose.onNodeWithText(compose.activity.getString(R.string.action_run)).performClick()
    }

    private fun waitForCompletion() {
        compose.waitUntil(65_000) {
            compose.onAllNodesWithText(compose.activity.getString(R.string.action_copy_result)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasSetTextAction()).assertIsEnabled()
    }

    private fun open(page: String) {
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.menu_more)).performClick()
        compose.onNodeWithText(page).performClick()
    }

    @Test fun ipv6LoopbackCompletes() {
        start("::1")
        waitForCompletion()
        compose.onAllNodesWithText("::1").fetchSemanticsNodes().let { assertTrue(it.isNotEmpty()) }
    }

    @Test fun dnsFailureAllowsAnotherRun() {
        start("audit.invalid")
        compose.waitUntil(25_000) {
            compose.onAllNodesWithText(compose.activity.getString(R.string.dns_no_response))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasSetTextAction()).assertIsEnabled()
        start("127.0.0.1")
        waitForCompletion()
    }

    @Test fun rotationDuringTraceAllowsStopAndRecreation() {
        start("192.0.2.1")
        compose.activityRule.scenario.onActivity {
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText(compose.activity.getString(R.string.action_stop)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(compose.activity.getString(R.string.action_stop)).performClick()
        compose.waitUntil(20_000) {
            compose.onAllNodesWithText(compose.activity.getString(R.string.trace_stopped)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.activityRule.scenario.onActivity {
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        compose.activityRule.scenario.recreate()
        compose.onNode(hasSetTextAction()).assertIsEnabled()
    }

    @Test fun settingsSaveAndColdReloadRemainUsable() {
        open(compose.activity.getString(R.string.menu_settings))
        compose.onNodeWithText(compose.activity.getString(R.string.action_save)).performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.action_home)).performClick()
        compose.activityRule.scenario.recreate()
        open(compose.activity.getString(R.string.menu_settings))
        compose.onNodeWithText(compose.activity.getString(R.string.max_hops)).assertExists()
        compose.onNodeWithText(compose.activity.getString(R.string.action_save)).assertIsEnabled()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.action_home)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.action_run)).assertIsEnabled()
    }

    @Test fun historyDetailsRoundTrip() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.databaseBuilder(context, AppDatabase::class.java, "app-database").build()
        val record = HistoryData(uuid = UUID.randomUUID().toString(),
            ip = "192.0.2.123", domain = "audit.invalid", history = "Synthetic audit result")
        try {
            runBlocking(Dispatchers.IO) { db.historyDao().insertHistory(record) }
            open(compose.activity.getString(R.string.menu_history))
            compose.waitUntil(15_000) {
                compose.onAllNodesWithText("audit.invalid").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onAllNodesWithContentDescription(compose.activity.getString(R.string.action_info))[0].performClick()
            compose.onNodeWithText(record.uuid).assertExists()
            compose.onNodeWithText("Synthetic audit result", substring = true).assertExists()
            compose.onNodeWithText(compose.activity.getString(R.string.action_close)).performClick()
            compose.onNodeWithContentDescription(compose.activity.getString(R.string.action_home)).performClick()
            compose.onNodeWithText(compose.activity.getString(R.string.action_run)).assertIsEnabled()
        } finally {
            runBlocking(Dispatchers.IO) { db.historyDao().deleteByUuid(record.uuid) }
            db.close()
        }
    }
}

class OfflineGeoAuditTest {
    @get:Rule(order = 0) val settings = AuditSettingsRule(mapOf(
        "apiDNSNamePOW" to "127.0.0.1", "apiDNSName" to "127.0.0.1"
    ))
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Test fun localAddressClassificationDoesNotRequireBackend() {
        compose.onNode(hasSetTextAction()).performTextReplacement("127.0.0.1")
        compose.onNodeWithText(compose.activity.getString(R.string.action_run)).performClick()
        compose.waitUntil(40_000) {
            compose.onAllNodesWithText(compose.activity.getString(R.string.action_copy_result)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasSetTextAction()).assertIsEnabled()
        assertTrue("Loopback classification is missing when the backend is unavailable",
            compose.onAllNodesWithText("RFC1122").fetchSemanticsNodes().isNotEmpty())
    }
}

class StoredSettingsAuditTest {
    @get:Rule(order = 0) val settings = AuditSettingsRule(mapOf(
        "traceTimeout" to "NaN", "traceCount" to "Infinity", "maxTraceTTL" to "1e100"
    ))
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Test fun malformedStoredNumberDoesNotCrashSettings() {
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.menu_more)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.menu_settings)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.action_save)).assertIsEnabled()
        compose.onNodeWithText(compose.activity.getString(R.string.action_save)).performClick()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val saved = JsonParser.parseString(File(context.filesDir, "settings.json").readText()).asJsonObject
        assertEquals("1", saved["traceTimeout"].asString)
        assertEquals("5", saved["traceCount"].asString)
        assertEquals("30", saved["maxTraceTTL"].asString)
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.action_home)).performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText(compose.activity.getString(R.string.action_run)).assertIsEnabled()
    }
}
