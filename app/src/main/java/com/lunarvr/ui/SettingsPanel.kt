package com.lunarvr.ui

import com.lunarvr.system.HardwareReport
import com.lunarvr.system.PerformanceLevel
import com.lunarvr.vr.VRSession

class SettingsPanel(
    private val vrSession: VRSession,
    val onSettingChanged: () -> Unit
) {
    var isVisible: Boolean = false

    // Dwell Time Options: 1.0s, 1.5s, 2.0s, 2.5s
    private val dwellSpeeds = listOf(1000L, 1500L, 2000L, 2500L)
    var currentDwellIndex = 2 // Default: 2000L (2.0s)

    // VR Settings
    var vrQuality: String = "Alta"
    var uiDistance: Float = 1.35f
    var mirrorLeftRight: Boolean = false
    var invertX: Boolean = false
    var invertY: Boolean = false

    // Performance Settings
    var economicMode: Boolean = false

    val buttons = mutableListOf<AppButton>()

    private var currentCenterX = 0f
    private var currentCenterY = 0.10f

    init {
        setupButtons(0f, 0.10f)
    }

    fun getDwellTimeMs(): Long = dwellSpeeds[currentDwellIndex]

    fun setupButtons(centerX: Float = currentCenterX, centerY: Float = currentCenterY) {
        currentCenterX = centerX
        currentCenterY = centerY
        buttons.clear()

        val zPos = -1.30f
        val startY = centerY + 0.12f
        val btnW = 0.46f
        val btnH = 0.09f
        val colLeft = centerX - 0.25f
        val colRight = centerX + 0.25f

        val dwellSec = dwellSpeeds[currentDwellIndex] / 1000f

        // Row 1: Click response dwell speed & Recenter
        buttons.add(
            AppButton("btn_dwell_speed", "Tempo Clique Mira: ${dwellSec}s", colLeft, startY, zPos, btnW, btnH) {
                currentDwellIndex = (currentDwellIndex + 1) % dwellSpeeds.size
                setupButtons()
                onSettingChanged()
            }
        )

        buttons.add(
            AppButton("btn_recenter_settings", "Centralizar Visão", colRight, startY, zPos, btnW, btnH) {
                vrSession.recenterManager.triggerRecenter()
            }
        )

        // Row 2: Invert Axis
        buttons.add(
            AppButton("btn_inv_x", "Inverter Eixo X: ${if (invertX) "SIM" else "NÃO"}", colLeft, startY - 0.12f, zPos, btnW, btnH) {
                invertX = !invertX
                vrSession.headTracking.invertYaw = invertX
                setupButtons()
                onSettingChanged()
            }
        )

        buttons.add(
            AppButton("btn_inv_y", "Inverter Eixo Y: ${if (invertY) "SIM" else "NÃO"}", colRight, startY - 0.12f, zPos, btnW, btnH) {
                invertY = !invertY
                vrSession.headTracking.invertPitch = invertY
                setupButtons()
                onSettingChanged()
            }
        )

        // Row 3: Performance mode & Stereo Mirror
        buttons.add(
            AppButton("btn_eco", "Modo Eco: ${if (economicMode) "LIGADO" else "DESLIG"}", colLeft, startY - 0.24f, zPos, btnW, btnH) {
                economicMode = !economicMode
                vrSession.performanceManager.applyLevel(
                    if (economicMode) PerformanceLevel.ECONOMIC else PerformanceLevel.QUALITY
                )
                setupButtons()
                onSettingChanged()
            }
        )

        buttons.add(
            AppButton("btn_close_settings", "Fechar Painel", colRight, startY - 0.24f, zPos, btnW, btnH) {
                isVisible = false
                onSettingChanged()
            }
        )
    }

    fun getSystemInfoText(report: HardwareReport): String {
        return """
            LUNAR VR v1.0.3
            Dispositivo: ${report.deviceModel}  |  Android: ${report.osVersion}
            Memória RAM: ${report.totalRamMb} MB  |  ${vrSession.performanceManager.currentLevel.name} (${vrSession.performanceManager.targetFps} FPS)
            Tempo de Resposta do Clique: ${getDwellTimeMs() / 1000f}s
        """.trimIndent()
    }
}
