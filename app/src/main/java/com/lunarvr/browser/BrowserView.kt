package com.lunarvr.browser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient

class BrowserView(private val context: Context, private val controller: BrowserController) {

    private val webView: WebView = WebView(context)
    private var webViewBitmap: Bitmap? = null
    private var isDirty: Boolean = true

    init {
        val width = 1024
        val height = 768
        webViewBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
        }

        webView.layout(0, 0, width, height)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                url?.let { controller.listener?.onPageStarted(it) }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                isDirty = true
                url?.let {
                    controller.updateNavigationState(
                        canBack = webView.canGoBack(),
                        canForward = webView.canGoForward(),
                        title = webView.title
                    )
                    controller.listener?.onPageFinished(it)
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                controller.updateProgress(newProgress)
                isDirty = true
            }
        }

        webView.loadUrl(controller.currentState.currentUrl)
    }

    fun loadUrl(url: String) {
        webView.loadUrl(url)
    }

    fun goBack() {
        if (webView.canGoBack()) {
            webView.goBack()
        }
    }

    fun goForward() {
        if (webView.canGoForward()) {
            webView.goForward()
        }
    }

    fun reload() {
        webView.reload()
    }

    fun captureBitmap(): Bitmap? {
        val bmp = webViewBitmap ?: return null
        val canvas = Canvas(bmp)
        webView.draw(canvas)
        isDirty = false
        return bmp
    }

    fun isContentDirty(): Boolean = isDirty
}
