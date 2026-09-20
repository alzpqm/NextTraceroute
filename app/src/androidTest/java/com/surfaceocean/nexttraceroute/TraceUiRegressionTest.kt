package com.surfaceocean.nexttraceroute

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performImeAction
import org.junit.Rule
import org.junit.Test

class TraceUiRegressionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun longUrlPasteAndInvalidInputRemainResponsive() {
        val host = "ipv4-c004-hkg001-smartone-isp.1.oca.nflxvideo.net"
        val input = compose.onNode(hasSetTextAction())
        input.performTextReplacement("https://$host/speedtest?test=" + "x".repeat(100_000))
        input.assertTextEquals(host)
        input.performTextReplacement("https://example.com/a b?q=%")
        input.assertTextEquals("example.com")
        input.performTextReplacement("a".repeat(64) + ".com")
        compose.onNodeWithText("Run").performClick()
        compose.onNodeWithText("Enter a valid hostname, IPv4, IPv6 address or URL.").assertExists()
        input.assertIsEnabled()
    }

    @Test fun repeatedStopRestartAndActivityRecreation() {
        repeat(4) {
            compose.onNode(hasSetTextAction()).performTextReplacement("127.0.0.1")
            compose.onNodeWithText("Run").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Stop").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Stop").performClick()
            compose.waitUntil(20_000) { compose.onAllNodesWithText("Trace stopped.").fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(hasSetTextAction()).assertIsEnabled()
        }
        compose.activityRule.scenario.recreate()
        compose.onNode(hasSetTextAction()).assertIsEnabled()
        compose.onNodeWithText("Run").assertIsEnabled()
    }

    @Test fun loopbackTraceCompletesAndCanRunAgain() {
        compose.onNode(hasSetTextAction()).performTextReplacement("127.0.0.1")
        compose.onNodeWithText("Run").performClick()
        compose.waitUntil(60_000) { compose.onAllNodesWithText("Copy result").fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasSetTextAction()).assertIsEnabled()
        compose.onNodeWithText("Run").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Stop").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Stop").performClick()
        compose.waitUntil(20_000) { compose.onAllNodesWithText("Trace stopped.").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun providedCdnUrlResolvesAndClearAndImeWork() {
        val host = "ipv4-c004-hkg001-smartone-isp.1.oca.nflxvideo.net"
        compose.onNode(hasSetTextAction()).performTextReplacement("https://$host/speedtest?c=hk&n=17924")
        compose.onNodeWithContentDescription("Clear target").performClick()
        compose.onNode(hasSetTextAction()).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        compose.onNode(hasSetTextAction()).performTextReplacement("https://$host/speedtest?query=" + "x".repeat(1000))
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.waitUntil(40_000) {
            compose.onAllNodesWithText("Choose an address").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Stop").performClick()
        compose.waitUntil(20_000) { compose.onAllNodesWithText("Trace stopped.").fetchSemanticsNodes().isNotEmpty() }
    }
}
