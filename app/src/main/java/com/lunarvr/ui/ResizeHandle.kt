package com.lunarvr.ui

import android.os.SystemClock
import com.lunarvr.handtracking.BoundingBox3D
import com.lunarvr.handtracking.InteractableElement

class ResizeHandle(
    override val id: String,
    var x: Float,
    var y: Float,
    var z: Float,
    var width: Float = 0.12f,
    var height: Float = 0.40f
) : InteractableElement {

    var isHovered: Boolean = false
        private set
    var hoverProgress: Float = 0f
        private set

    var isDragging: Boolean = false
        private set

    var onResizeDelta: ((Float) -> Unit)? = null
    var cooldownUntilTime: Long = 0

    override fun getBoundingBox3D(): BoundingBox3D {
        val pad = 0.06f
        return BoundingBox3D(
            minX = x - width / 2.0f - pad,
            maxX = x + width / 2.0f + pad,
            minY = y - height / 2.0f - pad,
            maxY = y + height / 2.0f + pad,
            minZ = z - 0.25f,
            maxZ = z + 0.25f
        )
    }

    override fun onHoverEnter() {
        if (SystemClock.uptimeMillis() < cooldownUntilTime) return
        isHovered = true
        hoverProgress = 0f
    }

    override fun onHoverProgress(progress: Float) {
        if (SystemClock.uptimeMillis() < cooldownUntilTime) return
        hoverProgress = progress
    }

    override fun onHoverExit() {
        isHovered = false
        hoverProgress = 0f
        if (isDragging) {
            isDragging = false
            cooldownUntilTime = SystemClock.uptimeMillis() + 800L
        }
    }

    override fun onClick() {
        // When gaze completes dwell on the handle, activate drag mode
        if (SystemClock.uptimeMillis() >= cooldownUntilTime) {
            isDragging = !isDragging
            if (!isDragging) {
                cooldownUntilTime = SystemClock.uptimeMillis() + 800L
            }
        }
    }

    // Called every frame with the user's current gaze yaw degree
    fun updateResize(currentGazeYawDeg: Float) {
        if (!isDragging) return
        onResizeDelta?.invoke(currentGazeYawDeg)
    }
}
