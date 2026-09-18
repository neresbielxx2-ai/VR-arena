package com.lunarvr.handtracking

import kotlin.math.sqrt

data class Ray3D(
    val originX: Float,
    val originY: Float,
    val originZ: Float,
    val dirX: Float,
    val dirY: Float,
    val dirZ: Float
)

class FingerRay {

    var sensitivity: Float = 1.0f
    var markerRadius: Float = 0.025f
    var showRayLine: Boolean = true

    fun calculateRay(pose: HandPose): Ray3D? {
        if (!pose.isDetected) return null

        // Origin is the tip of the index finger
        val originX = (pose.indexTip.x - 0.5f) * 2.0f * sensitivity
        val originY = -(pose.indexTip.y - 0.5f) * 2.0f * sensitivity
        val originZ = -0.5f + pose.indexTip.z

        // Direction vector from PIP joint to Tip
        var dx = pose.indexTip.x - pose.indexPip.x
        var dy = -(pose.indexTip.y - pose.indexPip.y)
        var dz = -1.0f // Forward into the virtual space

        val len = sqrt(dx * dx + dy * dy + dz * dz)
        if (len > 0.0001f) {
            dx /= len
            dy /= len
            dz /= len
        } else {
            dx = 0f
            dy = 0f
            dz = -1f
        }

        return Ray3D(originX, originY, originZ, dx, dy, dz)
    }
}
