package com.surfaceocean.nexttraceroute

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

@Composable
internal fun V4AuthorizationSettings() {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    var revision by remember { mutableIntStateOf(0) }
    var grant by remember { mutableStateOf<V4Grant?>(null) }
    var loaded by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { revision++ }
    LaunchedEffect(revision) {
        grant = withContext(Dispatchers.IO) { V4TokenStore(context).load() }
        loaded = true
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(resources.getString(R.string.v4_authorization_title), style = MaterialTheme.typography.titleMedium)
            Text(resources.getString(when {
                !loaded -> R.string.v4_authorization_loading
                grant == null -> R.string.v4_not_configured
                grant?.usable() == true -> R.string.v4_authorization_ready
                else -> R.string.v4_authorization_expired
            }))
            grant?.takeIf { it.expiresAtMillis > 0 }?.let {
                Text(resources.getString(R.string.v4_expires_at, DateFormat.getDateTimeInstance().format(Date(it.expiresAtMillis))))
            }
            Text(resources.getString(R.string.v4_authorization_instructions), style = MaterialTheme.typography.bodySmall)
            TextButton(enabled = loaded, onClick = { launcher.launch(Intent(context, V4AuthorizationActivity::class.java)) }) {
                Text(resources.getString(R.string.v4_get_authorization))
            }
            if (grant != null) {
                TextButton(onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) { V4TokenStore(context).clear() }
                        revision++
                    }
                }) { Text(resources.getString(R.string.v4_remove_authorization)) }
            }
        }
    }
}
