package com.lunarvr.browser

import com.lunarvr.ui.AppButton

class URLBar(
    private val onUrlClick: () -> Unit,
    private val onBackClick: () -> Unit,
    private val onForwardClick: () -> Unit,
    private val onRefreshClick: () -> Unit,
    private val onHomeClick: () -> Unit,
    private val onResizeClick: () -> Unit
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

        // Navigation controls
        buttons.add(AppButton("url_back", "◀", centerX - 0.65f, yPos, zPos, 0.08f, 0.08f) { onBackClick() })
        buttons.add(AppButton("url_fwd", "▶", centerX - 0.55f, yPos, zPos, 0.08f, 0.08f) { onForwardClick() })
        buttons.add(AppButton("url_reload", "↻", centerX - 0.45f, yPos, zPos, 0.08f, 0.08f) { onRefreshClick() })
        buttons.add(AppButton("url_home", "✦", centerX - 0.35f, yPos, zPos, 0.08f, 0.08f) { onHomeClick() })

        // URL address bar button (spacious width: 0.80m)
        buttons.add(AppButton("url_input", displayUrl, centerX + 0.12f, yPos, zPos, 0.80f, 0.08f) {
            onUrlClick()
        })

        // Resize button (+ Tamanho da Aba)
        buttons.add(AppButton("url_resize", "⤢ $scaleName", centerX + 0.65f, yPos, zPos, 0.18f, 0.08f) {
            onResizeClick()
        })
    }
}
