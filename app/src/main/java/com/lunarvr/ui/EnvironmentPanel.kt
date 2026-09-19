package com.lunarvr.ui

import com.lunarvr.environment.EnvironmentManager
import com.lunarvr.environment.VREnvironmentType

class EnvironmentPanel(
    private val envManager: EnvironmentManager,
    private val onEnvironmentChanged: () -> Unit
) {
    var isVisible: Boolean = false
    val buttons = mutableListOf<AppButton>()
    val closeButton: AppButton

    private var currentCenterX = 0f
    private var currentCenterY = 0.10f

    init {
        closeButton = AppButton("btn_close_env", "✕ Fechar Cenários", 0f, -0.20f, -1.30f, 0.50f, 0.08f) {
            isVisible = false
            onEnvironmentChanged()
        }
        setupButtons(0f, 0.10f)
    }

    fun setupButtons(centerX: Float = currentCenterX, centerY: Float = currentCenterY) {
        currentCenterX = centerX
        currentCenterY = centerY
        buttons.clear()

        val zPos = -1.30f
        val startY = centerY + 0.14f
        val btnW = 0.46f
        val btnH = 0.10f
        val colLeft = centerX - 0.25f
        val colRight = centerX + 0.25f

        val envs = VREnvironmentType.values()

        // 2x2 grid of environment themes (Generous, accessible click zones)
        for (i in envs.indices) {
            val env = envs[i]
            val bx = if (i % 2 == 0) colLeft else colRight
            val by = startY - (i / 2) * 0.14f
            val isCurrent = (env == envManager.currentEnvironment)
            val label = "${if (isCurrent) "✓ " else ""}${env.displayName}"

            buttons.add(
                AppButton("btn_env_${env.name}", label, bx, by, zPos, btnW, btnH) {
                    envManager.setEnvironment(env)
                    setupButtons(currentCenterX, currentCenterY)
                    onEnvironmentChanged()
                }
            )
        }

        // Close button at bottom
        closeButton.x = centerX
        closeButton.y = startY - 0.30f
        closeButton.z = zPos
        buttons.add(closeButton)
    }
}
