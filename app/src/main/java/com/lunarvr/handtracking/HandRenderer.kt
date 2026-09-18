package com.lunarvr.handtracking

import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class HandRenderer {

    private var lineProgram: Int = 0
    private var pointProgram: Int = 0

    private val vertexShaderCode = """
        uniform mat4 uMVPMatrix;
        attribute vec4 vPosition;
        uniform float uPointSize;
        void main() {
            gl_Position = uMVPMatrix * vPosition;
            gl_PointSize = uPointSize;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        uniform vec4 vColor;
        void main() {
            gl_FragColor = vColor;
        }
    """.trimIndent()

    private val lineCoords = FloatArray(6) // 2 points x 3 coords
    private var lineBuffer: FloatBuffer

    private val jointCoords = FloatArray(21 * 3)
    private var jointBuffer: FloatBuffer

    init {
        lineBuffer = ByteBuffer.allocateDirect(lineCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        jointBuffer = ByteBuffer.allocateDirect(jointCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
    }

    fun initGL() {
        val vShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        lineProgram = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vShader)
            GLES20.glAttachShader(it, fShader)
            GLES20.glLinkProgram(it)
        }

        pointProgram = lineProgram
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }

    fun renderHand(projectionMatrix: FloatArray, pose: HandPose, ray: Ray3D?, showRayLine: Boolean, markerRadius: Float) {
        if (!pose.isDetected || lineProgram == 0) return

        GLES20.glUseProgram(pointProgram)
        val mvpHandle = GLES20.glGetUniformLocation(pointProgram, "uMVPMatrix")
        val colorHandle = GLES20.glGetUniformLocation(pointProgram, "vColor")
        val pointSizeHandle = GLES20.glGetUniformLocation(pointProgram, "uPointSize")
        val posHandle = GLES20.glGetAttribLocation(pointProgram, "vPosition")

        // Render directly in camera screen space so the hand is attached to user perspective
        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, projectionMatrix, 0)

        // Draw Joints
        val landmarks = if (pose.allLandmarks.isNotEmpty()) pose.allLandmarks else listOf(
            pose.wrist, pose.thumbTip, pose.indexPip, pose.indexTip,
            pose.middleTip, pose.ringTip, pose.pinkyTip, pose.palmCenter
        )

        var idx = 0
        for (lm in landmarks) {
            if (idx + 3 <= jointCoords.size) {
                // In Camera View Coordinates: X [-0.6, 0.6], Y [-0.5, 0.5], Z ~ -0.7m
                jointCoords[idx] = (lm.x - 0.5f) * 1.4f
                jointCoords[idx + 1] = -(lm.y - 0.5f) * 1.2f
                jointCoords[idx + 2] = -0.7f + lm.z
                idx += 3
            }
        }

        jointBuffer.clear()
        jointBuffer.put(jointCoords, 0, idx)
        jointBuffer.position(0)

        GLES20.glEnableVertexAttribArray(posHandle)
        GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 0, jointBuffer)

        // Lunar cyan glow color for joints
        GLES20.glUniform4f(colorHandle, 0.0f, 0.9f, 1.0f, 0.85f)
        GLES20.glUniform1f(pointSizeHandle, 16.0f)
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, idx / 3)

        // Highlight index tip (Gold / Cyan accent)
        GLES20.glUniform4f(colorHandle, 1.0f, 0.8f, 0.1f, 1.0f)
        GLES20.glUniform1f(pointSizeHandle, 24.0f)
        GLES20.glDrawArrays(GLES20.GL_POINTS, if (landmarks.size > 8) 8 else 3, 1)

        // Draw Ray Line shooting from fingertip forward towards the UI
        if (showRayLine && ray != null) {
            GLES20.glLineWidth(4.0f)
            lineCoords[0] = ray.originX
            lineCoords[1] = ray.originY
            lineCoords[2] = ray.originZ
            lineCoords[3] = ray.originX + ray.dirX * 2.5f
            lineCoords[4] = ray.originY + ray.dirY * 2.5f
            lineCoords[5] = ray.originZ + ray.dirZ * 2.5f

            lineBuffer.clear()
            lineBuffer.put(lineCoords)
            lineBuffer.position(0)

            GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 0, lineBuffer)
            // Glowing translucent cyan ray
            GLES20.glUniform4f(colorHandle, 0.3f, 0.95f, 1.0f, 0.85f)
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2)
        }

        GLES20.glDisableVertexAttribArray(posHandle)
    }
}
