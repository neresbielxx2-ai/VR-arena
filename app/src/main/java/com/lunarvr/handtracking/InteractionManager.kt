package com.lunarvr.handtracking

import android.os.SystemClock

interface InteractableElement {
    val id: String
    fun getBoundingBox3D(): BoundingBox3D
    fun onHoverEnter()
    fun onHoverProgress(progress: Float)
    fun onHoverExit()
    fun onClick()
}

data class BoundingBox3D(
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float,
    val minZ: Float,
    val maxZ: Float
) {
    fun intersects(ray: Ray3D): Boolean {
        // Plane intersection at Z = (minZ + maxZ) / 2
        val targetZ = (minZ + maxZ) / 2.0f
        if (ray.dirZ == 0f) return false
        val t = (targetZ - ray.originZ) / ray.dirZ
        if (t <= 0f) return false // Ray points backwards

        val hitX = ray.originX + t * ray.dirX
        val hitY = ray.originY + t * ray.dirY

        return hitX in minX..maxX && hitY in minY..maxY
    }
}

class InteractionManager {

    private val interactables = mutableListOf<InteractableElement>()
    private var currentlyHovered: InteractableElement? = null
    private var hoverStartTime: Long = 0
    private val dwellClickDurationMs: Long = 1000L // 1 second dwell time

    var lastHitElementId: String? = null
        private set
    var currentProgress: Float = 0f
        private set

    fun register(element: InteractableElement) {
        if (!interactables.contains(element)) {
            interactables.add(element)
        }
    }

    fun unregister(element: InteractableElement) {
        interactables.remove(element)
        if (currentlyHovered == element) {
            currentlyHovered?.onHoverExit()
            currentlyHovered = null
            currentProgress = 0f
        }
    }

    fun clear() {
        interactables.clear()
        currentlyHovered = null
        currentProgress = 0f
    }

    fun update(ray: Ray3D?) {
        if (ray == null) {
            currentlyHovered?.onHoverExit()
            currentlyHovered = null
            currentProgress = 0f
            lastHitElementId = null
            return
        }

        var hit: InteractableElement? = null
        for (elem in interactables) {
            if (elem.getBoundingBox3D().intersects(ray)) {
                hit = elem
                break
            }
        }

        val now = SystemClock.uptimeMillis()

        if (hit != currentlyHovered) {
            currentlyHovered?.onHoverExit()
            currentlyHovered = hit
            hoverStartTime = now
            currentProgress = 0f
            hit?.onHoverEnter()
            lastHitElementId = hit?.id
        } else if (hit != null) {
            val elapsed = now - hoverStartTime
            val progress = (elapsed.toFloat() / dwellClickDurationMs).coerceIn(0f, 1f)
            currentProgress = progress
            hit.onHoverProgress(progress)

            if (elapsed >= dwellClickDurationMs) {
                // Execute click!
                hit.onClick()
                // Reset timer after click so it doesn't instantly click again
                hoverStartTime = now + 500L
                currentProgress = 0f
            }
        }
    }
}
