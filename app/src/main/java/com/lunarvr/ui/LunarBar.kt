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
    var vrStatus: String = "3DoF Ativo"

    // World position of the bar in spherical coordinates: yawDeg, height Y, distance Z
    var yawDeg: Float = 0.0f
    var posY: Float = -0.28f
    var radiusZ: Float = 1.35f

    // Cartesian coordinates
    var posX: Float = 0.0f
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

        // 4 actions
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

        // Meta Quest style pill drag handle located right underneath the main bar
        grabHandle = GrabHandle(
            id = "grab_lunar_bar",
            x = posX,
            y = posY - 0.15f,
            z = posZ,
            width = 0.42f,
            height = 0.06f
        )
    }

    fun setSphericalPosition(newYawDeg: Float, newHeightY: Float, newDist: Float = radiusZ) {
        yawDeg = newYawDeg
        posY = newHeightY
        radiusZ = newDist

        val rad = Math.toRadians(newYawDeg.toDouble())
        posX = (newDist * Math.sin(rad)).toFloat()
        posZ = (-newDist * Math.cos(rad)).toFloat()

        // Update button world positions relative to tangent and normal vectors
        val cosA = Math.cos(rad).toFloat()
        val sinA = Math.sin(rad).toFloat()

        fun setRelPos(btn: AppButton, offsetX: Float) {
            btn.x = posX + offsetX * cosA
            btn.y = posY
            btn.z = posZ + offsetX * sinA
        }

        setRelPos(buttons[0], -0.38f)
        setRelPos(buttons[1], -0.13f)
        setRelPos(buttons[2], 0.13f)
        setRelPos(buttons[3], 0.38f)

        grabHandle.x = posX
        grabHandle.y = posY - 0.15f
        grabHandle.z = posZ
    }

    fun updateClock() {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        currentTimeString = sdf.format(Date())
    }
}
