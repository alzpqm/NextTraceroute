package com.surfaceocean.nexttraceroute

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast

internal fun Context.tryStartActivity(intent: Intent) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, getString(R.string.external_missing), Toast.LENGTH_SHORT).show()
    } catch (_: SecurityException) {
        Toast.makeText(this, getString(R.string.external_forbidden), Toast.LENGTH_SHORT).show()
    }
}
