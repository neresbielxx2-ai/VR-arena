package com.lunarvr.browser

import com.lunarvr.ui.AppButton

class URLBar(
    private val onUrlClick: () -> Unit,
    private val onBackClick: () -> Unit,
    private val onForwardClick: () -> Unit,
    private val onRefreshClick: () -> Unit,
    private val onHomeClick: () -> Unit,
    private val onResizeClick: () -> Unit,
    private val onCloseClick: () -> Unit
) {
    var displayUrl: String = "https://html.duckduckgo.com/html/"
    var scaleName: String = "1.0x"

    val buttons = mutableListOf<AppButton>()

    init {
        setupButtons(0f, 0.58f)
    }

    fun setupButtons(centerX: Float = 0f, centerY: Float = 0.58f) {
        buttons.clear()
        val zPos = -1.45f
        val yPos = centerY

        // Close button at top-left corner
        buttons.add(AppButton("url_close_top_left", "✕", centerX - 0.75f, yPos, zPos, 0.06f, 0.07f) { onCloseClick() })

        // Navigation controls (Back, Forward, Reload, Home)
        buttons.add(AppButton("url_back", "◀", centerX - 0.66f, yPos, zPos, 0.06f, 0.07f) { onBackClick() })
        buttons.add(AppButton("url_fwd", "▶", centerX - 0.58f, yPos, zPos, 0.06f, 0.07f) { onForwardClick() })
        buttons.add(AppButton("url_reload", "↻", centerX - 0.50f, yPos, zPos, 0.06f, 0.07f) { onRefreshClick() })
        buttons.add(AppButton("url_home", "✦", centerX - 0.42f, yPos, zPos, 0.06f, 0.07f) { onHomeClick() })

        // Clean central URL input field (capsule address bar)
        buttons.add(AppButton("url_input", displayUrl, centerX + 0.05f, yPos, zPos, 0.82f, 0.075f) {
            onUrlClick()
        })

        // Corner Resize button
        buttons.add(AppButton("url_resize", "⤢ $scaleName", centerX + 0.72f, yPos, zPos, 0.10f, 0.07f) {
            onResizeClick()
        })
    }
}
