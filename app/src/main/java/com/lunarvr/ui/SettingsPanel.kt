package com.lunarvr.ui

import com.lunarvr.system.ConfigManager
import com.lunarvr.system.HardwareReport
import com.lunarvr.system.PerformanceLevel
import com.lunarvr.vr.VRSession

class SettingsPanel(
    private val vrSession: VRSession,
    private val lunarBar: LunarBar,
    val configManager: ConfigManager,
    val onResetToDefaults: () -> Unit,
    val onCloseSettings: () -> Unit,
    val onSettingChanged: () -> Unit
) {
    var isVisible: Boolean = false

    private val dwellSpeeds = listOf(1000L, 1500L, 2000L, 2500L)
    var currentDwellIndex = configManager.dwellIndex

    var vrQuality: String = "Alta"
    var uiDistance: Float = 1.35f
    var mirrorLeftRight: Boolean = false
    var invertX: Boolean = configManager.invertX
    var invertY: Boolean = configManager.invertY

    var economicMode: Boolean = configManager.economicMode

    val buttons = mutableListOf<AppButton>()

    private var currentCenterX = 0f
    private var currentCenterY = 0.10f

    init {
        setupButtons(0f, 0.10f)
    }

    fun getDwellTimeMs(): Long = dwellSpeeds[currentDwellIndex.coerceIn(0, dwellSpeeds.size - 1)]

    fun setupButtons(centerX: Float = currentCenterX, centerY: Float = currentCenterY) {
        currentCenterX = centerX
        currentCenterY = centerY
        buttons.clear()

        val zPos = -1.30f

        // Top-left distinct close '✕' button for the panel (does not overlap content)
        buttons.add(
            AppButton("btn_close_settings_top_left", "✕", centerX - 0.49f, centerY + 0.35f, zPos, 0.08f, 0.08f) {
                isVisible = false
                onCloseSettings()
            }
        )

        val startY = centerY + 0.18f
        val btnW = 0.46f
        val btnH = 0.08f
        val colLeft = centerX - 0.25f
        val colRight = centerX + 0.25f

        val dwellSec = getDwellTimeMs() / 1000f

        // Row 1: Click response dwell speed & Recenter
        buttons.add(
            AppButton("btn_dwell_speed", "Tempo Clique Mira: ${dwellSec}s", colLeft, startY, zPos, btnW, btnH) {
                currentDwellIndex = (currentDwellIndex + 1) % dwellSpeeds.size
                configManager.dwellIndex = currentDwellIndex
                setupButtons()
                onSettingChanged()
            }
        )

        buttons.add(
            AppButton("btn_recenter_settings", "Centralizar Visão", colRight, startY, zPos, btnW, btnH) {
                vrSession.recenterManager.triggerRecenter()
            }
        )

        // Row 2: Style of Lunar Bar & Color Theme
        buttons.add(
            AppButton("btn_bar_style", "Estilo Barra: ${lunarBar.currentStyle.displayName}", colLeft, startY - 0.095f, zPos, btnW, btnH) {
                val styles = BarStyle.values()
                val nextIdx = (lunarBar.currentStyle.ordinal + 1) % styles.size
                lunarBar.currentStyle = styles[nextIdx]
                configManager.barStyleOrdinal = nextIdx
                setupButtons()
                onSettingChanged()
            }
        )

        buttons.add(
            AppButton("btn_bar_color", "Cor Barra: ${lunarBar.currentColorTheme.displayName}", colRight, startY - 0.095f, zPos, btnW, btnH) {
                val themes = BarColorTheme.values()
                val nextIdx = (lunarBar.currentColorTheme.ordinal + 1) % themes.size
                lunarBar.currentColorTheme = themes[nextIdx]
                configManager.barColorOrdinal = nextIdx
                setupButtons()
                onSettingChanged()
            }
        )

        // Row 3: Invert Axis
        buttons.add(
            AppButton("btn_inv_x", "Inverter Eixo X: ${if (invertX) "SIM" else "NÃO"}", colLeft, startY - 0.19f, zPos, btnW, btnH) {
                invertX = !invertX
                configManager.invertX = invertX
                vrSession.headTracking.invertYaw = invertX
                setupButtons()
                onSettingChanged()
            }
        )

        buttons.add(
            AppButton("btn_inv_y", "Inverter Eixo Y: ${if (invertY) "SIM" else "NÃO"}", colRight, startY - 0.19f, zPos, btnW, btnH) {
                invertY = !invertY
                configManager.invertY = invertY
                vrSession.headTracking.invertPitch = invertY
                setupButtons()
                onSettingChanged()
            }
        )

        // Row 4: Performance mode & Reset to Defaults
        buttons.add(
            AppButton("btn_eco", "Modo Eco: ${if (economicMode) "LIGADO" else "DESLIG"}", colLeft, startY - 0.285f, zPos, btnW, btnH) {
                economicMode = !economicMode
                configManager.economicMode = economicMode
                vrSession.performanceManager.applyLevel(
                    if (economicMode) PerformanceLevel.ECONOMIC else PerformanceLevel.QUALITY
                )
                setupButtons()
                onSettingChanged()
            }
        )

        // Reset all settings to defaults
        buttons.add(
            AppButton("btn_reset_defaults", "↺ Resetar Configurações", colRight, startY - 0.285f, zPos, btnW, btnH) {
                configManager.resetToDefaults()
                currentDwellIndex = 2
                invertX = false
                invertY = false
                economicMode = false
                vrSession.headTracking.invertYaw = false
                vrSession.headTracking.invertPitch = false
                lunarBar.currentStyle = BarStyle.META_QUEST
                lunarBar.currentColorTheme = BarColorTheme.DEEP_OCEAN
                onResetToDefaults()
                setupButtons()
                onSettingChanged()
            }
        )
    }

    fun getSystemInfoText(report: HardwareReport): String {
        return """
            LUNAR VR v1.0.5  |  Meta Quest Experience
            Dispositivo: ${report.deviceModel}  |  Android: ${report.osVersion}
            Memória RAM: ${report.totalRamMb} MB  |  ${vrSession.performanceManager.currentLevel.name} (${vrSession.performanceManager.targetFps} FPS)
            Tempo de Clique Mira: ${getDwellTimeMs() / 1000f}s  |  Estilo: ${lunarBar.currentStyle.displayName} (${lunarBar.currentColorTheme.displayName})
        """.trimIndent()
    }
}
