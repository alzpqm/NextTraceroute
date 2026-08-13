/*

NextTraceroute, an Android traceroute app using Nexttrace API
Copyright (C) 2024-2026 surfaceocean
Email: r2qb8uc5@protonmail.com
GitHub: https://github.com/nxtrace/NextTraceroute
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.

Disclaimer: The NextTrace API (hosted at nxtrace.org) used by default in this program is not managed by the program's developer.
We do not guarantee the performance, accuracy, or any other aspect of the NextTrace API,
nor do we endorse, approve, or guarantee the results returned by the NextTrace API. Users may customize the API server address themselves.

This project uses the libraries listed below. Detailed information can be found in the LICENSE file of this project.
The "dnsjava" library is licensed under the BSD 3-Clause License.
The "seancfoley/IPAddress" library is licensed under the Apache 2.0 License.
The "square/okhttp" library is licensed under the Apache 2.0 License.
The "gson" library is licensed under the Apache 2.0 License.
The "slf4j-android" library is licensed under the MIT License.
The "androidx" library is licensed under the Apache 2.0 License.
The "Compose Color Picker" library is licensed under the MIT License.

*/




package com.surfaceocean.nexttraceroute

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.core.net.toUri
import androidx.room.Room
import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.surfaceocean.nexttraceroute.ui.theme.NextTracerouteTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NextTracerouteTheme {
                val materialColors = MaterialTheme.colorScheme
                val borderColor = remember {
                    mutableStateOf(materialColors.outlineVariant)
                }
                val disabledContentColor = remember {
                    mutableStateOf(materialColors.onSurface.copy(alpha = 0.38f))
                }
                val backgroundColor = remember { mutableStateOf(materialColors.background) }
                val genericTextColor = remember { mutableStateOf(materialColors.onBackground) }
                val navigationIconColor = remember { mutableStateOf(materialColors.onSurface) }
                val buttonEnabledColor = remember { mutableStateOf(materialColors.primary) }
                val buttonDisabledColor = remember { mutableStateOf(materialColors.surfaceVariant) }
                val buttonTextColor = remember { mutableStateOf(materialColors.onPrimary) }
                val resultSNColor = remember { mutableStateOf(materialColors.primary) }
                val resultASColor = remember { mutableStateOf(materialColors.tertiary) }
                val resultPingColor = remember { mutableStateOf(materialColors.secondary) }
                Surface(
                    modifier = Modifier
                        .fillMaxSize(),
                    color = backgroundColor.value,
                    contentColor = genericTextColor.value
                ) {
                    org.xbill.DNS.config.AndroidResolverConfigProvider.setContext(this)
                    val isSearchBarEnabled = remember { mutableStateOf(true) }
                    val currentPage = remember { mutableStateOf("main") }
                    val context = LocalContext.current
                    val currentLanguage =
                        remember { mutableStateOf("Default") } // Default, zh or en
                    val isTraceMapEnabled = remember { mutableStateOf(true) }
                    val maxTraceTTL = remember { mutableIntStateOf(30) }
                    val traceTimeout = remember { mutableStateOf("1") }
                    val traceCount = remember { mutableStateOf("5") }
                    val currentDNSMode = remember { mutableStateOf("udp") }
                    val currentDOHServer = remember { mutableStateOf("https://1.1.1.1/dns-query") }
                    val tracerouteDNSServer = remember { mutableStateOf("1.1.1.1") }
                    val apiHostNamePOW = remember { mutableStateOf("origin-fallback.nxtrace.org") }
                    val apiDNSNamePOW = remember { mutableStateOf("api.nxtrace.org") }
                    val apiHostName = remember { mutableStateOf("origin-fallback.nxtrace.org") }
                    val apiDNSName = remember { mutableStateOf("api.nxtrace.org") }
                    val lastBackPress = remember { mutableLongStateOf(0L) }
                    val listState = rememberLazyListState()
                    val isScrollToFirstLineTriggered = remember { mutableStateOf(false) }
                    val db = Room.databaseBuilder(
                        context,
                        AppDatabase::class.java, "app-database"
                    ).build()
                    val historyDao = db.historyDao()

                    //load settings from file
                    LaunchedEffect(Unit) {
                        try {
                            val file = File(context.filesDir, "settings.json")
                            if (file.exists()) {
                                context.openFileInput("settings.json").use { inputStream ->
                                    val size = inputStream.available()
                                    val buffer = ByteArray(size)
                                    inputStream.read(buffer)
                                    val jsonString = String(buffer)
                                    val gson = Gson()
                                    val mapType = object : TypeToken<Map<String, Any>>() {}.type
                                    val settingsMap: Map<String, Any> =
                                        gson.fromJson(jsonString, mapType)
                                    currentLanguage.value = settingsMap["currentLanguage"] as String
                                    isTraceMapEnabled.value =
                                        settingsMap["isTraceMapEnabled"] as Boolean
                                    maxTraceTTL.intValue =
                                        (settingsMap["maxTraceTTL"] as String).toInt()
                                    traceTimeout.value = settingsMap["traceTimeout"] as String
                                    traceCount.value = settingsMap["traceCount"] as String
                                    currentDNSMode.value = settingsMap["currentDNSMode"] as String
                                    tracerouteDNSServer.value =
                                        settingsMap["tracerouteDNSServer"] as String
                                    currentDOHServer.value =
                                        settingsMap["currentDOHServer"] as String
                                    apiHostNamePOW.value = settingsMap["apiHostNamePOW"] as String
                                    apiDNSNamePOW.value = settingsMap["apiDNSNamePOW"] as String
                                    apiHostName.value = settingsMap["apiHostName"] as String
                                    apiDNSName.value = settingsMap["apiDNSName"] as String
                                    borderColor.value =
                                        Color((settingsMap["borderColor"] as Double).toInt())
                                    disabledContentColor.value =
                                        Color((settingsMap["disabledContentColor"] as Double).toInt())
                                    backgroundColor.value =
                                        Color((settingsMap["backgroundColor"] as Double).toInt())
                                    genericTextColor.value =
                                        Color((settingsMap["genericTextColor"] as Double).toInt())
                                    navigationIconColor.value =
                                        Color((settingsMap["navigationIconColor"] as Double).toInt())
                                    buttonEnabledColor.value =
                                        Color((settingsMap["buttonEnabledColor"] as Double).toInt())
                                    buttonDisabledColor.value =
                                        Color((settingsMap["buttonDisabledColor"] as Double).toInt())
                                    buttonTextColor.value =
                                        Color((settingsMap["buttonTextColor"] as Double).toInt())
                                    resultSNColor.value =
                                        Color((settingsMap["resultSNColor"] as Double).toInt())
                                    resultASColor.value =
                                        Color((settingsMap["resultASColor"] as Double).toInt())
                                    resultPingColor.value =
                                        Color((settingsMap["resultPingColor"] as Double).toInt())
                                }

                            }


                        } catch (e: Exception) {
                            Log.e("readSettingsHandler", e.printStackTrace().toString())
                        }
                    }

                    // Scroll to the first result after a new trace starts.
                    LaunchedEffect(isScrollToFirstLineTriggered.value) {
                        if (isScrollToFirstLineTriggered.value) {
                            isScrollToFirstLineTriggered.value = false
                            listState.animateScrollToItem(index = 0)
                        }
                    }

                    BackHandler {
                        if (System.currentTimeMillis() - lastBackPress.longValue < 2000) {
                            db.close()
                            (context as? Activity)?.finish()
                        } else {
                            Toast.makeText(
                                context,
                                "Press again to exit this program!",
                                Toast.LENGTH_SHORT
                            ).show()
                            lastBackPress.longValue = System.currentTimeMillis()
                        }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        when (currentPage.value) {
                            "settings" -> {
                                SettingsColumn(
                                    context = context,
                                    currentPage = currentPage,
                                    currentLanguage = currentLanguage,
                                    isTraceMapEnabled = isTraceMapEnabled,
                                    maxTraceTTL = maxTraceTTL,
                                    traceTimeout = traceTimeout,
                                    traceCount = traceCount,
                                    tracerouteDNSServer = tracerouteDNSServer,
                                    apiHostNamePOW = apiHostNamePOW,
                                    apiDNSNamePOW = apiDNSNamePOW,
                                    apiHostName = apiHostName,
                                    apiDNSName = apiDNSName,
                                    currentDOHServer = currentDOHServer,
                                    currentDNSMode = currentDNSMode,
                                    borderColor = borderColor,
                                    disabledContentColor = disabledContentColor,
                                    backgroundColor = backgroundColor,
                                    genericTextColor = genericTextColor,
                                    navigationIconColor = navigationIconColor,
                                    buttonEnabledColor = buttonEnabledColor,
                                    buttonDisabledColor = buttonDisabledColor,
                                    buttonTextColor = buttonTextColor,
                                    resultSNColor = resultSNColor,
                                    resultASColor = resultASColor,
                                    resultPingColor = resultPingColor
                                )
                            }

                            "history" -> {
                                HistoryPage(
                                    context = context,
                                    currentPage = currentPage,
                                    historyDao = historyDao,
                                    db = db,
                                    borderColor = borderColor,
                                    disabledContentColor = disabledContentColor,
                                    backgroundColor = backgroundColor,
                                    genericTextColor = genericTextColor,
                                    navigationIconColor = navigationIconColor,
                                    buttonEnabledColor = buttonEnabledColor,
                                    buttonDisabledColor = buttonDisabledColor,
                                    buttonTextColor = buttonTextColor,
                                    resultSNColor = resultSNColor,
                                    resultASColor = resultASColor,
                                    resultPingColor = resultPingColor
                                )
                            }

                            "main" -> {
                                MyTopAppBar(
                                    currentPage = currentPage,
                                    isSearchBarEnabled = isSearchBarEnabled,
                                    context = context,
                                    borderColor = borderColor,
                                    disabledContentColor = disabledContentColor,
                                    backgroundColor = backgroundColor,
                                    genericTextColor = genericTextColor,
                                    navigationIconColor = navigationIconColor
                                )
                                MainColumn(
                                    isSearchBarEnabled = isSearchBarEnabled,
                                    currentLanguage = currentLanguage,
                                    isTraceMapEnabled = isTraceMapEnabled,
                                    maxTraceTTL = maxTraceTTL,
                                    traceTimeout = traceTimeout,
                                    traceCount = traceCount,
                                    tracerouteDNSServer = tracerouteDNSServer,
                                    apiHostNamePOW = apiHostNamePOW,
                                    apiDNSNamePOW = apiDNSNamePOW,
                                    apiHostName = apiHostName,
                                    apiDNSName = apiDNSName,
                                    context = context,
                                    currentDOHServer = currentDOHServer,
                                    currentDNSMode = currentDNSMode,
                                    listState = listState,
                                    isScrollToFirstLineTriggered = isScrollToFirstLineTriggered,
                                    historyDao = historyDao,
                                    db = db,
                                    borderColor = borderColor,
                                    disabledContentColor = disabledContentColor,
                                    backgroundColor = backgroundColor,
                                    genericTextColor = genericTextColor,
                                    buttonEnabledColor = buttonEnabledColor,
                                    buttonDisabledColor = buttonDisabledColor,
                                    buttonTextColor = buttonTextColor,
                                    resultSNColor = resultSNColor,
                                    resultASColor = resultASColor,
                                    resultPingColor = resultPingColor
                                )


                            }

                            "about" -> {
                                AboutPage(
                                    currentPage = currentPage,
                                    borderColor = borderColor,
                                    genericTextColor = genericTextColor,
                                    navigationIconColor = navigationIconColor
                                )
                            }
                        }

                    }

                }
            }
        }
    }
}


