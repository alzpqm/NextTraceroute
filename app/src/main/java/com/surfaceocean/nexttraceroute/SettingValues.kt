package com.surfaceocean.nexttraceroute

/** Settings may come from older versions or restored files, not only the sliders. */
internal fun validIntegerSetting(value: Any?, default: Int, range: IntRange): Int {
    val number = when (value) {
        is Number -> value.toDouble()
        is String -> value.trim().toDoubleOrNull()
        else -> null
    } ?: return default
    return if (number.isFinite() && number >= range.first && number <= range.last &&
        number % 1.0 == 0.0) number.toInt() else default
}
