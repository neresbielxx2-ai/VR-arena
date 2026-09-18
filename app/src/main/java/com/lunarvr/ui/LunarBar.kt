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

    // World position of the bar
    var posX: Float = 0.0f
    var posY: Float = -0.26f
    var posZ: Float = -1.35f

    // Interactive buttons and grab handle
    val buttons = mutableListOf<AppButton>()
    lateinit var grabHandle: GrabHandle

    init {
        setupBarButtons()
    }

    fun setupBarButtons() {
        buttons.clear()

        val btnW = 0.23f
        val btnH = 0.13f

        // 4 sleek, modern actions
        buttons.add(
            AppButton("btn_home", "Início", posX - 0.38f, posY, posZ, btnW, btnH) {
                onNavigate(LunarNavDestination.HOME)
            }
        )

        buttons.add(
            AppButton("btn_browser", "Navegador", posX - 0.13f, posY, posZ, btnW, btnH) {
                onNavigate(LunarNavDestination.BROWSER)
            }
        )

        buttons.add(
            AppButton("btn_recenter", "Centralizar", posX + 0.13f, posY, posZ, btnW, btnH) {
                onRecenter()
            }
        )

        buttons.add(
            AppButton("btn_settings", "Ajustes", posX + 0.38f, posY, posZ, btnW, btnH) {
                onNavigate(LunarNavDestination.SETTINGS)
            }
        )

        // Grab handle located right underneath the main bar
        grabHandle = GrabHandle(
            id = "grab_lunar_bar",
            x = posX,
            y = posY - 0.15f,
            z = posZ,
            width = 0.40f,
            height = 0.06f
        )
    }

    fun updatePosition(newX: Float, newY: Float) {
        posX = newX
        posY = newY
        // Update children button positions
        buttons[0].x = posX - 0.38f
        buttons[0].y = posY
        buttons[1].x = posX - 0.13f
        buttons[1].y = posY
        buttons[2].x = posX + 0.13f
        buttons[2].y = posY
        buttons[3].x = posX + 0.38f
        buttons[3].y = posY

        grabHandle.x = posX
        grabHandle.y = posY - 0.15f
    }

    fun updateClock() {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        currentTimeString = sdf.format(Date())
    }
}
