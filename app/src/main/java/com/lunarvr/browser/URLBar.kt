package com.lunarvr.browser

import com.lunarvr.ui.AppButton

class URLBar(
    private val onUrlClick: () -> Unit,
    private val onBackClick: () -> Unit,
    private val onForwardClick: () -> Unit,
    private val onRefreshClick: () -> Unit,
    private val onHomeClick: () -> Unit
) {
    var displayUrl: String = "https://www.google.com"

    val buttons = mutableListOf<AppButton>()

    init {
        setupButtons(0f, 0.45f)
    }

    fun setupButtons(centerX: Float = 0f, centerY: Float = 0.45f) {
        buttons.clear()
        val zPos = -1.35f
        val yPos = centerY

        // Navigation controls
        buttons.add(AppButton("url_back", "◀", centerX - 0.48f, yPos, zPos, 0.08f, 0.08f) { onBackClick() })
        buttons.add(AppButton("url_fwd", "▶", centerX - 0.38f, yPos, zPos, 0.08f, 0.08f) { onForwardClick() })
        buttons.add(AppButton("url_reload", "↻", centerX - 0.28f, yPos, zPos, 0.08f, 0.08f) { onRefreshClick() })
        buttons.add(AppButton("url_home", "✦", centerX - 0.18f, yPos, zPos, 0.08f, 0.08f) { onHomeClick() })

        // URL address bar button (opens VR keyboard)
        buttons.add(AppButton("url_input", displayUrl, centerX + 0.20f, yPos, zPos, 0.60f, 0.08f) {
            onUrlClick()
        })
    }
}