@Composable
fun AboutPage(
    currentPage: MutableState<String>,
    borderColor: MutableState<Color>,
    genericTextColor: MutableState<Color>,
    navigationIconColor: MutableState<Color>

) {
    BackHandler {
        currentPage.value = "main"
    }
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor.value)
            .padding(bottom = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { currentPage.value = "main" }) {
            Icon(Icons.Filled.Home, contentDescription = "Home", tint = navigationIconColor.value)
        }
    }
    Spacer(modifier = Modifier.height(8.dp))

    Column(modifier = Modifier.verticalScroll(scrollState)) {
        Text(
            color = genericTextColor.value,
            text = "NextTraceroute version " +
                    BuildConfig.VERSION_NAME + ", an Android traceroute app using Nexttrace API\n" +
                    "NextTrace core/API compatibility: v" + NEXTTRACE_CORE_VERSION + "\n" +
                    "Copyright (C) 2024-2026 surfaceocean\n" +
                    "Email: r2qb8uc5@protonmail.com\n" +
                    "GitHub: https://github.com/nxtrace/NextTraceroute\n" +
                    "This program is free software: you can redistribute it and/or modify\n" +
                    "it under the terms of the GNU General Public License as published by\n" +
                    "the Free Software Foundation, either version 3 of the License, or\n" +
                    "any later version.\n" +
                    "\n" +
                    "This program is distributed in the hope that it will be useful,\n" +
                    "but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
                    "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n" +
                    "GNU General Public License for more details.\n" +
                    "\n" +
                    "You should have received a copy of the GNU General Public License\n" +
                    "along with this program in the LICENSE file.  If not, see <https://www.gnu.org/licenses/>.\n" +
                    "\n" +
                    "Disclaimer: The NextTrace API (hosted at nxtrace.org) used by default in this program is not managed by the program's developer.\n" +
                    "We do not guarantee the performance, accuracy, or any other aspect of the NextTrace API,\n" +
                    "nor do we endorse, approve, or guarantee the results returned by the NextTrace API. Users may customize the API server address themselves.\n\n" +
                    "This project uses the libraries listed below. Detailed information can be found in the LICENSE file of this project.\n" +
                    "The \"dnsjava\" library is licensed under the BSD 3-Clause License.\n" +
                    "The \"seancfoley/IPAddress\" library is licensed under the Apache 2.0 License.\n" +
                    "The \"square/okhttp\" library is licensed under the Apache 2.0 License.\n" +
                    "The \"gson\" library is licensed under the Apache 2.0 License.\n" +
                    "The \"slf4j-android\" library is licensed under the MIT License.\n" +
                    "The \"androidx\" library is licensed under the Apache 2.0 License.\n" +
                    "\n",
            modifier = Modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTopAppBar(
    modifier: Modifier = Modifier,
    currentPage: MutableState<String>,
    isSearchBarEnabled: MutableState<Boolean>,
    context: Context,
    borderColor: MutableState<Color>,
    disabledContentColor: MutableState<Color>,
    backgroundColor: MutableState<Color>,
    genericTextColor: MutableState<Color>,
    navigationIconColor: MutableState<Color>
) {
    var showMenu by remember { mutableStateOf(false) }
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = backgroundColor.value,
            scrolledContainerColor = backgroundColor.value,
            navigationIconContentColor = genericTextColor.value,
            titleContentColor = genericTextColor.value,
            actionIconContentColor = navigationIconColor.value
        ),
        title = {
            Text(
                "NextTraceroute",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        modifier = modifier
            .fillMaxWidth(),
        actions = {
            IconButton(onClick = { showMenu = !showMenu }
            ) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "More",
                    tint = navigationIconColor.value
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(backgroundColor.value),
                shape = RoundedCornerShape(20.dp)
            ) {
                DropdownMenuItem(
                    modifier = Modifier.background(backgroundColor.value),
                    text = {
                        Text(
                            "Settings",
                            color = if (isSearchBarEnabled.value) genericTextColor.value else disabledContentColor.value
                        )
                    },
                    onClick = {
                        showMenu = false
                        currentPage.value = "settings"
                    },
                    enabled = isSearchBarEnabled.value
                )
                DropdownMenuItem(
                    modifier = Modifier.background(backgroundColor.value),
                    text = {
                        Text(
                            "History",
                            color = if (isSearchBarEnabled.value) genericTextColor.value else disabledContentColor.value
                        )
                    },
                    onClick = {
                        showMenu = false
                        currentPage.value = "history"
                    },
                    enabled = isSearchBarEnabled.value
                )
                DropdownMenuItem(
                    modifier = Modifier.background(backgroundColor.value),
                    text = {
                        Text(
                            "About",
                            color = if (isSearchBarEnabled.value) genericTextColor.value else disabledContentColor.value
                        )
                    },
                    onClick = {
                        showMenu = false
                        currentPage.value = "about"
                    },
                    enabled = isSearchBarEnabled.value
                )
                DropdownMenuItem(
                    modifier = Modifier.background(backgroundColor.value),
                    text = {
                        Text(
                            "Privacy Policy",
                            color = if (isSearchBarEnabled.value) genericTextColor.value else disabledContentColor.value
                        )
                    },
                    onClick = {
                        val privacyURL =
                            "https://github.com/nxtrace/NextTraceroute/blob/master/PrivacyPolicy.md"
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                privacyURL.toUri()
                            )
                        )
                    },
                    enabled = isSearchBarEnabled.value
                )
            }
        }
    )

}


