package com.lunarvr.ui

import com.lunarvr.system.HardwareReport
import com.lunarvr.system.PerformanceLevel
import com.lunarvr.vr.VRSession

class SettingsPanel(
    private val vrSession: VRSession,
    val onSettingChanged: () -> Unit
) {
    var isVisible: Boolean = false

    // VR Settings
    var vrQuality: String = "Alta"
    var uiDistance: Float = 1.35f
    var mirrorLeftRight: Boolean = false
    var invertX: Boolean = false
    var invertY: Boolean = false

    // Performance Settings
    var economicMode: Boolean = false

    val buttons = mutableListOf<AppButton>()

    init {
        setupSettingsButtons()
    }

    fun setupSettingsButtons() {
        buttons.clear()
        val zPos = -1.30f
        val startY = 0.22f
        val btnW = 0.46f
        val btnH = 0.09f
        val colLeft = -0.36f
        val colRight = 0.36f

        // Row 1: Mode toggles
        buttons.add(
            AppButton("btn_eco", "Modo Eco: ${if (economicMode) "LIGADO" else "DESLIG"}", colLeft, startY, zPos, btnW, btnH) {
                economicMode = !economicMode
                vrSession.performanceManager.applyLevel(
                    if (economicMode) PerformanceLevel.ECONOMIC else PerformanceLevel.QUALITY
                )
                setupSettingsButtons()
                onSettingChanged()
            }
        )

        buttons.add(
            AppButton("btn_recenter_settings", "🎯 Centralizar Visão", colRight, startY, zPos, btnW, btnH) {
                vrSession.recenterManager.triggerRecenter()
            }
        )

        // Row 2: Invert Axis
        buttons.add(
            AppButton("btn_inv_x", "Inverter Eixo X: ${if (invertX) "SIM" else "NÃO"}", colLeft, startY - 0.12f, zPos, btnW, btnH) {
                invertX = !invertX
                vrSession.headTracking.invertYaw = invertX
                setupSettingsButtons()
                onSettingChanged()
            }
        )

        buttons.add(
            AppButton("btn_inv_y", "Inverter Eixo Y: ${if (invertY) "SIM" else "NÃO"}", colRight, startY - 0.12f, zPos, btnW, btnH) {
                invertY = !invertY
                vrSession.headTracking.invertPitch = invertY
                setupSettingsButtons()
                onSettingChanged()
            }
        )

        // Row 3: Stereo Mirror
        buttons.add(
            AppButton("btn_mirror", "Espelhar Olhos: ${if (mirrorLeftRight) "SIM" else "NÃO"}", colLeft, startY - 0.24f, zPos, btnW, btnH) {
                mirrorLeftRight = !mirrorLeftRight
                vrSession.stereoCamera.mirrorLeftRight = mirrorLeftRight
                setupSettingsButtons()
                onSettingChanged()
            }
        )

        buttons.add(
            AppButton("btn_close_settings", "✖ Fechar Painel", colRight, startY - 0.24f, zPos, btnW, btnH) {
                isVisible = false
                onSettingChanged()
            }
        )
    }

    fun getSystemInfoText(report: HardwareReport): String {
        return """
            LUNAR VR v1.0.1
            Dispositivo: ${report.deviceModel}
            Sistema: ${report.osVersion}
            Memória RAM: ${report.totalRamMb} MB
            Giroscópio: ${if (report.hasGyroscope) "Presente (VR 3DoF Ativo)" else "Ausente"}
            Acelerômetro: ${if (report.hasAccelerometer) "Presente" else "Ausente"}
            Modo Atual: ${vrSession.performanceManager.currentLevel.name} (${vrSession.performanceManager.targetFps} FPS)
        """.trimIndent()
    }
}
