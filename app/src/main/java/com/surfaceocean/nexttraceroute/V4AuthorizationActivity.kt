package com.surfaceocean.nexttraceroute

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.gson.JsonParser
import com.surfaceocean.nexttraceroute.ui.theme.NextTracerouteTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.net.URI

internal fun isV4AuthorizationPage(url: String?): Boolean = runCatching {
    val uri = URI(url ?: "")
    uri.scheme == "https" && uri.host.equals("api.nxtrace.org", true) &&
        uri.port in listOf(-1, 443) && uri.rawUserInfo == null && uri.rawQuery == null &&
        uri.rawPath in listOf("/v4/api-tokens", "/v4/api-tokens/")
}.getOrDefault(false)

internal fun isV4WebResource(url: String?): Boolean {
    if (url == "about:blank" || url == "about:srcdoc") return true
    return runCatching {
        val uri = URI(url ?: "")
        uri.scheme == "https" && uri.port in listOf(-1, 443) && uri.rawUserInfo == null &&
            (uri.host.equals("challenges.cloudflare.com", true) ||
                (uri.host.equals("api.nxtrace.org", true) &&
                    (uri.rawPath == "/v4/api-tokens" || uri.rawPath.startsWith("/v4/api-tokens/"))))
    }.getOrDefault(false)
}

/** Official, interactive issuance only. No JavaScript bridge or CAPTCHA automation. */
class V4AuthorizationActivity : ComponentActivity() {
    private var web: WebView? = null
    private val handler = Handler(Looper.getMainLooper())
    private var status by mutableStateOf("")
    private var importing = false
    private var lastCandidate: String? = null
    private var pageGeneration = 0

    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(localizedContext(newBase))

    private val poll = object : Runnable {
        override fun run() {
            val view = web ?: return
            if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
            if (!importing && isV4AuthorizationPage(view.url)) {
                val generation = pageGeneration
                view.evaluateJavascript(READ_RECORD) { value ->
                    if (generation != pageGeneration || !isV4AuthorizationPage(view.url) || isFinishing || importing) return@evaluateJavascript
                    val raw = runCatching { JsonParser.parseString(value).asString }.getOrNull() ?: return@evaluateJavascript
                    val grant = parseV4Grant(raw) ?: return@evaluateJavascript
                    if (lastCandidate == grant.token) return@evaluateJavascript
                    lastCandidate = grant.token
                    import(grant)
                }
            }
            handler.postDelayed(this, 1000)
        }
    }

    private fun import(grant: V4Grant) {
        importing = true
        status = getString(R.string.v4_authorizing)
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val verified = NextTraceV4Client().verify(grant)
                    V4TokenStore(this@V4AuthorizationActivity).save(verified)
                }
                setResult(Activity.RESULT_OK)
                finish()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                status = getString(when ((e as? V4ApiException)?.status) {
                    401 -> R.string.v4_authorization_expired
                    429 -> R.string.v4_quota_exhausted
                    else -> R.string.v4_authorization_failed
                })
            } finally {
                importing = false
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        status = getString(R.string.v4_authorization_instructions)
        setContent {
            NextTracerouteTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                        Row {
                            TextButton(onClick = { finish() }) { Text(getString(R.string.action_close)) }
                            TextButton(enabled = !importing, onClick = { lastCandidate = null }) {
                                Text(getString(R.string.v4_retry_import))
                            }
                        }
                        Text(getString(R.string.v4_authorization_title), Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleLarge)
                        Text(status, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                        AndroidView(modifier = Modifier.weight(1f), factory = { context ->
                            WebView(context).also { view ->
                                web = view
                                view.settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    allowFileAccess = false
                                    allowContentAccess = false
                                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                    setSupportMultipleWindows(false)
                                }
                                view.webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                                        if (request.isForMainFrame) !isV4AuthorizationPage(request.url.toString())
                                        else !isV4WebResource(request.url.toString())

                                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                                        if (isV4WebResource(request.url.toString())) null else
                                            WebResourceResponse("text/plain", "UTF-8", 403, "Blocked", emptyMap(), ByteArrayInputStream(ByteArray(0)))

                                    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                                        pageGeneration++
                                        if (!isV4AuthorizationPage(url)) view.stopLoading()
                                    }
                                }
                                view.loadUrl(V4_TOKEN_PAGE)
                            }
                        })
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        web?.onResume()
        handler.removeCallbacks(poll)
        handler.postDelayed(poll, 1000)
    }

    override fun onPause() {
        handler.removeCallbacks(poll)
        web?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacks(poll)
        web?.apply { stopLoading(); clearHistory(); clearCache(true); destroy() }
        web = null
        // Official page uses localStorage; discard that plaintext copy on every exit.
        WebStorage.getInstance().deleteOrigin(V4_ORIGIN)
        CookieManager.getInstance().removeAllCookies(null)
        super.onDestroy()
    }

    companion object {
        private const val READ_RECORD = """(function(){
            if(location.origin !== 'https://api.nxtrace.org' || !/^\/v4\/api-tokens\/?$/.test(location.pathname)) return null;
            var value = localStorage.getItem('nexttrace_v4_public_token_v1');
            return value && value.length <= 8192 ? value : null;
        })();"""
    }
}