@Composable
fun CheckThreadsStatus(
    scope: CoroutineScope,
    mutex: Mutex,
    tracerouteThreadsIntList: MutableList<Int>,
    isSearchBarEnabled: MutableState<Boolean>,
    multipleIps: MutableList<MutableState<String>>,
    isDNSInProgress: MutableState<Boolean>,
    currentDomain: MutableState<String>,
    searchText: MutableState<String>,
    copyHistory: MutableState<String>,
    historyDao: HistoryDao,
    db: AppDatabase
) {
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            // Give the worker effects time to register their jobs, then finish as
            // soon as the run is actually idle. The old implementation always
            // waited ten seconds and could also finish an empty/failed run.
            delay(timeMillis = 500)
            while (isDNSInProgress.value || tracerouteThreadsIntList.any { it != 0 }) {
                delay(timeMillis = 250)
            }
            mutex.withLock {
                tracerouteThreadsIntList.removeAll { it == 0 }
            }
            if (multipleIps.isEmpty()) {
                isSearchBarEnabled.value = true
                //Add history after all threads are finished
                val historyData = HistoryData(
                    ip = searchText.value,
                    domain = currentDomain.value,
                    history = copyHistory.value
                )
                db.withTransaction {
                    historyDao.insertHistory(historyData)
                }
                currentDomain.value = ""
            }
        }
    }

}


