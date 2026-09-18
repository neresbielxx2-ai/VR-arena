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
            val planes = imageProxy.planes
            if (planes.isEmpty()) {
                imageProxy.close()
                return
            }

            val yPlane = planes[0]
            val width = imageProxy.width
            val height = imageProxy.height

            // Use Y plane directly if U/V not present or corrupted
            val yBuffer = yPlane.buffer
            val yRowStride = yPlane.rowStride

            val hasUv = planes.size >= 3
            val uPlane = if (hasUv) planes[1] else null
            val vPlane = if (hasUv) planes[2] else null
            val uBuffer = uPlane?.buffer
            val vBuffer = vPlane?.buffer
            val uvRowStride = uPlane?.rowStride ?: 0
            val uvPixelStride = uPlane?.pixelStride ?: 1

            val step = 6
            var sumX = 0L
            var sumY = 0L
            var handPixelCount = 0

            val startY = height / 6
            val endY = (height * 5) / 6

            for (y in startY until endY step step) {
                for (x in 0 until width step step) {
                    val yIndex = y * yRowStride + x
                    if (yIndex >= yBuffer.limit()) continue

                    val yVal = yBuffer.get(yIndex).toInt() and 0xFF

                    var isSkin = false
                    if (hasUv && uBuffer != null && vBuffer != null) {
                        val uvIndex = (y / 2) * uvRowStride + (x / 2) * uvPixelStride
                        if (uvIndex < uBuffer.limit() && uvIndex < vBuffer.limit()) {
                            val uVal = uBuffer.get(uvIndex).toInt() and 0xFF
                            val vVal = vBuffer.get(uvIndex).toInt() and 0xFF
                            // Broad skin chrominance filter
                            if (yVal in 45..240 && uVal in 70..135 && vVal in 125..185) {
                                isSkin = true
                            }
                        }
                    } else {
                        // High brightness contrast fallback
                        if (yVal in 70..230) {
                            isSkin = true
                        }
                    }

                    if (isSkin) {
                        sumX += x
                        sumY += y
                        handPixelCount++
                    }
                }
            }

            // Lowered threshold to ensure high responsiveness when hand is shown
            if (handPixelCount >= 12) {
                consecutiveFramesWithoutHand = 0
                val rawCenterX = sumX.toFloat() / handPixelCount / width
                val rawCenterY = sumY.toFloat() / handPixelCount / height

                // Fast responsive smoothing (alpha = 0.5)
                smoothedX = smoothedX * 0.5f + rawCenterX * 0.5f
                smoothedY = smoothedY * 0.5f + rawCenterY * 0.5f
                hasDetection = true

                val tipX = smoothedX
                val tipY = (smoothedY - 0.12f).coerceAtLeast(0.02f)
                val pipY = smoothedY - 0.05f
                val mcpY = smoothedY

                val pose = HandPose(
                    isDetected = true,
                    isLeftHand = false,
                    wrist = Landmark3D(smoothedX, (smoothedY + 0.15f).coerceAtMost(0.98f), 0.0f),
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
                if (consecutiveFramesWithoutHand > 3) {
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
