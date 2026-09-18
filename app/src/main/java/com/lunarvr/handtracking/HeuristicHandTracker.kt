package com.lunarvr.handtracking

import android.content.Context
import android.graphics.ImageFormat
import android.util.Log
import androidx.camera.core.ImageProxy

class HeuristicHandTracker : HandTracker {

    override var isInitialized: Boolean = false
        private set
    override val isSupported: Boolean = true
    override val trackerName: String = "Lunar Optical Skin-Blob Tracker"

    private var listener: HandTrackerListener? = null
    private var smoothedX = 0.5f
    private var smoothedY = 0.5f
    private var hasDetection = false
    private var consecutiveFramesWithoutHand = 0

    override fun initialize(context: Context, listener: HandTrackerListener) {
        this.listener = listener
        isInitialized = true
        Log.d("LunarVR", "HeuristicHandTracker initialized")
    }

    override fun processImageProxy(imageProxy: ImageProxy) {
        try {
            if (imageProxy.format != ImageFormat.YUV_420_888) {
                // If not YUV, fallback or sample Y
                imageProxy.close()
                return
            }

            val yPlane = imageProxy.planes[0]
            val uPlane = imageProxy.planes[1]
            val vPlane = imageProxy.planes[2]

            val yBuffer = yPlane.buffer
            val uBuffer = uPlane.buffer
            val vBuffer = vPlane.buffer

            val width = imageProxy.width
            val height = imageProxy.height

            val yRowStride = yPlane.rowStride
            val uvRowStride = uPlane.rowStride
            val uvPixelStride = uPlane.pixelStride

            // Sample every 8 pixels to keep processing under 3ms
            val step = 8
            var sumX = 0L
            var sumY = 0L
            var handPixelCount = 0

            // Search mainly in lower/middle region where hands naturally enter the camera view
            val startY = height / 5
            val endY = height

            for (y in startY until endY step step) {
                for (x in 0 until width step step) {
                    val yIndex = y * yRowStride + x
                    val uvIndex = (y / 2) * uvRowStride + (x / 2) * uvPixelStride

                    if (yIndex < yBuffer.limit() && uvIndex < uBuffer.limit() && uvIndex < vBuffer.limit()) {
                        val yVal = yBuffer.get(yIndex).toInt() and 0xFF
                        val uVal = uBuffer.get(uvIndex).toInt() and 0xFF
                        val vVal = vBuffer.get(uvIndex).toInt() and 0xFF

                        // Human skin chrominance bounding box in YCbCr:
                        // Y > 40, Cb (u) between 77 and 127, Cr (v) between 133 and 173
                        if (yVal > 40 && uVal in 75..130 && vVal in 130..180) {
                            sumX += x
                            sumY += y
                            handPixelCount++
                        }
                    }
                }
            }

            // Need at least 25 skin pixels sampled
            if (handPixelCount > 25) {
                consecutiveFramesWithoutHand = 0
                val rawCenterX = sumX.toFloat() / handPixelCount / width
                val rawCenterY = sumY.toFloat() / handPixelCount / height

                // Exponential smoothing (alpha = 0.35)
                smoothedX = smoothedX * 0.65f + rawCenterX * 0.35f
                smoothedY = smoothedY * 0.65f + rawCenterY * 0.35f
                hasDetection = true

                // In landscape rear camera:
                // Camera coords: X goes right, Y goes down.
                val tipX = smoothedX
                val tipY = (smoothedY - 0.12f).coerceAtLeast(0.05f) // Index tip is above center of hand
                val pipY = smoothedY - 0.05f
                val mcpY = smoothedY

                val pose = HandPose(
                    isDetected = true,
                    isLeftHand = false,
                    wrist = Landmark3D(smoothedX, (smoothedY + 0.15f).coerceAtMost(0.95f), 0.0f),
                    indexTip = Landmark3D(tipX, tipY, 0.0f),
                    indexPip = Landmark3D(smoothedX, pipY, 0.0f),
                    indexMcp = Landmark3D(smoothedX, mcpY, 0.0f),
                    thumbTip = Landmark3D(smoothedX - 0.08f, smoothedY, 0.0f),
                    middleTip = Landmark3D(smoothedX + 0.02f, tipY + 0.02f, 0.0f),
                    ringTip = Landmark3D(smoothedX + 0.05f, tipY + 0.05f, 0.0f),
                    pinkyTip = Landmark3D(smoothedX + 0.08f, tipY + 0.09f, 0.0f),
                    palmCenter = Landmark3D(smoothedX, smoothedY, 0.0f)
                )
                listener?.onHandPoseUpdated(pose)
            } else {
                consecutiveFramesWithoutHand++
                if (consecutiveFramesWithoutHand > 5) {
                    hasDetection = false
                    listener?.onHandPoseUpdated(HandPose.empty())
                }
            }
        } catch (e: Exception) {
            listener?.onError("Erro na análise da mão: ${e.message}")
        } finally {
            imageProxy.close()
        }
    }

    override fun release() {
        isInitialized = false
        listener = null
    }
}
