package com.surfaceocean.nexttraceroute

import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class GeoLocalizationTest {
    private fun geo(text: String) = JsonParser.parseString(text).asJsonObject

    @Test fun screenshotPlacesUseChineseEvenWhenApiFieldsContainEnglish() {
        val samples = listOf(
            """{"country":"中国","city":"Macau","domain":"example.net"}""" to "中國 澳門 example.net",
            """{"country":"China","prov":"Hong Kong","domain":"example.net"}""" to "中國 香港 example.net",
            """{"country":"Singapore","city":"Singapore","domain":"example.net"}""" to "新加坡 example.net"
        )
        for ((json, expected) in samples) assertEquals(expected, formatGeoLocation(geo(json), GeoLanguage.CHINESE))
    }

    @Test fun chineseOnlyKnownPlacesAreAvailableInEnglish() {
        assertEquals("China Macao example.net", formatGeoLocation(geo(
            """{"country":"中国","city":"澳门","domain":"example.net"}"""), GeoLanguage.ENGLISH))
        assertEquals("China Hong Kong", formatGeoLocation(geo(
            """{"country":"中國","prov":"香港","city":"香港"}"""), GeoLanguage.ENGLISH))
    }

    @Test fun targetLanguageWinsEvenWhenBilingualValuesAreInWrongFields() {
        val data = geo("""{"country":"China","country_en":"中国","prov":"Hong Kong","prov_en":"香港","city":"Test City","city_en":"測試城"}""")
        assertEquals("中國 香港 測試城", formatGeoLocation(data, GeoLanguage.CHINESE))
        assertEquals("China Hong Kong Test City", formatGeoLocation(data, GeoLanguage.ENGLISH))
    }

    @Test fun countryNamesUseExplicitLocaleAndAcceptCodes() {
        for (name in listOf("US", "USA", "United States", "美国", "美國")) {
            assertEquals("美國", formatGeoLocation(geo("""{"country":"$name"}"""), GeoLanguage.CHINESE))
        }
    }

    @Test fun aliasesRespectCountryContextAndNeverChangeTechnicalNamesOrRawJson() {
        val data = geo("""{"country":"US","city":"Singapore","domain":"HongKong.Example.net","owner":"Macau","whois":"Singapore"}""")
        val before = data.toString()
        assertEquals("美國 HongKong.Example.net", formatGeoLocation(data, GeoLanguage.CHINESE))
        assertEquals(before, data.toString())
    }

    @Test fun unavailableTranslationsDoNotIntroduceAnotherLanguage() {
        val data = geo("""{"country":"Unlisted country","prov":"Unlisted province","city":"Unlisted city"}""")
        val original = data.toString()
        assertEquals("Unlisted country Unlisted province Unlisted city", formatGeoLocation(data, GeoLanguage.ENGLISH))
        assertEquals("*", formatGeoLocation(data, GeoLanguage.CHINESE))
        assertEquals(original, data.toString())
        assertEquals("*", formatGeoLocation(geo("""{"country":null,"city":"*"}"""), GeoLanguage.CHINESE))
    }

    @Test fun languageFollowsAppIndependentlyOfSystemLocale() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.UK)
            assertEquals(GeoLanguage.CHINESE, resolveGeoLanguage("zh-Hant"))
            Locale.setDefault(Locale.TRADITIONAL_CHINESE)
            assertEquals(GeoLanguage.ENGLISH, resolveGeoLanguage("en-GB"))
            assertEquals(GeoLanguage.CHINESE, resolveGeoLanguage("invalid"))
        } finally { Locale.setDefault(original) }
    }
}