fun clearData(
    multipleIps: MutableList<MutableState<String>>,
    insertErrorText: MutableState<String>,
    nativePingCheckErrorText: MutableState<String>,
    singleHopCursor: MutableList<MutableState<String>>,
    gridDataList: MutableList<MutableList<MutableList<MutableState<String>>>>,
    testAPIText: MutableState<String>,
    preferredAPIIp: MutableState<String>,
    apiDNSList: MutableList<String>,
    apiToken: MutableState<String>,
    preferredAPIIpPOW: MutableState<String>,
    apiDNSListPOW: MutableList<String>,
    traceMapThreadsMapList: MutableList<List<MutableMap<String, Any?>>>,
    traceMapURL: MutableState<String>, isAPIFinished: MutableState<Boolean>,
    copyHistory: MutableState<String>
) {
    copyHistory.value = ""
    isAPIFinished.value = false
    traceMapURL.value = ""
    testAPIText.value = ""
    preferredAPIIp.value = ""
    apiDNSList.clear()
    apiDNSListPOW.clear()
    preferredAPIIpPOW.value = ""
    apiToken.value = ""
    multipleIps.clear()
    nativePingCheckErrorText.value = ""
    insertErrorText.value = ""
    traceMapThreadsMapList.clear()
    singleHopCursor.forEach { i ->
        i.value = ""
    }
    for (gridRow in gridDataList) {
        for (gridColumn in gridRow) {
            for (i in gridColumn) {
                i.value = ""
            }
        }
    }

}

