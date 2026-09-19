package com.lunarvr.browser

import com.lunarvr.handtracking.BoundingBox3D
import com.lunarvr.handtracking.InteractableElement
import com.lunarvr.handtracking.Ray3D

class BrowserTouchElement(
    override val id: String = "browser_touch_surface",
    var x: Float,
    var y: Float,
    var z: Float,
    var width: Float,
    var height: Float,
    private val onClickNormalized: (Float, Float) -> Unit
) : InteractableElement {

    var isHovered: Boolean = false
        private set
    var hoverProgress: Float = 0f
        private set

    private var lastHitNormX: Float = 0.5f
    private var lastHitNormY: Float = 0.5f

    override fun getBoundingBox3D(): BoundingBox3D {
        return BoundingBox3D(
            minX = x - width / 2f,
            maxX = x + width / 2f,
            minY = y - height / 2f,
            maxY = y + height / 2f,
            minZ = z - 0.2f,
            maxZ = z + 0.2f
        )
    }

    fun updateHitCoordinate(ray: Ray3D) {
        if (Math.abs(ray.dirZ) < 0.0001f) return
        val t = (z - ray.originZ) / ray.dirZ
        if (t <= 0f) return

        val hitWorldX = ray.originX + t * ray.dirX
        val hitWorldY = ray.originY + t * ray.dirY

        // Map world hit into 0.0 .. 1.0 UV coords
        val normX = ((hitWorldX - (x - width / 2f)) / width).coerceIn(0f, 1f)
        // OpenGL Y is inverted relative to Android screen coordinates (0 is top in Android)
        val normY = (1f - ((hitWorldY - (y - height / 2f)) / height)).coerceIn(0f, 1f)

        lastHitNormX = normX
        lastHitNormY = normY
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
        onClickNormalized(lastHitNormX, lastHitNormY)
    }
}
