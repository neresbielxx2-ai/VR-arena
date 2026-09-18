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
        return BoundingBox3D(
            minX = x - width / 2.0f,
            maxX = x + width / 2.0f,
            minY = y - height / 2.0f,
            maxY = y + height / 2.0f,
            minZ = z - 0.05f,
            maxZ = z + 0.05f
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
