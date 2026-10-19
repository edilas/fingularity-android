package com.agosllc.fingularity

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var billing: BillingManager
    private lateinit var playGames: PlayGamesManager
    private var pageReady = false
    private var pendingPrice: String? = null
    private var pendingEntitlement: Boolean? = null
    private var pendingGamesAvailability: Boolean? = null

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        // Defer immersive mode until the Activity has an attached decor view.

        webView = WebView(this).apply {
            setBackgroundColor(Color.BLACK)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = false
            settings.mediaPlaybackRequiresUserGesture = false
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            settings.setSupportZoom(false)
            settings.loadWithOverviewMode = false
            settings.useWideViewPort = true
        }

        billing = BillingManager(
            activity = this,
            onPrice = { price -> runOnUiThread { pushPrice(price) } },
            onEntitlement = { owned -> runOnUiThread { pushEntitlement(owned) } },
            onPurchaseResult = { ok, message -> runOnUiThread { reportPurchase(ok, message) } }
        )
        playGames = PlayGamesManager(this) { available ->
            runOnUiThread { pushPlayGamesAvailability(available) }
        }

        webView.addJavascriptInterface(GameBridge(this, billing, playGames), "AndroidBridge")
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                android.util.Log.d(
                    "FingularityJS",
                    "${consoleMessage.message()} @ ${consoleMessage.lineNumber()}"
                )
                return true
            }
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                // The native build is fully local. Do not allow the game WebView to navigate away.
                return request.url.scheme != "file"
            }

            override fun onPageFinished(view: WebView, url: String) {
                pageReady = true
                pushSafeAreas()
                pendingPrice?.let { pushPrice(it) }
                pendingEntitlement?.let { pushEntitlement(it) }
                pendingGamesAvailability?.let { pushPlayGamesAvailability(it) }
                playGames.refreshAvailability()
            }
        }

        webView.setOnApplyWindowInsetsListener { _, insets ->
            pushSafeAreas(insets)
            insets
        }

        setContentView(webView)
        window.decorView.post { enterImmersiveMode() }
        webView.loadUrl("file:///android_asset/game.html")

        billing.start()
        playGames.refreshAvailability()
    }

    override fun onResume() {
        super.onResume()
        enterImmersiveMode()
        if (::billing.isInitialized) billing.refreshEntitlement()
        if (::playGames.isInitialized) playGames.refreshAvailability()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersiveMode()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!::webView.isInitialized) {
            super.onBackPressed()
            return
        }
        webView.evaluateJavascript(
            "(function(){" +
                "if(document.body.classList.contains('playing')){" +
                "var b=document.getElementById('menu-btn');if(b){b.click();return 'handled';}}" +
                "if(document.getElementById('pause-overlay')?.classList.contains('show')){" +
                "document.querySelector('[data-pause=resume]')?.click();return 'handled';}" +
                "return 'exit';})()"
        ) { result ->
            if (result == "\"exit\"") finish()
        }
    }

    override fun onDestroy() {
        if (::billing.isInitialized) billing.destroy()
        if (::webView.isInitialized) {
            webView.removeJavascriptInterface("AndroidBridge")
            webView.destroy()
        }
        super.onDestroy()
    }

    private fun enterImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
        }
    }

    private fun pushPrice(price: String) {
        pendingPrice = price
        if (!pageReady) return
        js("window.fingOnPrice && window.fingOnPrice(${quote(price)})")
    }

    private fun pushEntitlement(owned: Boolean) {
        pendingEntitlement = owned
        if (!pageReady) return
        js("window.fingOnEntitlement && window.fingOnEntitlement(${if (owned) "true" else "false"})")
    }

    private fun reportPurchase(ok: Boolean, message: String?) {
        if (!pageReady) return
        js(
            "window.fingOnPurchaseResult && window.fingOnPurchaseResult(" +
                (if (ok) "true" else "false") + "," + quote(message ?: "") + ")"
        )
    }

    private fun pushPlayGamesAvailability(available: Boolean) {
        pendingGamesAvailability = available
        if (!pageReady) return
        js("window.fingOnGameCenter && window.fingOnGameCenter(${if (available) "true" else "false"})")
    }

    private fun pushSafeAreas(insets: WindowInsets? = if (Build.VERSION.SDK_INT >= 23) webView.rootWindowInsets else null) {
        if (!pageReady || insets == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val cutout = insets.displayCutout
        val top = cutout?.safeInsetTop ?: 0
        val bottom = cutout?.safeInsetBottom ?: 0
        val left = cutout?.safeInsetLeft ?: 0
        val right = cutout?.safeInsetRight ?: 0
        js(
            "(function(){var r=document.documentElement.style;" +
                "r.setProperty('--sa-top','${top}px');" +
                "r.setProperty('--sa-bottom','${bottom}px');" +
                "r.setProperty('--sa-left','${left}px');" +
                "r.setProperty('--sa-right','${right}px');})()"
        )
    }

    private fun js(script: String) {
        webView.evaluateJavascript(script, null)
    }

    private fun quote(value: String): String {
        return org.json.JSONObject.quote(value)
    }
}
