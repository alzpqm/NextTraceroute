package com.surfaceocean.nexttraceroute

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.surfaceocean.nexttraceroute.ui.theme.NextTracerouteTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TraceResultLayoutTest {
    @get:Rule val compose = createComposeRule()

    private val ipv6 = "2001:db8:aaaa:bbbb:cccc:dddd:eeee:ffff"
    private val asNumber = "AS4294967295"

    private fun showRow(
        widthDp: Int,
        fontScale: Float,
        rowIndex: Int = 0,
        items: List<String> = listOf("1", ipv6, asNumber, "*"),
        onLongPress: (String) -> Unit = {},
        onTap: (Int, String) -> Unit = { _, _ -> }
    ) {
        val density = compose.density.density
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                NextTracerouteTheme {
                    Box(Modifier.width(widthDp.dp)) {
                        TraceResultRow(
                            rowIndex = rowIndex,
                            items = items,
                            textColor = Color.Black,
                            numberColor = Color.Black,
                            asColor = Color.Black,
                            pingColor = Color.Black,
                            onLongPress = onLongPress,
                            onTap = onTap
                        )
                    }
                }
            }
        }
    }

    private fun textLayout(text: String): TextLayoutResult {
        val node = compose.onNodeWithText(text).assertIsDisplayed().fetchSemanticsNode()
        val results = mutableListOf<TextLayoutResult>()
        assertTrue(node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results))
        return results.single()
    }

    private fun assertFullyVisible(layout: TextLayoutResult) {
        assertFalse("Text exceeds its available height", layout.didOverflowHeight)
        assertEquals(layout.layoutInput.text.length, layout.getLineEnd(layout.lineCount - 1))
        repeat(layout.lineCount) { line ->
            assertFalse("Text is ellipsized", layout.isLineEllipsized(line))
            assertTrue("Text extends beyond its width", layout.getLineRight(line) <= layout.size.width + 1f)
            assertTrue("Text extends before its left edge", layout.getLineLeft(line) >= -1f)
        }
    }

    private fun assertAddressAndAsFit() {
        val address = textLayout(ipv6)
        val asLayout = textLayout(asNumber)
        assertEquals(ipv6, address.layoutInput.text.text)
        assertFullyVisible(address)
        assertEquals("AS number should remain on one line when it fits the card", 1, asLayout.lineCount)
        assertFullyVisible(asLayout)
        val addressBounds = compose.onNodeWithText(ipv6).fetchSemanticsNode().boundsInRoot
        val asBounds = compose.onNodeWithText(asNumber).fetchSemanticsNode().boundsInRoot
        assertFalse("Address and AS must not overlap", addressBounds.overlaps(asBounds))
    }

    @Test fun longIpv6DoesNotSqueezeAsAndRetainsGestures() {
        var tapped: Pair<Int, String>? = null
        var copied: String? = null
        showRow(280, 1f, onLongPress = { copied = it }, onTap = { index, value -> tapped = index to value })
        assertAddressAndAsFit()
        compose.onNodeWithText(ipv6).performTouchInput { click() }
        compose.runOnIdle { assertEquals(1 to ipv6, tapped) }
        compose.onNodeWithText(asNumber).performTouchInput { longClick() }
        compose.runOnIdle { assertEquals(asNumber, copied) }
    }

    @Test fun longIpv6AndAsRemainReadableWithLargeText() {
        showRow(280, 1.8f, items = listOf("1", ipv6, asNumber, "peer.example"))
        assertAddressAndAsFit()
        assertFullyVisible(textLayout("peer.example"))
    }

    @Test fun shortIpv4AndAsStayOnOneLineWhenTheyFit() {
        val ipv4 = "192.0.2.1"
        val shortAs = "AS64512"
        showRow(360, 1f, items = listOf("2", ipv4, shortAs, "*"))
        assertEquals(1, textLayout(ipv4).lineCount)
        assertEquals(1, textLayout(shortAs).lineCount)
        val ipBounds = compose.onNodeWithText(ipv4).fetchSemanticsNode().boundsInRoot
        val asBounds = compose.onNodeWithText(shortAs).fetchSemanticsNode().boundsInRoot
        assertEquals(ipBounds.center.y, asBounds.center.y, 1f)
    }

    @Test fun longHostDoesNotSqueezeLatency() {
        val host = "a-long-reverse-hostname.example.test"
        val latency = "4.280/7.983/15.606/4.641 ms"
        showRow(280, 1f, rowIndex = 2, items = listOf(host, latency))
        assertFullyVisible(textLayout(host))
        val latencyLayout = textLayout(latency)
        assertEquals(1, latencyLayout.lineCount)
        assertFullyVisible(latencyLayout)
    }
}
