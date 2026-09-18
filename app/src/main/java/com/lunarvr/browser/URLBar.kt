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
        setupButtons()
    }

    fun setupButtons() {
        buttons.clear()
        val zPos = -1.25f
        val yPos = 0.48f

        // Navigation controls
        buttons.add(AppButton("url_back", "◀", -0.55f, yPos, zPos, 0.08f, 0.08f) { onBackClick() })
        buttons.add(AppButton("url_fwd", "▶", -0.45f, yPos, zPos, 0.08f, 0.08f) { onForwardClick() })
        buttons.add(AppButton("url_reload", "↻", -0.35f, yPos, zPos, 0.08f, 0.08f) { onRefreshClick() })
        buttons.add(AppButton("url_home", "🏠", -0.25f, yPos, zPos, 0.08f, 0.08f) { onHomeClick() })

        // URL address bar button (opens VR keyboard)
        buttons.add(AppButton("url_input", "🔍  $displayUrl", 0.20f, yPos, zPos, 0.70f, 0.08f) {
            onUrlClick()
        })
    }
}
