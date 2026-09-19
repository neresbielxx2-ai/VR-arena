package com.lunarvr.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class BarStyle(val displayName: String) {
    META_QUEST("Meta Quest 3S"),
    LUNAR_COSMIC("Lunar Cósmico"),
    MINIMAL_CYBER("Cyberpunk")
}

enum class BarColorTheme(
    val displayName: String,
    val bgHex: String,
    val borderHex: String,
    val primaryAccent: String,
    val secondaryAccent: String
) {
    NEBULA_PURPLE("Ametista Cósmico", "#F5121626", "#6366F1", "#A855F7", "#818CF8"),
    CYBER_EMERALD("Esmeralda Lunar", "#F50C1B17", "#10B981", "#34D399", "#059669"),
    SOLAR_AMBER("Âmbar Solar", "#F51C170E", "#F59E0B", "#FBBF24", "#D97706"),
    NEON_ROSE("Neon Sunset", "#F51F101A", "#EC4899", "#F472B6", "#DB2777"),
    DEEP_OCEAN("Oceano Glacial", "#F50E1826", "#0284C7", "#38BDF8", "#0369A1")
}

enum class LunarNavDestination {
    HOME,
    BROWSER,
    SETTINGS,
    ENVIRONMENTS
}

class LunarBar(
    private val onNavigate: (LunarNavDestination) -> Unit,
    private val onRecenter: () -> Unit
) {
    var currentTimeString: String = "12:00"
        private set
    var batteryPercentage: Int = 100
    var vrStatus: String = "3DoF Ativo"

    var currentStyle: BarStyle = BarStyle.META_QUEST
    var currentColorTheme: BarColorTheme = BarColorTheme.NEBULA_PURPLE

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

        val btnW = 0.18f
        val btnH = 0.13f

        // 5 sleek actions: Início, Navegador, Cenários, Centralizar, Ajustes
        buttons.add(
            AppButton("btn_home", "Início", posX - 0.40f, posY, posZ, btnW, btnH) {
                onNavigate(LunarNavDestination.HOME)
            }
        )

        buttons.add(
            AppButton("btn_browser", "Navegador", posX - 0.20f, posY, posZ, btnW, btnH) {
                onNavigate(LunarNavDestination.BROWSER)
            }
        )

        buttons.add(
            AppButton("btn_environments", "Cenários", posX, posY, posZ, btnW, btnH) {
                onNavigate(LunarNavDestination.ENVIRONMENTS)
            }
        )

        buttons.add(
            AppButton("btn_recenter", "Centralizar", posX + 0.20f, posY, posZ, btnW, btnH) {
                onRecenter()
            }
        )

        buttons.add(
            AppButton("btn_settings", "Configuração", posX + 0.40f, posY, posZ, btnW, btnH) {
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

        setRelPos(buttons[0], -0.40f)
        setRelPos(buttons[1], -0.20f)
        setRelPos(buttons[2], 0.00f)
        setRelPos(buttons[3], 0.20f)
        setRelPos(buttons[4], 0.40f)

        grabHandle.x = posX
        grabHandle.y = posY - 0.15f
        grabHandle.z = posZ
    }

    fun updateClock() {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        currentTimeString = sdf.format(Date())
    }
}
