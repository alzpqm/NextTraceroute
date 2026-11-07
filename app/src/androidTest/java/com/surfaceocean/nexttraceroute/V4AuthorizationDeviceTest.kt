package com.surfaceocean.nexttraceroute

import android.content.ComponentName
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class V4AuthorizationDeviceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun credentialsAreEncryptedAndCorruptionDoesNotSilentlySelectV3() {
        val name = "v4-device-test.bin"
        val store = V4TokenStore(context, name)
        val file = File(context.noBackupFilesDir, name)
        try {
            store.clear()
            assertNull(store.load())
            val credential = "device-test-credential-only"
            store.save(V4Grant(credential, System.currentTimeMillis() + 60_000))
            assertEquals(credential, store.load()!!.token)
            assertFalse(file.readBytes().toString(Charsets.ISO_8859_1).contains(credential))
            val bytes = file.readBytes()
            bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
            file.writeBytes(bytes)
            val invalid = store.load()
            assertNotNull("Configured but corrupted credentials must not select v3", invalid)
            assertFalse(invalid!!.usable())
            store.clear()
            assertNull(store.load())
        } finally { store.clear() }
    }

    @Test fun authorizationScreenIsPrivateAndDisablesFileAccess() {
        val info = context.packageManager.getActivityInfo(ComponentName(context, V4AuthorizationActivity::class.java), 0)
        assertFalse(info.exported)
        ActivityScenario.launch(V4AuthorizationActivity::class.java).use { scenario ->
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertTrue(activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
                fun findWeb(view: View): WebView? {
                    if (view is WebView) return view
                    if (view is ViewGroup) for (index in 0 until view.childCount) findWeb(view.getChildAt(index))?.let { return it }
                    return null
                }
                val web = findWeb(activity.window.decorView)
                assertNotNull("Official authorisation WebView must be present", web)
                assertFalse(web!!.settings.allowFileAccess)
                assertFalse(web.settings.allowContentAccess)
            }
        }
    }
}
