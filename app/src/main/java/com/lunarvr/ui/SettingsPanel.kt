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

    var is6Dof: Boolean = configManager.is6DofEnabled
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

        // Top-left distinct close '✕' button for the panel
        buttons.add(
            AppButton("btn_close_settings_top_left", "✕", centerX - 0.49f, centerY + 0.35f, zPos, 0.08f, 0.08f) {
                isVisible = false
                onCloseSettings()
            }
        )

        // Resizing +/- buttons
        buttons.add(
            AppButton("btn_settings_scale_down", "－", centerX + 0.38f, centerY + 0.35f, zPos, 0.07f, 0.07f) {
                // Handled in VRRenderer
            }
        )
        buttons.add(
            AppButton("btn_settings_scale_up", "＋", centerX + 0.46f, centerY + 0.35f, zPos, 0.07f, 0.07f) {
                // Handled in VRRenderer
            }
        )

        val startY = centerY + 0.22f
        val btnW = 0.46f
        val btnH = 0.085f
        val colLeft = centerX - 0.25f
        val colRight = centerX + 0.25f

        // Row 1 (FIRST OPTION): 3DoF / 6DoF Toggle!
        val dofText = if (is6Dof) "✦ Modo Atual: 6DoF (Clique para 3DoF)" else "✦ Modo Atual: 3DoF (Clique para 6DoF)"
        buttons.add(
            AppButton("btn_dof_toggle", dofText, centerX, startY, zPos, 0.94f, btnH) {
                is6Dof = !is6Dof
                configManager.is6DofEnabled = is6Dof
                vrSession.headTracking.is6DofEnabled = is6Dof
                setupButtons()
                onSettingChanged()
            }
        )

        val row2Y = startY - 0.095f
        // Row 2: Tempo Clique Mira & Centralizar
        val dwellSec = getDwellTimeMs() / 1000f
        buttons.add(
            AppButton("btn_dwell_speed", "Tempo Clique Mira: ${dwellSec}s", colLeft, row2Y, zPos, btnW, btnH) {
                currentDwellIndex = (currentDwellIndex + 1) % dwellSpeeds.size
                configManager.dwellIndex = currentDwellIndex
                setupButtons()
                onSettingChanged()
            }
        )

        buttons.add(
            AppButton("btn_recenter_settings", "Centralizar Visão", colRight, row2Y, zPos, btnW, btnH) {
                vrSession.recenterManager.triggerRecenter()
            }
        )

        val row3Y = row2Y - 0.095f
        // Row 3: Invert Axis
        buttons.add(
            AppButton("btn_inv_x", "Inverter Eixo X: ${if (invertX) "SIM" else "NÃO"}", colLeft, row3Y, zPos, btnW, btnH) {
                invertX = !invertX
                configManager.invertX = invertX
                vrSession.headTracking.invertYaw = invertX
                setupButtons()
                onSettingChanged()
            }
        )

        buttons.add(
            AppButton("btn_inv_y", "Inverter Eixo Y: ${if (invertY) "SIM" else "NÃO"}", colRight, row3Y, zPos, btnW, btnH) {
                invertY = !invertY
                configManager.invertY = invertY
                vrSession.headTracking.invertPitch = invertY
                setupButtons()
                onSettingChanged()
            }
        )

        val row4Y = row3Y - 0.095f
        // Row 4: Performance mode & Reset to Defaults
        buttons.add(
            AppButton("btn_eco", "Modo Eco: ${if (economicMode) "LIGADO" else "DESLIG"}", colLeft, row4Y, zPos, btnW, btnH) {
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
            AppButton("btn_reset_defaults", "↺ Resetar Configurações", colRight, row4Y, zPos, btnW, btnH) {
                configManager.resetToDefaults()
                currentDwellIndex = 2
                is6Dof = false
                invertX = false
                invertY = false
                economicMode = false
                vrSession.headTracking.is6DofEnabled = false
                vrSession.headTracking.invertYaw = false
                vrSession.headTracking.invertPitch = false
                onResetToDefaults()
                setupButtons()
                onSettingChanged()
            }
        )
    }

    fun getSystemInfoText(report: HardwareReport): String {
        val dofMode = if (is6Dof) "6DoF (Rotação + Translação)" else "3DoF (Giroscópio / Orientação)"
        return """
            LUNAR VR v1.0.6  |  Rastreamento: $dofMode
            Dispositivo: ${report.deviceModel}  |  Android: ${report.osVersion}
            Memória RAM: ${report.totalRamMb} MB  |  ${vrSession.performanceManager.currentLevel.name} (${vrSession.performanceManager.targetFps} FPS)
            Tempo de Clique Mira: ${getDwellTimeMs() / 1000f}s  |  Ajustes Rápidos
        """.trimIndent()
    }
}
