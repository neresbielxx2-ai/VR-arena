package com.lunarvr.ui

import com.lunarvr.handtracking.BoundingBox3D
import com.lunarvr.handtracking.InteractableElement

open class AppButton(
    override val id: String,
    val label: String,
    var x: Float,
    var y: Float,
    var z: Float,
    var width: Float,
    var height: Float,
    private val onClickAction: () -> Unit
) : InteractableElement {

    var isHovered: Boolean = false
        private set
    var hoverProgress: Float = 0f
        private set

    override fun getBoundingBox3D(): BoundingBox3D {
        // Generous hitbox margin for comfortable fingertip aim and dwell click
        val paddingX = 0.04f
        val paddingY = 0.04f
        return BoundingBox3D(
            minX = x - width / 2.0f - paddingX,
            maxX = x + width / 2.0f + paddingX,
            minY = y - height / 2.0f - paddingY,
            maxY = y + height / 2.0f + paddingY,
            minZ = z - 0.2f,
            maxZ = z + 0.2f
        )
    }

    override fun onHoverEnter() {
        isHovered = true
        hoverProgress = 0f
    }

    override fun onHoverProgress(progress: Float) {
        hoverProgress = progress
    }

    override fun onHoverExit() {
        isHovered = false
        hoverProgress = 0f
    }

    override fun onClick() {
        onClickAction.invoke()
    }
}
