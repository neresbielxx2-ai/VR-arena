package com.lunarvr.browser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient

class BrowserView(private val context: Context, private val controller: BrowserController) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    private val width = 1024
    private val height = 768
    private val webViewBitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    private val bitmapLock = Any()
    @Volatile private var isDirty: Boolean = true

    init {
        mainHandler.post {
            try {
                val wv = WebView(context)
                wv.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }
                wv.layout(0, 0, width, height)

                wv.webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        url?.let { controller.listener?.onPageStarted(it) }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        isDirty = true
                        url?.let {
                            controller.updateNavigationState(
                                canBack = wv.canGoBack(),
                                canForward = wv.canGoForward(),
                                title = wv.title
                            )
                            controller.listener?.onPageFinished(it)
                        }
                    }
                }

                wv.webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        controller.updateProgress(newProgress)
                        isDirty = true
                    }
                }

                wv.loadUrl(controller.currentState.currentUrl)
                webView = wv
            } catch (e: Exception) {
                // In case WebView is missing or fails on some devices
            }
        }
    }

    fun loadUrl(url: String) {
        mainHandler.post {
            try {
                webView?.loadUrl(url)
            } catch (_: Exception) {}
        }
    }

    fun goBack() {
        mainHandler.post {
            try {
                if (webView?.canGoBack() == true) {
                    webView?.goBack()
                }
            } catch (_: Exception) {}
        }
    }

    fun goForward() {
        mainHandler.post {
            try {
                if (webView?.canGoForward() == true) {
                    webView?.goForward()
                }
            } catch (_: Exception) {}
        }
    }

    fun reload() {
        mainHandler.post {
            try {
                webView?.reload()
            } catch (_: Exception) {}
        }
    }

    fun captureBitmap(): Bitmap? {
        val wv = webView ?: return null
        synchronized(bitmapLock) {
            try {
                val canvas = Canvas(webViewBitmap)
                wv.draw(canvas)
                isDirty = false
                return webViewBitmap
            } catch (e: Exception) {
                return null
            }
        }
    }

    fun isContentDirty(): Boolean = isDirty
}
