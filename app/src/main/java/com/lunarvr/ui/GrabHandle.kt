package com.lunarvr.ui

import android.os.SystemClock
import com.lunarvr.handtracking.BoundingBox3D
import com.lunarvr.handtracking.InteractableElement

class GrabHandle(
    override val id: String,
    var x: Float,
    var y: Float,
    var z: Float,
    var width: Float,
    var height: Float
) : InteractableElement {

    var isHovered: Boolean = false
        private set
    var hoverProgress: Float = 0f
        private set

    // Grab lock state: gaze locks on handle after 2 seconds
    var isGrabbed: Boolean = false
        private set
    private var grabStartTime: Long = 0
    private val grabDurationMs: Long = 2000L // 2 seconds lock duration

    // Callback when user drags the bar
    var onDragUpdate: ((Float, Float) -> Unit)? = null

    override fun getBoundingBox3D(): BoundingBox3D {
        val padX = 0.05f
        val padY = 0.04f
        return BoundingBox3D(
            minX = x - width / 2.0f - padX,
            maxX = x + width / 2.0f + padX,
            minY = y - height / 2.0f - padY,
            maxY = y + height / 2.0f + padY,
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
        // Toggle grab mode when dwell timer triggers (or lock starts)
        startGrab()
    }

    fun startGrab() {
        isGrabbed = true
        grabStartTime = SystemClock.uptimeMillis()
    }

    fun updateGrab(lookTargetX: Float, lookTargetY: Float) {
        if (!isGrabbed) return

        val now = SystemClock.uptimeMillis()
        val elapsed = now - grabStartTime

        if (elapsed <= grabDurationMs) {
            // Actively follow gaze target smoothly while locked
            onDragUpdate?.invoke(lookTargetX, lookTargetY)
        } else {
            // Automatically ungrab / release after exactly 2 seconds!
            isGrabbed = false
            hoverProgress = 0f
        }
    }
}
