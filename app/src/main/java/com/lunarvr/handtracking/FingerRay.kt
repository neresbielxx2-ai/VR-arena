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

    var sensitivity: Float = 1.6f
    var markerRadius: Float = 0.025f
    var showRayLine: Boolean = true

    fun calculateRay(pose: HandPose): Ray3D? {
        if (!pose.isDetected) return null

        // Map camera coordinates (0.0 to 1.0) into comfortable VR workspace [-0.8, 0.8]
        // In landscape VR:
        // pose.indexTip.x: 0 (left) to 1 (right) -> map to [-0.8, 0.8]
        // pose.indexTip.y: 0 (top) to 1 (bottom) -> inverted to Y-up [+0.6, -0.6]
        val originX = (pose.indexTip.x - 0.5f) * 1.8f * sensitivity
        val originY = -(pose.indexTip.y - 0.5f) * 1.6f * sensitivity
        val originZ = -0.6f + pose.indexTip.z

        // The ray aims forward towards the VR panels (Z = -1.2)
        var dx = (pose.indexTip.x - pose.indexPip.x) * 2.0f
        var dy = -(pose.indexTip.y - pose.indexPip.y) * 2.0f
        var dz = -1.0f // Projecting forward into the UI space

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
