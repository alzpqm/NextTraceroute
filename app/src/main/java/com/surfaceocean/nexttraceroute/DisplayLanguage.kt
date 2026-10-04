package com.surfaceocean.nexttraceroute

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

internal const val DISPLAY_LANGUAGE_PREFS = "display-language"
internal const val DISPLAY_LANGUAGE_KEY = "language"
internal val displayLanguages = listOf("zh-Hant", "en-GB")

internal fun displayLanguage(context: Context): String =
    context.getSharedPreferences(DISPLAY_LANGUAGE_PREFS, Context.MODE_PRIVATE)
        .getString(DISPLAY_LANGUAGE_KEY, null)?.takeIf { it in displayLanguages } ?: displayLanguages.first()

internal fun localizedContext(context: Context): Context {
    val locale = Locale.forLanguageTag(displayLanguage(context))
    val configuration = Configuration(context.resources.configuration).apply {
        setLocales(LocaleList(locale))
        setLayoutDirection(locale)
    }
    return context.createConfigurationContext(configuration)
}