//@Preview(showBackground = true)
@Composable
fun MainColumn(
    currentLanguage: MutableState<String>, isTraceMapEnabled: MutableState<Boolean>,
    maxTraceTTL: MutableIntState, traceTimeout: MutableState<String>,
    traceCount: MutableState<String>, tracerouteDNSServer: MutableState<String>,
    apiHostNamePOW: MutableState<String>, apiDNSNamePOW: MutableState<String>,
    apiHostName: MutableState<String>, apiDNSName: MutableState<String>,
    context: Context, isSearchBarEnabled: MutableState<Boolean>,
    currentDOHServer: MutableState<String>,
    currentDNSMode: MutableState<String>, listState: LazyListState,
    isScrollToFirstLineTriggered: MutableState<Boolean>,
    historyDao: HistoryDao, db: AppDatabase,
    borderColor: MutableState<Color>,
    disabledContentColor: MutableState<Color>,
    backgroundColor: MutableState<Color>,
    genericTextColor: MutableState<Color>,
    buttonEnabledColor: MutableState<Color>,
    buttonDisabledColor: MutableState<Color>,
    buttonTextColor: MutableState<Color>,
    resultSNColor: MutableState<Color>,
    resultASColor: MutableState<Color>,
    resultPingColor: MutableState<Color>
) {

    val threadMutex = remember { Mutex() }
    val tracerouteThreadsIntList = remember { mutableStateListOf<Int>() }
    val traceMapThreadsMapList = remember { mutableListOf<List<MutableMap<String, Any?>>>() }
    val traceMapURL = remember { mutableStateOf("") }
    val multipleIps = remember { mutableStateListOf<MutableState<String>>() }
    val isDNSInProgress = remember { mutableStateOf(false) }
    val isNativePing4Available = remember { mutableStateOf(true) }
    val isNativePing6Available = remember { mutableStateOf(true) }
    val nativePingCheckErrorText = remember { mutableStateOf("") }
    val traceRunId = remember { mutableIntStateOf(0) }
    val isButtonClicked = remember { mutableStateOf(false) }
    val singleHopCursor = remember(maxTraceTTL.intValue) {
        MutableList(maxTraceTTL.intValue) { mutableStateOf("") }
    }
    val insertErrorText = remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val activeRunScope = remember { mutableStateOf<CoroutineScope?>(null) }
    DisposableEffect(Unit) {
        onDispose { activeRunScope.value?.cancel() }
    }
    val searchText = remember { mutableStateOf("") }
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val keyboardController = LocalSoftwareKeyboardController.current
    val trHandler = remember { TracerouteHandler() }


    val preferredAPIIpPOW = remember { mutableStateOf("") }
    val apiDNSListPOW = remember { mutableListOf<String>() }
    val apiToken = remember { mutableStateOf("") }
    val preferredAPIIp = remember { mutableStateOf("") }
    val apiDNSList = remember { mutableListOf<String>() }
    val isAPIFinished = remember { mutableStateOf(false) }

    val currentDomain = remember { mutableStateOf("") }

    val basicGridData = remember {
        mutableStateListOf(
            mutableStateListOf(
                mutableStateOf(""), mutableStateOf(""),
                mutableStateOf(""), mutableStateOf("")
            ),
            mutableStateListOf(mutableStateOf("")),
            mutableStateListOf(mutableStateOf(""), mutableStateOf(""))
        )
    }
    val gridDataList = remember(maxTraceTTL.intValue) {
        mutableStateListOf<MutableList<MutableList<MutableState<String>>>>().apply {
            repeat(maxTraceTTL.intValue) {
                add(basicGridData.map { row ->
                    row.map { item ->
                        mutableStateOf(item.value)
                    }.toMutableList()
                }.toMutableList())
            }
        }
    }
    val testText = remember { mutableStateOf("") }
    val copyHistory = remember { mutableStateOf("") }
    val cancelTrace = {
        activeRunScope.value?.cancel()
        activeRunScope.value = null
        tracerouteThreadsIntList.clear()
        multipleIps.clear()
        isDNSInProgress.value = false
        isAPIFinished.value = true
        isSearchBarEnabled.value = true
        testText.value = "Trace stopped."
    }




    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        //Top Bar

        Spacer(modifier = Modifier.height(12.dp))
        //Run button
        if (isButtonClicked.value) {
            val normalizedTarget = normalizeTargetInput(searchText.value)
            if (trHandler.identifyInput(normalizedTarget) == ERROR_IDENTIFIER) {
                isButtonClicked.value = false
                insertErrorText.value = "Enter a valid hostname, IPv4, IPv6 address or URL."
            } else {
                searchText.value = normalizedTarget
                isButtonClicked.value = false
                isSearchBarEnabled.value = false
                traceRunId.intValue += 1
                keyboardController?.hide()
                if (activeRunScope.value == null) {
                    activeRunScope.value = CoroutineScope(
                        SupervisorJob() + Dispatchers.Main.immediate
                    )
                }
                tracerouteThreadsIntList.removeAll { it == 0 }
                clearData(
                multipleIps = multipleIps,
                nativePingCheckErrorText = nativePingCheckErrorText,
                singleHopCursor = singleHopCursor,
                gridDataList = gridDataList, insertErrorText = insertErrorText,
                testAPIText = testText, preferredAPIIp = preferredAPIIp,
                apiDNSList = apiDNSList, preferredAPIIpPOW = preferredAPIIpPOW,
                apiDNSListPOW = apiDNSListPOW,
                apiToken = apiToken,
                traceMapThreadsMapList = traceMapThreadsMapList,
                traceMapURL = traceMapURL, isAPIFinished = isAPIFinished,
                copyHistory = copyHistory
            )
                isScrollToFirstLineTriggered.value = true
            }
        }

        LaunchedEffect(traceRunId.intValue) {
            if (traceRunId.intValue > 0) {
                activeRunScope.value?.launch(Dispatchers.IO) {
                    trHandler.testNativePing(
                        v4Status = isNativePing4Available,
                        v6Status = isNativePing6Available,
                        errorText = nativePingCheckErrorText
                    )
                }
            }
        }

        // Keep the worker composables in the composition for the whole run.
        // The previous click-only placement could cancel their LaunchedEffects
        // as soon as the button reset its click state.
        if (!isSearchBarEnabled.value) {
            key(traceRunId.intValue) {
                val runScope = activeRunScope.value ?: coroutineScope
                trHandler.InsertHandler(
                    threadMutex = threadMutex,
                    tracerouteThreadsIntList = tracerouteThreadsIntList,
                    insertion = searchText,
                    insertErrorText = insertErrorText,
                    gridDataList = gridDataList,
                    scope = runScope,
                    maxTTL = maxTraceTTL,
                    count = traceCount,
                    timeout = traceTimeout,
                    multipleIps = multipleIps,
                    tracerouteDNSServer = tracerouteDNSServer,
                    context = context,
                    isDNSInProgress = isDNSInProgress,
                    testAPIText = testText,
                    currentDOHServer = currentDOHServer,
                    currentDNSMode = currentDNSMode,
                    isTraceMapEnabled = isTraceMapEnabled,
                    traceMapURL = traceMapURL,
                    preferredAPIIp = preferredAPIIp,
                    apiHostName = apiHostName,
                    traceMapThreadsMapList = traceMapThreadsMapList,
                    isSearchBarEnabled = isSearchBarEnabled,
                    isAPIFinished = isAPIFinished,
                    apiToken = apiToken,
                    currentLanguage = currentLanguage,
                    apiDNSList = apiDNSList,
                    apiDNSListPOW = apiDNSListPOW,
                    apiDNSName = apiDNSName,
                    apiDNSNamePOW = apiDNSNamePOW,
                    apiHostNamePOW = apiHostNamePOW,
                    preferredAPIIpPOW = preferredAPIIpPOW
                )
                CheckThreadsStatus(
                    scope = runScope,
                    mutex = threadMutex,
                    tracerouteThreadsIntList = tracerouteThreadsIntList,
                    isDNSInProgress = isDNSInProgress,
                    isSearchBarEnabled = isSearchBarEnabled,
                    multipleIps = multipleIps,
                    currentDomain = currentDomain,
                    searchText = searchText,
                    copyHistory = copyHistory,
                    historyDao = historyDao,
                    db = db
                )
            }
        }
        //Compare singleHopCursor with current value

        trHandler.EachHopHandler(
            threadMutex = threadMutex,
            tracerouteThreadsIntList = tracerouteThreadsIntList,
            singleHopCursor = singleHopCursor,
            gridDataList = gridDataList,
            scope = activeRunScope.value ?: coroutineScope,
            count = traceCount,

            timeout = traceTimeout,
            tracerouteDNSServer = tracerouteDNSServer,
            //testAPIText = testText,
            currentDNSMode = currentDNSMode,
            currentDOHServer = currentDOHServer
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchBar(
                    onSearchResults = searchText,
                    modifier = Modifier.weight(1f),
                    isButtonClicked = isButtonClicked,
                    isSearchBarEnabled = isSearchBarEnabled,
                    borderColor = borderColor,
                    backgroundColor = backgroundColor,
                    genericTextColor = genericTextColor,
                    buttonEnabledColor = buttonEnabledColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    modifier = Modifier.heightIn(min = 56.dp),
                    enabled = isSearchBarEnabled.value,
                    onClick = { isButtonClicked.value = true },
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSearchBarEnabled.value) buttonEnabledColor.value else buttonDisabledColor.value,
                        contentColor = buttonTextColor.value,
                        disabledContainerColor = buttonDisabledColor.value,
                        disabledContentColor = disabledContentColor.value
                    )
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Run")
                }
            }
            val searchDatabaseResultList = remember { mutableStateListOf<String>() }
            val scope = rememberCoroutineScope()
            LaunchedEffect(searchText.value) {
                scope.launch(Dispatchers.IO) {
                    if (isSearchBarEnabled.value && searchText.value != "") {
                        try {
                            db.withTransaction {
                                searchDatabaseResultList.clear()
                                val searchDatabaseReturn =
                                    (historyDao.findInputIP(searchText.value) + historyDao.findInputDomain(
                                        searchText.value
                                    )).distinct()
                                //only add if it's not perfectly matched
                                if (!(searchDatabaseReturn.size == 1 && searchDatabaseReturn[0] == searchText.value)
                                ) {
                                    searchDatabaseResultList.addAll(searchDatabaseReturn)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("databaseHandler", e.printStackTrace().toString())
                        }
                    }
                }
            }
            DropdownMenu(
                modifier = Modifier.background(backgroundColor.value),
                expanded = isSearchBarEnabled.value && searchDatabaseResultList.isNotEmpty(),
                properties = PopupProperties(
                    focusable = false,
                    dismissOnClickOutside = true,
                    dismissOnBackPress = true
                ),
                onDismissRequest = { }
            ) {
                searchDatabaseResultList.forEach { text ->
                    DropdownMenuItem(onClick = {
                        searchText.value = text
                        searchDatabaseResultList.clear()
                    }, text = { Text(text = text, color = genericTextColor.value) })
                }
            }
        }
        if (!isSearchBarEnabled.value) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Tracing route…",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall,
                            color = genericTextColor.value
                        )
                        OutlinedButton(
                            onClick = cancelTrace,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop")
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = buttonEnabledColor.value,
                        trackColor = borderColor.value
                    )
                }
            }
        }

        if (insertErrorText.value != "") {
            StatusCard(
                message = insertErrorText.value,
                color = MaterialTheme.colorScheme.errorContainer,
                textColor = MaterialTheme.colorScheme.onErrorContainer
            )
        }
        if (testText.value != "") {
            StatusCard(
                message = testText.value,
                color = MaterialTheme.colorScheme.secondaryContainer,
                textColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
//            clipboardManager.setPrimaryClip(ClipData.newPlainText("simple text",testText.value))
        }
        if (nativePingCheckErrorText.value != "") {
            StatusCard(
                message = nativePingCheckErrorText.value,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                textColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }

        val visibleGridDataList = gridDataList.filter { layer ->
            layer.any { row -> row.any { cell -> cell.value.isNotBlank() } }
        }
        val hasResult = visibleGridDataList.isNotEmpty()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (traceMapURL.value != "" && Patterns.WEB_URL.matcher(traceMapURL.value).matches()) {
                Button(
                    onClick = {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                traceMapURL.value.toUri()
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonEnabledColor.value,
                        contentColor = buttonTextColor.value,
                        disabledContainerColor = buttonDisabledColor.value,
                        disabledContentColor = disabledContentColor.value
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Open map")
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (hasResult && tracerouteThreadsIntList.all { it == 0 }) {
                copyHistory.value = gridDataList.joinToString(
                    separator = "\n",
                    prefix = "Traceroute Result:\n" + "IP:" + searchText.value + "\n" + "Domain:" + currentDomain.value + "\n"
                ) { layer ->
                    if (layer[0][0].value != "") {
                        layer.joinToString(separator = "\n") { row ->
                            row.joinToString(separator = ", ") { cell ->
                                cell.value
                            }
                        }
                    } else {
                        ""
                    }
                }.trim()
                Button(
                    onClick = {
                        clipboardManager.setPrimaryClip(
                            ClipData.newPlainText("simple text", copyHistory.value)
                        )
                        Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()

                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonEnabledColor.value,
                        contentColor = buttonTextColor.value,
                        disabledContainerColor = buttonDisabledColor.value,
                        disabledContentColor = disabledContentColor.value
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Copy result")
                }
            }
        }
        //test text

//        val testtest: MutableList<Int> = tracerouteThreadsIntList.toMutableStateList()
//        Text(text = testtest.toList().toString(), color = genericTextColor.value)
        //test button
//        Button(onClick = {
//            gridDataList[0][0][0].value = "1"
//            gridDataList[0][0][1].value = "114.51.41.91"
//            gridDataList[0][0][2].value = "AS114514"
//            gridDataList[0][0][3].value = "[EXAMPLE-peers]"
//            gridDataList[0][1][0].value = "United States example.com"
//            gridDataList[0][2][0].value = "123.example.com"
//            gridDataList[0][2][1].value = " 123.45 ms / 234.56 ms / 345.67 ms"
//
//        }) {
//            Text("Update")
//        }
        //Select a ip and change

        if (multipleIps.isNotEmpty() && tracerouteThreadsIntList.none { it != 0 }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Choose an address",
                        style = MaterialTheme.typography.titleSmall,
                        color = genericTextColor.value
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = multipleIps,
                            key = { _, item -> item.value }) { _, multipleIPItem ->
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    currentDomain.value = searchText.value
                                    searchText.value = multipleIPItem.value
                                    multipleIps.clear()
                                    isButtonClicked.value = true

                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = buttonEnabledColor.value,
                                    contentColor = buttonTextColor.value,
                                    disabledContainerColor = buttonDisabledColor.value,
                                    disabledContentColor = disabledContentColor.value
                                )
                            ) {
                                Text(multipleIPItem.value)
                            }
                        }
                    }
                }
            }
        }
        //LazyVerticalGrid(columns = GridCells.Fixed(gridRows),
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(items = visibleGridDataList) { _, gridDataListItem ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        for ((gridDataIndex, gridDataItem) in gridDataListItem.withIndex()) {
                            val arrangementForOneColumn =
                                if (gridDataItem.size == 1) {
                                    Arrangement.Center
                                } else {
                                    Arrangement.SpaceBetween
                                }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = arrangementForOneColumn,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for ((colIndex, item) in gridDataItem.withIndex()) {
                                    val colorForSpecialUse = when {
                                        gridDataIndex == 0 && colIndex == 0 -> resultSNColor.value
                                        gridDataIndex == 0 && (colIndex == 2 || colIndex == 3) -> resultASColor.value
                                        gridDataIndex == 2 && colIndex == 1 -> resultPingColor.value
                                        else -> genericTextColor.value
                                    }
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .padding(vertical = 4.dp, horizontal = 6.dp)
                                            .pointerInput(item) {
                                                detectTapGestures(
                                                    onLongPress = {
                                                        clipboardManager.setPrimaryClip(
                                                            ClipData.newPlainText(
                                                                "simple text",
                                                                item.value
                                                            )
                                                        )
                                                        Toast.makeText(
                                                            context,
                                                            "Copied!",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    },
                                                    onTap = {
                                                        if (item.value != "*" && item.value != "") {
                                                            if (!(gridDataIndex == 0 && colIndex == 0) && !(gridDataIndex == 2 && colIndex == 1)) {
                                                                val tapURL =
                                                                    "https://bgp.tools/search?q=" + item.value
                                                                context.startActivity(
                                                                    Intent(
                                                                        Intent.ACTION_VIEW,
                                                                        tapURL.toUri()
                                                                    )
                                                                )
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                    ) {
                                        Text(
                                            text = item.value,
                                            style = TextStyle(color = colorForSpecialUse)
                                        )
                                    }
                                }

                            }
                        }
                    }
                }
            }


        }


    }


}


