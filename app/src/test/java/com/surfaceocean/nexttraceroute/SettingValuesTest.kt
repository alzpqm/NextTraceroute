package com.surfaceocean.nexttraceroute

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingValuesTest {
    @Test fun rejectsNonFiniteMalformedAndFractionalValues() {
        for (value in listOf(null, "", "NaN", "Infinity", "-Infinity", "1e400", "bad", "1.5",
            Double.NaN, Double.POSITIVE_INFINITY, true, emptyList<String>())) {
            assertEquals("Unexpected value: $value", 5, validIntegerSetting(value, 5, 1..10))
        }
    }

    @Test fun acceptsStringAndLegacyNumericIntegers() {
        for (value in listOf("1", " 1 ", "1.0", 1, 1.0)) {
            assertEquals(1, validIntegerSetting(value, 5, 1..10))
        }
        assertEquals(10, validIntegerSetting("10", 5, 1..10))
        assertEquals(255, validIntegerSetting("255", 30, 1..255))
    }

    @Test fun rejectsValuesOutsideEachSettingRange() {
        for (value in listOf("0", "-1", "11", Long.MAX_VALUE)) {
            assertEquals(5, validIntegerSetting(value, 5, 1..10))
        }
        assertEquals(30, validIntegerSetting(256, 30, 1..255))
    }
}
