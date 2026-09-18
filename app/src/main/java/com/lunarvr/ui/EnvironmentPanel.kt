package com.lunarvr.ui

import com.lunarvr.environment.EnvironmentManager
import com.lunarvr.environment.VREnvironmentType

class EnvironmentPanel(
    private val envManager: EnvironmentManager,
    private val onEnvironmentChanged: () -> Unit
) {
    var isVisible: Boolean = false
    val buttons = mutableListOf<AppButton>()

    private var currentCenterX = 0f
    private var currentCenterY = 0.10f

    init {
        setupButtons(0f, 0.10f)
    }

    fun setupButtons(centerX: Float = currentCenterX, centerY: Float = currentCenterY) {
        currentCenterX = centerX
        currentCenterY = centerY
        buttons.clear()

        val zPos = -1.30f
        val startY = centerY + 0.14f
        val btnW = 0.46f
        val btnH = 0.09f
        val colLeft = centerX - 0.25f
        val colRight = centerX + 0.25f

        val envs = VREnvironmentType.values()

        // 2x2 grid of environment themes
        for (i in envs.indices) {
            val env = envs[i]
            val bx = if (i % 2 == 0) colLeft else colRight
            val by = startY - (i / 2) * 0.12f
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
        buttons.add(
            AppButton("btn_close_env", "✕ Fechar Cenários", centerX, startY - 0.26f, zPos, 0.50f, 0.08f) {
                isVisible = false
                onEnvironmentChanged()
            }
        )
    }
}
