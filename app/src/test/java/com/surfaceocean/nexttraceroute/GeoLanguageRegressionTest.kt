package com.surfaceocean.nexttraceroute

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Test

class GeoLanguageRegressionTest {
    @Test fun missingEnglishCityMustNotAppendChineseToEnglishProvince() {
        val data = JsonParser.parseString("""{"country":"奥地利","country_en":"Austria","prov":"维也纳州","prov_en":"Vienna","city":"维也纳","city_en":"","domain":"example.net"}""").asJsonObject
        assertEquals("Austria Vienna example.net", formatGeoLocation(data, GeoLanguage.ENGLISH))
    }

    @Test fun missingChineseProvinceMustNotAppendEnglishToChineseCity() {
        val data = JsonParser.parseString("""{"country":"奥地利","country_en":"Austria","prov":"Untranslated district","prov_en":"Untranslated district","city":"維也納","city_en":"Vienna","domain":"example.net"}""").asJsonObject
        val original = data.toString()
        assertEquals("奧地利 維也納 example.net", formatGeoLocation(data, GeoLanguage.CHINESE))
        assertEquals(original, data.toString())
    }
}
