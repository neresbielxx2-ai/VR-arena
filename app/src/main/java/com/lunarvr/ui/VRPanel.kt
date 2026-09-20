package com.lunarvr.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class VRPanel(
    val id: String,
    var x: Float,
    var y: Float,
    var z: Float,
    var width: Float,
    var height: Float,
    val pixelWidth: Int = 512,
    val pixelHeight: Int = 256,
    var rotationYDeg: Float = 0f
) {
    private var textureId: Int = 0
    var surfaceBitmap: Bitmap = Bitmap.createBitmap(pixelWidth, pixelHeight, Bitmap.Config.ARGB_8888)
    private var canvas: Canvas = Canvas(surfaceBitmap)

    private val vertexCoords = FloatArray(12)
    private val textureCoords = floatArrayOf(
        0f, 0f,
        0f, 1f,
        1f, 0f,
        1f, 1f
    )

    private var vertexBuffer: FloatBuffer
    private var textureBuffer: FloatBuffer
    private var isTextureDirty: Boolean = true

    init {
        updateVertexCoordinates()

        vertexBuffer = ByteBuffer.allocateDirect(vertexCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        vertexBuffer.put(vertexCoords).position(0)

        textureBuffer = ByteBuffer.allocateDirect(textureCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        textureBuffer.put(textureCoords).position(0)
    }

    fun setDimensions(newW: Float, newH: Float) {
        width = newW
        height = newH
        updateVertexCoordinates()
        vertexBuffer.position(0)
        vertexBuffer.put(vertexCoords).position(0)
    }

    private fun updateVertexCoordinates() {
        val hw = width / 2f
        val hh = height / 2f

        // Quad triangles: (x-hw, y+hh), (x-hw, y-hh), (x+hw, y+hh), (x+hw, y-hh)
        vertexCoords[0] = -hw; vertexCoords[1] = hh; vertexCoords[2] = 0f
        vertexCoords[3] = -hw; vertexCoords[4] = -hh; vertexCoords[5] = 0f
        vertexCoords[6] = hw; vertexCoords[7] = hh; vertexCoords[8] = 0f
        vertexCoords[9] = hw; vertexCoords[10] = -hh; vertexCoords[11] = 0f
    }

    fun initGL() {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    fun drawCustom(block: (Canvas, Paint) -> Unit) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        block(canvas, paint)
        isTextureDirty = true
    }

    fun copyBitmap(source: Bitmap) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(source, null, RectF(0f, 0f, pixelWidth.toFloat(), pixelHeight.toFloat()), paint)
        isTextureDirty = true
    }

    fun bindAndRender(program: Int, vpMatrix: FloatArray, aPosHandle: Int, aTexHandle: Int, uMvpHandle: Int) {
        if (textureId == 0) return

        if (isTextureDirty) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, surfaceBitmap, 0)
            isTextureDirty = false
        }

        val modelMatrix = FloatArray(16)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        if (rotationYDeg != 0f) {
            Matrix.rotateM(modelMatrix, 0, rotationYDeg, 0f, 1f, 0f)
        }

        val mvpMatrix = FloatArray(16)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(uMvpHandle, 1, false, mvpMatrix, 0)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        vertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aPosHandle)
        GLES20.glVertexAttribPointer(aPosHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        textureBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aTexHandle)
        GLES20.glVertexAttribPointer(aTexHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPosHandle)
        GLES20.glDisableVertexAttribArray(aTexHandle)
    }
}
