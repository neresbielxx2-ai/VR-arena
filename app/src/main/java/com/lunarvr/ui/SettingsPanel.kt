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
    var uiDistance: Float = 1.3f
    var uiScale: Float = 1.0f
    var mirrorLeftRight: Boolean = false
    var invertX: Boolean = false
    var invertY: Boolean = false

    // Performance Settings
    var economicMode: Boolean = false
    var targetFps: Int = 60
    var animationsEnabled: Boolean = true

    // Hand Tracking Settings
    var handTrackingEnabled: Boolean = true
    var handSensitivity: Float = 1.0f
    var showFingerRay: Boolean = true
    var markerRadius: Float = 0.025f

    val buttons = mutableListOf<AppButton>()

    init {
        setupSettingsButtons()
    }

    fun setupSettingsButtons() {
        buttons.clear()
        val zPos = -1.1f
        val startY = 0.25f
        val btnW = 0.45f
        val btnH = 0.09f
        val colLeft = -0.35f
        val colRight = 0.35f

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
            AppButton("btn_ht", "Mão/Ray: ${if (handTrackingEnabled) "LIGADO" else "DESLIG"}", colRight, startY, zPos, btnW, btnH) {
                handTrackingEnabled = !handTrackingEnabled
                setupSettingsButtons()
                onSettingChanged()
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

        // Row 3: Stereo Mirror & Ray Line
        buttons.add(
            AppButton("btn_mirror", "Espelhar Olhos: ${if (mirrorLeftRight) "SIM" else "NÃO"}", colLeft, startY - 0.24f, zPos, btnW, btnH) {
                mirrorLeftRight = !mirrorLeftRight
                vrSession.stereoCamera.mirrorLeftRight = mirrorLeftRight
                setupSettingsButtons()
                onSettingChanged()
            }
        )

        buttons.add(
            AppButton("btn_ray", "Linha do Dedo: ${if (showFingerRay) "LIGADA" else "DESLIG"}", colRight, startY - 0.24f, zPos, btnW, btnH) {
                showFingerRay = !showFingerRay
                setupSettingsButtons()
                onSettingChanged()
            }
        )

        // Row 4: UI Distance & Recenter
        buttons.add(
            AppButton("btn_dist", "Distância UI: ${String.format("%.1f", uiDistance)}m", colLeft, startY - 0.36f, zPos, btnW, btnH) {
                uiDistance = if (uiDistance >= 1.8f) 1.0f else (uiDistance + 0.2f)
                setupSettingsButtons()
                onSettingChanged()
            }
        )

        buttons.add(
            AppButton("btn_close_settings", "✖ Fechar Painel", colRight, startY - 0.36f, zPos, btnW, btnH) {
                isVisible = false
                onSettingChanged()
            }
        )
    }

    fun getSystemInfoText(report: HardwareReport): String {
        return """
            LUNAR VR v1.0.0
            Dispositivo: ${report.deviceModel}
            Sistema: ${report.osVersion}
            Memória RAM: ${report.totalRamMb} MB
            Giroscópio: ${if (report.hasGyroscope) "Presente" else "Ausente (Emulado)"}
            Acelerômetro: ${if (report.hasAccelerometer) "Presente" else "Ausente"}
            Câmera: ${if (report.hasCamera) "Pronta" else "Ausente"}
            Modo Atual: ${vrSession.performanceManager.currentLevel.name} (${vrSession.performanceManager.targetFps} FPS)
        """.trimIndent()
    }
}
