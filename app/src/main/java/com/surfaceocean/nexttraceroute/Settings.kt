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
    val keyboardController = LocalSoftwareKeyboardController.current
    val tracerouteHandler = remember { TracerouteHandler() }
    val languageValues = listOf("Default", "zh", "en")
    val languageLabels = listOf("跟隨系統", "中文", "English")
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
        maxHop = maxTraceTTL.intValue.toFloat()
        timeout = traceTimeout.value.toFloatOrNull()?.coerceIn(1f, 10f) ?: 1f
        packetCount = traceCount.value.toFloatOrNull()?.coerceIn(1f, 10f) ?: 5f
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

        if (!isIp(cleanDnsServer)) errors += "UDP／TCP DNS 伺服器格式無效"
        if (tracerouteHandler.identifyInput(cleanPowHost) != HOSTNAME_IDENTIFIER) {
            errors += "PoW 主機名稱格式無效"
        }
        if (!isHostOrIp(cleanPowDns)) errors += "PoW DNS 名稱格式無效"
        if (tracerouteHandler.identifyInput(cleanApiHost) != HOSTNAME_IDENTIFIER) {
            errors += "API 主機名稱格式無效"
        }
        if (!isHostOrIp(cleanApiDns)) errors += "API DNS 名稱格式無效"

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
            Toast.makeText(context, "設定已儲存", Toast.LENGTH_SHORT).show()
        } catch (exception: Exception) {
            Log.e("SettingSaveHandler", "Unable to save settings", exception)
            Toast.makeText(context, "無法儲存設定", Toast.LENGTH_LONG).show()
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
                Icon(Icons.Filled.Home, contentDescription = "返回首頁")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "設定",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "網路探測與服務端選項",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(onClick = ::saveSettings) {
                Text("儲存")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSection(title = "一般") {
                ChoiceSetting(
                    title = "API 回應語言",
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
                        Text("顯示路由地圖", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "追蹤完成後產生 TraceMap",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = traceMapEnabled, onCheckedChange = { traceMapEnabled = it })
                }
            }

            SettingsSection(title = "探測參數") {
                NumberSliderSetting(
                    title = "最大跳數",
                    value = maxHop,
                    valueRange = 1f..255f,
                    onValueChange = { maxHop = it.roundToInt().toFloat() }
                )
                SettingsDivider()
                NumberSliderSetting(
                    title = "單一封包逾時",
                    suffix = " 秒",
                    value = timeout,
                    valueRange = 1f..10f,
                    onValueChange = { timeout = it.roundToInt().toFloat() }
                )
                SettingsDivider()
                NumberSliderSetting(
                    title = "每跳封包數",
                    value = packetCount,
                    valueRange = 1f..10f,
                    onValueChange = { packetCount = it.roundToInt().toFloat() }
                )
            }

            SettingsSection(title = "DNS") {
                ChoiceSetting(
                    title = "查詢模式",
                    selectedLabel = dnsModeValues[dnsModeIndex].uppercase(),
                    options = dnsModeValues.map { it.uppercase() },
                    onSelected = { dnsModeIndex = it }
                )
                Spacer(Modifier.height(12.dp))
                SettingsTextField(
                    label = "UDP／TCP DNS 伺服器",
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
                title = "進階服務端",
                supportingText = "一般使用者不需要修改；NextTrace 預設不需要 API Token。"
            ) {
                SettingsTextField(
                    label = "PoW 主機名稱",
                    value = powHostName,
                    onValueChange = { powHostName = it.replace("\n", "") },
                    onDone = { keyboardController?.hide() }
                )
                Spacer(Modifier.height(12.dp))
                SettingsTextField(
                    label = "PoW DNS 名稱或 IP",
                    value = powDnsName,
                    onValueChange = { powDnsName = it.replace("\n", "") },
                    onDone = { keyboardController?.hide() }
                )
                Spacer(Modifier.height(12.dp))
                SettingsTextField(
                    label = "API 主機名稱",
                    value = apiHostNameDraft,
                    onValueChange = { apiHostNameDraft = it.replace("\n", "") },
                    onDone = { keyboardController?.hide() }
                )
                Spacer(Modifier.height(12.dp))
                SettingsTextField(
                    label = "API DNS 名稱或 IP",
                    value = apiDnsNameDraft,
                    onValueChange = { apiDnsNameDraft = it.replace("\n", "") },
                    onDone = { keyboardController?.hide() }
                )
            }

            Text(
                text = "外觀會自動跟隨 Android 的日間／夜間模式與動態色彩。",
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
    Column {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Text(
                text = value.roundToInt().toString() + suffix,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
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