@Composable
fun StatusCard(
    message: String,
    color: Color,
    textColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )
    }
}


@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    onSearchResults: MutableState<String>,
    isSearchBarEnabled: MutableState<Boolean>,
    isButtonClicked: MutableState<Boolean>,
    borderColor: MutableState<Color>,
    backgroundColor: MutableState<Color>,
    genericTextColor: MutableState<Color>,
    buttonEnabledColor: MutableState<Color>
) {
    // var searchText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(
            onGo = {
                if (isSearchBarEnabled.value) {
                    isButtonClicked.value = true
                }
                keyboardController?.hide()
            }
        ),
        singleLine = true,
        textStyle = TextStyle(color = genericTextColor.value),
        value = onSearchResults.value,
        onValueChange = {
            if (isSearchBarEnabled.value) {
                onSearchResults.value = it
                onSearchResults.value = onSearchResults.value.replace("\n", "").trim()

            }
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Target"
            )
        },
        trailingIcon = if (onSearchResults.value.isNotEmpty()) {
            {
                IconButton(onClick = { onSearchResults.value = "" }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear target")
                }
            }
        } else {
            null
        },
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = backgroundColor.value,
            focusedContainerColor = backgroundColor.value,
            focusedTextColor = genericTextColor.value,
            unfocusedTextColor = genericTextColor.value,
            focusedBorderColor = buttonEnabledColor.value,
            unfocusedBorderColor = borderColor.value,
            cursorColor = buttonEnabledColor.value
        ),
        placeholder = {
            Text("example.com, IPv4, IPv6 or URL", color = genericTextColor.value)
        },
        label = {
            Text("Target")
        },
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    )

}
