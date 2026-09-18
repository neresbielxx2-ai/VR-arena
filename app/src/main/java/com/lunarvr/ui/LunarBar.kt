package com.lunarvr.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LunarNavDestination {
    HOME,
    BROWSER,
    SETTINGS
}

class LunarBar(
    private val onNavigate: (LunarNavDestination) -> Unit,
    private val onRecenter: () -> Unit
) {
    var currentTimeString: String = "12:00"
        private set
    var batteryPercentage: Int = 100
    var wifiStatus: String = "Wi-Fi OK"
    var vrStatus: String = "3DoF Ativo"

    val buttons = mutableListOf<AppButton>()

    init {
        setupBarButtons()
    }

    private fun setupBarButtons() {
        buttons.clear()

        // Bar sits at bottom front: y = -0.35f, z = -1.2f
        val zPos = -1.2f
        val yPos = -0.38f
        val btnW = 0.22f
        val btnH = 0.12f

        buttons.add(
            AppButton("btn_home", "✦ Início", -0.36f, yPos, zPos, btnW, btnH) {
                onNavigate(LunarNavDestination.HOME)
            }
        )

        buttons.add(
            AppButton("btn_browser", "🌐 Navegador", -0.12f, yPos, zPos, btnW, btnH) {
                onNavigate(LunarNavDestination.BROWSER)
            }
        )

        buttons.add(
            AppButton("btn_recenter", "🎯 Centralizar", 0.12f, yPos, zPos, btnW, btnH) {
                onRecenter()
            }
        )

        buttons.add(
            AppButton("btn_settings", "⚙ Ajustes", 0.36f, yPos, zPos, btnW, btnH) {
                onNavigate(LunarNavDestination.SETTINGS)
            }
        )
    }

    fun updateClock() {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        currentTimeString = sdf.format(Date())
    }
}
