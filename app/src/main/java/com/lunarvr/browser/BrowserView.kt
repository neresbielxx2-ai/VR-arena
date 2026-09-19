package com.lunarvr.browser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient

class BrowserView(private val context: Context, private val controller: BrowserController) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    val width = 1024
    val height = 768
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
                // Ignore fallback
            }
        }
    }

    fun dispatchClick(normalizedX: Float, normalizedY: Float) {
        val px = (normalizedX * width).coerceIn(0f, width.toFloat() - 1f)
        val py = (normalizedY * height).coerceIn(0f, height.toFloat() - 1f)

        mainHandler.post {
            try {
                val wv = webView ?: return@post
                val now = SystemClock.uptimeMillis()
                val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, px, py, 0)
                val up = MotionEvent.obtain(now, now + 50, MotionEvent.ACTION_UP, px, py, 0)
                wv.dispatchTouchEvent(down)
                wv.dispatchTouchEvent(up)
                down.recycle()
                up.recycle()
                isDirty = true
            } catch (_: Exception) {}
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
