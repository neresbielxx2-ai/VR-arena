package com.lunarvr.ui

import com.lunarvr.environment.EnvironmentManager
import com.lunarvr.environment.VREnvironmentType

class EnvironmentPanel(
    private val envManager: EnvironmentManager,
    private val onEnvironmentChanged: () -> Unit,
    private val onCloseEnvironment: () -> Unit
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

        // Top-left distinct close '✕' button for the panel (does not overlap content)
        buttons.add(
            AppButton("btn_close_env_top_left", "✕", centerX - 0.49f, centerY + 0.31f, zPos, 0.08f, 0.08f) {
                isVisible = false
                onCloseEnvironment()
            }
        )

        val startY = centerY + 0.18f
        val btnW = 0.44f
        val btnH = 0.11f
        val colLeft = centerX - 0.24f
        val colRight = centerX + 0.24f

        val envs = VREnvironmentType.values()

        for (i in envs.indices) {
            val env = envs[i]
            val bx = if (i % 2 == 0) colLeft else colRight
            val by = startY - (i / 2) * 0.15f
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
    }
}
