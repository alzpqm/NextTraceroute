package com.surfaceocean.nexttraceroute

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast

internal fun Context.tryStartActivity(intent: Intent) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, "No app is available to open this link or share this result.", Toast.LENGTH_SHORT).show()
    } catch (_: SecurityException) {
        Toast.makeText(this, "This action is not permitted on this device.", Toast.LENGTH_SHORT).show()
    }
}
