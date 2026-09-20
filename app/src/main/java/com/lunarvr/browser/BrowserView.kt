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

class BrowserView(
    private val context: Context,
    private val controller: BrowserController,
    var onTextInputRequested: ((initialText: String, onInputSubmitted: (String) -> Unit) -> Unit)? = null
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    val width = 1280
    val height = 800
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
                    databaseEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    mediaPlaybackRequiresUserGesture = false
                    setRenderPriority(android.webkit.WebSettings.RenderPriority.HIGH)
                    cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                }
                wv.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
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
                
                // Emulate genuine finger tap sequence with proper coordinates and properties
                val downProps = arrayOf(MotionEvent.PointerProperties().apply {
                    id = 0
                    toolType = MotionEvent.TOOL_TYPE_FINGER
                })
                val downCoords = arrayOf(MotionEvent.PointerCoords().apply {
                    x = px
                    y = py
                    pressure = 1.0f
                    size = 1.0f
                })

                val down = MotionEvent.obtain(
                    now, now, MotionEvent.ACTION_DOWN,
                    1, downProps, downCoords, 0, 0, 1.0f, 1.0f, 0, 0, 0, 0
                )
                val up = MotionEvent.obtain(
                    now, now + 65, MotionEvent.ACTION_UP,
                    1, downProps, downCoords, 0, 0, 1.0f, 1.0f, 0, 0, 0, 0
                )

                wv.dispatchTouchEvent(down)
                wv.dispatchTouchEvent(up)
                down.recycle()
                up.recycle()

                // Execute JavaScript elementFromPoint tap and detect interactive text input fields reliably
                val jsClick = """
                    (function() {
                        function isTextField(el) {
                            if (!el) return false;
                            var tag = el.tagName ? el.tagName.toLowerCase() : '';
                            var type = el.type ? el.type.toLowerCase() : '';
                            var role = el.getAttribute ? el.getAttribute('role') : '';
                            if (tag === 'input') {
                                return ['text', 'search', 'url', 'password', 'email', 'number', 'tel', ''].indexOf(type) !== -1;
                            }
                            if (tag === 'textarea' || el.isContentEditable || role === 'textbox') {
                                return true;
                            }
                            return false;
                        }

                        var elem = document.elementFromPoint($px, $py);
                        var target = elem;
                        while (target && target !== document.body && !isTextField(target)) {
                            target = target.parentElement;
                        }
                        var field = isTextField(target) ? target : (isTextField(elem) ? elem : null);

                        if (field) {
                            field.focus();
                            try { field.click(); } catch(e){}
                            var val = field.value !== undefined ? field.value : (field.innerText || '');
                            return JSON.stringify({ isText: true, val: val });
                        }

                        // Also check currently focused element
                        var active = document.activeElement;
                        if (isTextField(active)) {
                            var val2 = active.value !== undefined ? active.value : (active.innerText || '');
                            return JSON.stringify({ isText: true, val: val2 });
                        }

                        return JSON.stringify({ isText: false });
                    })();
                """.trimIndent()

                wv.evaluateJavascript(jsClick) { result ->
                    try {
                        if (result != null && result != "null") {
                            val isText = result.contains("\"isText\":true") || result.contains("\"isText\": true")
                            if (isText) {
                                var initialVal = ""
                                val idx = result.indexOf("\"val\":\"")
                                if (idx != -1) {
                                    val start = idx + 7
                                    val end = result.indexOf("\"", start)
                                    if (end != -1) {
                                        initialVal = result.substring(start, end)
                                    }
                                }
                                mainHandler.post {
                                    onTextInputRequested?.invoke(initialVal) { submittedText ->
                                        mainHandler.post {
                                            val escaped = submittedText.replace("'", "\\'").replace("\n", " ")
                                            val insertJs = """
                                                (function() {
                                                    var elem = document.activeElement;
                                                    if (elem) {
                                                        if ('value' in elem) {
                                                            elem.value = '$escaped';
                                                        } else if (elem.isContentEditable) {
                                                            elem.innerText = '$escaped';
                                                        }
                                                        elem.dispatchEvent(new Event('input', { bubbles: true }));
                                                        elem.dispatchEvent(new Event('change', { bubbles: true }));
                                                        if (elem.form) {
                                                            elem.form.submit();
                                                        } else {
                                                            var enterEvent = new KeyboardEvent('keydown', { bubbles: true, cancelable: true, keyCode: 13 });
                                                            elem.dispatchEvent(enterEvent);
                                                        }
                                                    }
                                                })();
                                            """.trimIndent()
                                            wv.evaluateJavascript(insertJs, null)
                                            isDirty = true
                                        }
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }

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

    private var lastCaptureTime = 0L

    fun captureBitmap(): Bitmap? {
        val wv = webView ?: return null
        val now = SystemClock.uptimeMillis()
        if (now - lastCaptureTime < 33L) {
            return webViewBitmap
        }
        lastCaptureTime = now

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
