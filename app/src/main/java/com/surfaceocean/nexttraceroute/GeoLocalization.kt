package com.surfaceocean.nexttraceroute

import com.google.gson.JsonObject
import java.util.Locale

internal enum class GeoLanguage { CHINESE, ENGLISH }

/** Resolve once per trace, using the app's locale rather than the device's locale. */
internal fun resolveGeoLanguage(displayLanguageTag: String): GeoLanguage =
    if (Locale.forLanguageTag(displayLanguageTag).language == "en") GeoLanguage.ENGLISH else GeoLanguage.CHINESE

private data class CountryName(val code: String, val chinese: String, val english: String)
private val chineseLocale = Locale.forLanguageTag("zh-Hant-TW")
private val spaces = Regex("\\s+")
private fun nameKey(value: String) = value.trim().replace(spaces, " ").lowercase(Locale.ROOT)
private fun hasHan(value: String) = value.codePoints().anyMatch { Character.UnicodeScript.of(it) == Character.UnicodeScript.HAN }

// These are exact geographic aliases, never substring replacements in technical names.
private val regionNames = listOf(
    CountryName("CN", "中國", "China") to listOf("中国", "中國", "China"),
    CountryName("HK", "香港", "Hong Kong") to listOf("香港", "Hong Kong", "Hongkong", "香港特别行政区", "香港特別行政區"),
    CountryName("MO", "澳門", "Macao") to listOf("澳门", "澳門", "Macao", "Macau", "澳门特别行政区", "澳門特別行政區"),
    CountryName("SG", "新加坡", "Singapore") to listOf("新加坡", "Singapore", "Singapore City")
)

private val countryNames: Map<String, CountryName> by lazy {
    buildMap {
        for (code in Locale.getISOCountries()) {
            val locale = Locale.Builder().setRegion(code).build()
            val override = regionNames.firstOrNull { it.first.code == code }?.first
            val name = override ?: CountryName(code, locale.getDisplayCountry(chineseLocale), locale.getDisplayCountry(Locale.UK))
            for (alias in listOf(code, runCatching { locale.isO3Country }.getOrDefault(""), locale.getDisplayCountry(Locale.UK),
                locale.getDisplayCountry(Locale.SIMPLIFIED_CHINESE), locale.getDisplayCountry(chineseLocale))) {
                if (alias.isNotBlank()) put(nameKey(alias), name)
            }
        }
        for ((name, aliases) in regionNames) for (alias in aliases) put(nameKey(alias), name)
    }
}

/** Shared by v3 and v4. Raw response fields are retained separately for the map. */
internal fun formatGeoLocation(data: JsonObject, language: GeoLanguage): String {
    fun text(key: String) = data.get(key)?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty().takeUnless { it == "*" }.orEmpty()
    fun candidates(key: String): List<String> = (if (language == GeoLanguage.CHINESE)
        listOf(text(key), text("${key}_en")) else listOf(text("${key}_en"), text(key)))
        .filter { it.isNotBlank() }.distinct()
    fun preferred(values: List<String>): String = values.firstOrNull {
        hasHan(it) == (language == GeoLanguage.CHINESE)
    }.orEmpty()
    fun CountryName.display() = if (language == GeoLanguage.CHINESE) chinese else english

    val countryCandidates = candidates("country")
    val rawCountry = preferred(countryCandidates).ifBlank {
        countryCandidates.firstOrNull { nameKey(it) in countryNames }.orEmpty()
    }
    val country = countryNames[nameKey(rawCountry)]
    fun place(key: String): String {
        val values = candidates(key)
        val raw = preferred(values)
        // A city called Singapore in another country is not Singapore the country.
        val alias = regionNames.firstOrNull { (name, aliases) ->
            name.code != "CN" &&
                (country?.code == name.code || (country?.code == "CN" && name.code in listOf("HK", "MO"))) &&
                aliases.any { alias -> values.any { nameKey(alias) == nameKey(it) } }
        }?.first
        return alias?.display() ?: raw
    }
    val places = listOf(country?.display() ?: rawCountry, place("prov"), place("city"))
        .filter { it.isNotBlank() }.distinctBy(::nameKey)
    // Domain names are identifiers; do not translate or alter them.
    return (places + text("domain")).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "*" }
}
