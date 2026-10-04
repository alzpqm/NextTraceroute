/*

NextTraceroute, an Android traceroute app using Nexttrace API
Copyright (C) 2024-2026 surfaceocean
Project: https://github.com/alzpqm/NextTraceroute
Upstream: https://github.com/nxtrace/NextTraceroute
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

*/

package com.surfaceocean.nexttraceroute

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.google.gson.Gson
import kotlin.math.roundToInt

private val dohServers = listOf(
    "https://1.1.1.1/dns-query",
    "https://[2606:4700:4700::1111]/dns-query",
    "https://8.8.8.8/dns-query",
    "https://[2001:4860:4860::8888]/dns-query",
    "https://223.5.5.5/dns-query",
    "https://doh.pub/dns-query",
    "https://dns.cloudflare.com/dns-query",
    "https://dns.adguard-dns.com/dns-query",
    "https://doh.opendns.com/dns-query",
    "https://dns.google/dns-query",
    "https://ordns.he.net/dns-query",
    "https://dns.quad9.net/dns-query"
)

@Composable
fun SettingsColumn(
    modifier: Modifier = Modifier,
    context: Context,
    currentPage: MutableState<String>,
    currentLanguage: MutableState<String>,
    isTraceMapEnabled: MutableState<Boolean>,
    maxTraceTTL: MutableIntState,
    traceTimeout: MutableState<String>,
    traceCount: MutableState<String>,
    currentDNSMode: MutableState<String>,
    tracerouteDNSServer: MutableState<String>,
    currentDOHServer: MutableState<String>,
    apiHostNamePOW: MutableState<String>,
    apiDNSNamePOW: MutableState<String>,
    apiHostName: MutableState<String>,
    apiDNSName: MutableState<String>
) {
    val resources = LocalResources.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val tracerouteHandler = remember { TracerouteHandler() }
    val languageValues = listOf("Default", "zh", "en")
    val languageLabels = listOf(resources.getString(R.string.language_system), resources.getString(R.string.language_chinese), resources.getString(R.string.language_english))
    val displayLanguageLabels = listOf(resources.getString(R.string.language_chinese), resources.getString(R.string.language_english))
    var displayLanguageIndex by remember { mutableIntStateOf(displayLanguages.indexOf(displayLanguage(context))) }
    val dnsModeValues = listOf("udp", "tcp", "doh")

    var languageIndex by remember { mutableIntStateOf(0) }
    var traceMapEnabled by remember { mutableStateOf(true) }
    var maxHop by remember { mutableFloatStateOf(30f) }
    var timeout by remember { mutableFloatStateOf(1f) }
    var packetCount by remember { mutableFloatStateOf(5f) }
    var dnsModeIndex by remember { mutableIntStateOf(0) }
    var dnsServer by remember { mutableStateOf("") }
    var dohServerIndex by remember { mutableIntStateOf(0) }
    var powHostName by remember { mutableStateOf("") }
    var powDnsName by remember { mutableStateOf("") }
    var apiHostNameDraft by remember { mutableStateOf("") }
    var apiDnsNameDraft by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        languageIndex = languageValues.indexOf(currentLanguage.value).coerceAtLeast(0)
        traceMapEnabled = isTraceMapEnabled.value
        maxHop = validIntegerSetting(maxTraceTTL.intValue, 30, 1..255).toFloat()
        timeout = validIntegerSetting(traceTimeout.value, 1, 1..10).toFloat()
        packetCount = validIntegerSetting(traceCount.value, 5, 1..10).toFloat()
        dnsModeIndex = dnsModeValues.indexOf(currentDNSMode.value).coerceAtLeast(0)
        dnsServer = tracerouteDNSServer.value
        dohServerIndex = dohServers.indexOf(currentDOHServer.value).coerceAtLeast(0)
        powHostName = apiHostNamePOW.value
        powDnsName = apiDNSNamePOW.value
        apiHostNameDraft = apiHostName.value
        apiDnsNameDraft = apiDNSName.value
    }

    fun saveSettings() {
        val errors = mutableListOf<String>()
        val cleanDnsServer = dnsServer.trim()
        val cleanPowHost = powHostName.trim()
        val cleanPowDns = powDnsName.trim()
        val cleanApiHost = apiHostNameDraft.trim()
        val cleanApiDns = apiDnsNameDraft.trim()

        fun isIp(value: String): Boolean {
            val type = tracerouteHandler.identifyInput(value)
            return type == IPV4_IDENTIFIER || type == IPV6_IDENTIFIER
        }

        fun isHostOrIp(value: String): Boolean {
            val type = tracerouteHandler.identifyInput(value)
            return type == HOSTNAME_IDENTIFIER || type == IPV4_IDENTIFIER || type == IPV6_IDENTIFIER
        }

        if (!isIp(cleanDnsServer)) errors += resources.getString(R.string.dns_server_invalid)
        if (tracerouteHandler.identifyInput(cleanPowHost) != HOSTNAME_IDENTIFIER) {
            errors += resources.getString(R.string.pow_host_invalid)
        }
        if (!isHostOrIp(cleanPowDns)) errors += resources.getString(R.string.pow_dns_invalid)
        if (tracerouteHandler.identifyInput(cleanApiHost) != HOSTNAME_IDENTIFIER) {
            errors += resources.getString(R.string.api_host_invalid)
        }
        if (!isHostOrIp(cleanApiDns)) errors += resources.getString(R.string.api_dns_invalid)

        if (errors.isNotEmpty()) {
            Toast.makeText(context, errors.joinToString("\n"), Toast.LENGTH_LONG).show()
            return
        }

        currentLanguage.value = languageValues[languageIndex]
        isTraceMapEnabled.value = traceMapEnabled
        maxTraceTTL.intValue = maxHop.roundToInt()
        traceTimeout.value = timeout.roundToInt().toString()
        traceCount.value = packetCount.roundToInt().toString()
        currentDNSMode.value = dnsModeValues[dnsModeIndex]
        tracerouteDNSServer.value = cleanDnsServer
        currentDOHServer.value = dohServers[dohServerIndex]
        apiHostNamePOW.value = cleanPowHost
        apiDNSNamePOW.value = cleanPowDns
        apiHostName.value = cleanApiHost
        apiDNSName.value = cleanApiDns

        val settings = mapOf(
            "currentLanguage" to currentLanguage.value,
            "isTraceMapEnabled" to isTraceMapEnabled.value,
            "maxTraceTTL" to maxTraceTTL.intValue.toString(),
            "traceTimeout" to traceTimeout.value,
            "traceCount" to traceCount.value,
            "currentDNSMode" to currentDNSMode.value,
            "tracerouteDNSServer" to tracerouteDNSServer.value,
            "currentDOHServer" to currentDOHServer.value,
            "apiHostNamePOW" to apiHostNamePOW.value,
            "apiDNSNamePOW" to apiDNSNamePOW.value,
            "apiHostName" to apiHostName.value,
            "apiDNSName" to apiDNSName.value
        )

        try {
            context.openFileOutput("settings.json", Context.MODE_PRIVATE).use { output ->
                output.write(Gson().toJson(settings).toByteArray())
            }
            Toast.makeText(context, resources.getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
            val selectedLanguage = displayLanguages[displayLanguageIndex]
            if (selectedLanguage != displayLanguage(context)) {
                context.getSharedPreferences(DISPLAY_LANGUAGE_PREFS, Context.MODE_PRIVATE)
                    .edit { putString(DISPLAY_LANGUAGE_KEY, selectedLanguage) }
                (context as? Activity)?.recreate()
            }
        } catch (exception: Exception) {
            Log.e("SettingSaveHandler", "Unable to save settings", exception)
            Toast.makeText(context, resources.getString(R.string.settings_save_failed), Toast.LENGTH_LONG).show()
        }
    }

    BackHandler { currentPage.value = "main" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { currentPage.value = "main" }) {
                Icon(Icons.Filled.Home, contentDescription = resources.getString(R.string.action_home))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resources.getString(R.string.menu_settings),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = resources.getString(R.string.settings_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(onClick = ::saveSettings) {
                Text(resources.getString(R.string.action_save))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSection(title = resources.getString(R.string.settings_general)) {
                ChoiceSetting(
                    title = resources.getString(R.string.display_language),
                    selectedLabel = displayLanguageLabels[displayLanguageIndex],
                    options = displayLanguageLabels,
                    onSelected = { displayLanguageIndex = it }
                )
                SettingsDivider()
                ChoiceSetting(
                    title = resources.getString(R.string.api_language),
                    selectedLabel = languageLabels[languageIndex],
                    options = languageLabels,
                    onSelected = { languageIndex = it }
                )
                SettingsDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(resources.getString(R.string.show_map), style = MaterialTheme.typography.titleMedium)
                        Text(
                            resources.getString(R.string.show_map_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = traceMapEnabled, onCheckedChange = { traceMapEnabled = it })
                }
            }

            SettingsSection(title = resources.getString(R.string.probe_settings)) {
                NumberSliderSetting(
                    title = resources.getString(R.string.max_hops),
                    value = maxHop,
                    valueRange = 1f..255f,
                    onValueChange = { maxHop = it.roundToInt().toFloat() }
                )
                SettingsDivider()
                NumberSliderSetting(
                    title = resources.getString(R.string.packet_timeout),
                    suffix = resources.getString(R.string.seconds_suffix),
                    value = timeout,
                    valueRange = 1f..10f,
                    onValueChange = { timeout = it.roundToInt().toFloat() }
                )
                SettingsDivider()
                NumberSliderSetting(
                    title = resources.getString(R.string.packet_count),
                    value = packetCount,
                    valueRange = 1f..10f,
                    onValueChange = { packetCount = it.roundToInt().toFloat() }
                )
            }

            SettingsSection(title = "DNS") {
                ChoiceSetting(
                    title = resources.getString(R.string.dns_mode),
                    selectedLabel = dnsModeValues[dnsModeIndex].uppercase(),
                    options = dnsModeValues.map { it.uppercase() },
                    onSelected = { dnsModeIndex = it }
                )
                Spacer(Modifier.height(12.dp))
                SettingsTextField(
                    label = resources.getString(R.string.dns_server),
                    value = dnsServer,
                    onValueChange = { dnsServer = it.replace("\n", "") },
                    onDone = { keyboardController?.hide() }
                )
                Spacer(Modifier.height(12.dp))
                ChoiceSetting(
                    title = "DNS over HTTPS",
                    selectedLabel = dohServers[dohServerIndex],
                    options = dohServers,
                    onSelected = { dohServerIndex = it }
                )
            }

            SettingsSection(
                title = resources.getString(R.string.advanced_services),
                supportingText = resources.getString(R.string.advanced_description)
            ) {
                SettingsTextField(
                    label = resources.getString(R.string.pow_host),
                    value = powHostName,
                    onValueChange = { powHostName = it.replace("\n", "") },
                    onDone = { keyboardController?.hide() }
                )
                Spacer(Modifier.height(12.dp))
                SettingsTextField(
                    label = resources.getString(R.string.pow_dns),
                    value = powDnsName,
                    onValueChange = { powDnsName = it.replace("\n", "") },
                    onDone = { keyboardController?.hide() }
                )
                Spacer(Modifier.height(12.dp))
                SettingsTextField(
                    label = resources.getString(R.string.api_host),
                    value = apiHostNameDraft,
                    onValueChange = { apiHostNameDraft = it.replace("\n", "") },
                    onDone = { keyboardController?.hide() }
                )
                Spacer(Modifier.height(12.dp))
                SettingsTextField(
                    label = resources.getString(R.string.api_dns),
                    value = apiDnsNameDraft,
                    onValueChange = { apiDnsNameDraft = it.replace("\n", "") },
                    onDone = { keyboardController?.hide() }
                )
            }

            Text(
                text = resources.getString(R.string.appearance_description),
                modifier = Modifier.padding(horizontal = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    supportingText: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (supportingText != null) {
                    Text(
                        text = supportingText,
                        modifier = Modifier.padding(bottom = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                content()
            }
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 14.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    )
}

@Composable
private fun ChoiceSetting(
    title: String,
    selectedLabel: String,
    options: List<String>,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium
        )
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.widthIn(max = 230.dp)
            ) {
                Text(text = selectedLabel, maxLines = 1)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelected(index)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberSliderSetting(
    title: String,
    suffix: String = "",
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    val safeValue = value.takeIf { it.isFinite() }?.coerceIn(valueRange) ?: valueRange.start
    Column {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Text(
                text = safeValue.roundToInt().toString() + suffix,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = safeValue,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}

@Composable
private fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit
) {
    Column {
        Text(
            text = label,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() })
        )
    }
}
