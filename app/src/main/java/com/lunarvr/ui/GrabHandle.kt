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
    private val grabDurationMs: Long = 2000L // EXACTLY 2 seconds grab movement window
    var cooldownUntilTime: Long = 0 // Cooldown so it doesn't immediately re-grab

    // Callback when user drags the window in 3D spherical space (yaw angle in deg, height Y, distance Z)
    var onDragUpdateSpherical: ((Float, Float, Float) -> Unit)? = null

    override fun getBoundingBox3D(): BoundingBox3D {
        val padX = 0.08f
        val padY = 0.05f
        return BoundingBox3D(
            minX = x - width / 2.0f - padX,
            maxX = x + width / 2.0f + padX,
            minY = y - height / 2.0f - padY,
            maxY = y + height / 2.0f + padY,
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
        if (SystemClock.uptimeMillis() < cooldownUntilTime) {
            hoverProgress = 0f
            return
        }
        hoverProgress = progress
    }

    override fun onHoverExit() {
        if (!isGrabbed) {
            isHovered = false
            hoverProgress = 0f
        }
    }

    override fun onClick() {
        if (SystemClock.uptimeMillis() < cooldownUntilTime) return
        // Dwell completed: Start grab!
        startGrab()
    }

    fun startGrab() {
        isGrabbed = true
        grabStartTime = SystemClock.uptimeMillis()
    }

    fun updateGrabSpherical(yawDeg: Float, heightY: Float, distanceZ: Float) {
        if (!isGrabbed) return

        val now = SystemClock.uptimeMillis()
        val elapsed = now - grabStartTime

        if (elapsed <= grabDurationMs) {
            // Smoothly follow head gaze without any boundary walls
            onDragUpdateSpherical?.invoke(yawDeg, heightY, distanceZ)
        } else {
            // AUTOMATICALLY UNLOCK AND RELEASE after exactly 2 seconds!
            isGrabbed = false
            isHovered = false
            hoverProgress = 0f
            // Enforce a 1.5-second cooldown to avoid instant re-lock
            cooldownUntilTime = now + 1500L
        }
    }
}
