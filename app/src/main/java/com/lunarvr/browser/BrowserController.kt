package com.lunarvr.browser

data class BrowserState(
    val currentUrl: String = "https://html.duckduckgo.com/html/",
    val title: String = "DuckDuckGo VR",
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Int = 0
)

interface BrowserNavigationListener {
    fun onStateChanged(state: BrowserState)
    fun onPageStarted(url: String)
    fun onPageFinished(url: String)
}

class BrowserController {

    var listener: BrowserNavigationListener? = null
    var currentState: BrowserState = BrowserState()
        private set

    fun updateUrl(url: String) {
        val sanitized = sanitizeUrl(url)
        currentState = currentState.copy(currentUrl = sanitized, isLoading = true)
        listener?.onStateChanged(currentState)
    }

    fun updateProgress(progress: Int) {
        currentState = currentState.copy(progress = progress, isLoading = progress < 100)
        listener?.onStateChanged(currentState)
    }

    fun updateNavigationState(canBack: Boolean, canForward: Boolean, title: String? = null) {
        currentState = currentState.copy(
            canGoBack = canBack,
            canGoForward = canForward,
            title = title ?: currentState.title
        )
        listener?.onStateChanged(currentState)
    }

    private fun sanitizeUrl(input: String): String {
        val trimmed = input.trim()
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
            else -> "https://duckduckgo.com/?q=" + java.net.URLEncoder.encode(trimmed, "UTF-8")
        }
    }
}
