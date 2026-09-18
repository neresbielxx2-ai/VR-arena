package com.lunarvr.vr

import android.opengl.Matrix

class StereoCamera {

    var ipd: Float = 0.064f // 64mm average interpupillary distance
    var fovDegrees: Float = 95.0f
    var nearClip: Float = 0.1f
    var farClip: Float = 100.0f
    var mirrorLeftRight: Boolean = false

    private val leftEyeViewMatrix = FloatArray(16)
    private val rightEyeViewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)

    fun updateProjection(eyeWidth: Int, eyeHeight: Int) {
        val aspect = eyeWidth.toFloat() / eyeHeight.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, fovDegrees, aspect, nearClip, farClip)
    }

    fun computeEyeMatrices(headMatrix: FloatArray) {
        // Translation for Left Eye (-ipd / 2)
        val leftTranslation = FloatArray(16)
        Matrix.setIdentityM(leftTranslation, 0)
        val leftOffset = if (mirrorLeftRight) (ipd / 2.0f) else (-ipd / 2.0f)
        Matrix.translateM(leftTranslation, 0, leftOffset, 0f, 0f)

        // Translation for Right Eye (+ipd / 2)
        val rightTranslation = FloatArray(16)
        Matrix.setIdentityM(rightTranslation, 0)
        val rightOffset = if (mirrorLeftRight) (-ipd / 2.0f) else (ipd / 2.0f)
        Matrix.translateM(rightTranslation, 0, rightOffset, 0f, 0f)

        // Combine translation with head orientation
        Matrix.multiplyMM(leftEyeViewMatrix, 0, leftTranslation, 0, headMatrix, 0)
        Matrix.multiplyMM(rightEyeViewMatrix, 0, rightTranslation, 0, headMatrix, 0)
    }

    fun getLeftEyeViewMatrix(): FloatArray = leftEyeViewMatrix
    fun getRightEyeViewMatrix(): FloatArray = rightEyeViewMatrix
    fun getProjectionMatrix(): FloatArray = projectionMatrix
}
