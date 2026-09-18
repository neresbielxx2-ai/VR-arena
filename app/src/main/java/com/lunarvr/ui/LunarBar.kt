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

    fun setupBarButtons() {
        buttons.clear()

        // Positioned comfortably in lower frontal view: y = -0.32f, z = -1.25f
        val zPos = -1.25f
        val yPos = -0.32f
        val btnW = 0.24f
        val btnH = 0.14f

        buttons.add(
            AppButton("btn_home", "✦ Início", -0.42f, yPos, zPos, btnW, btnH) {
                onNavigate(LunarNavDestination.HOME)
            }
        )

        buttons.add(
            AppButton("btn_browser", "🌐 Navegador", -0.14f, yPos, zPos, btnW, btnH) {
                onNavigate(LunarNavDestination.BROWSER)
            }
        )

        buttons.add(
            AppButton("btn_recenter", "🎯 Centralizar", 0.14f, yPos, zPos, btnW, btnH) {
                onRecenter()
            }
        )

        buttons.add(
            AppButton("btn_settings", "⚙ Ajustes", 0.42f, yPos, zPos, btnW, btnH) {
                onNavigate(LunarNavDestination.SETTINGS)
            }
        )
    }

    fun updateClock() {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        currentTimeString = sdf.format(Date())
    }
}
