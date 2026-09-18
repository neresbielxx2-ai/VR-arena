package com.lunarvr.handtracking

import android.content.Context
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

class MediaPipeHandTracker : HandTracker {

    private var handLandmarker: HandLandmarker? = null
    override var isInitialized: Boolean = false
        private set
    override var isSupported: Boolean = true
        private set
    override val trackerName: String = "MediaPipe Tasks HandLandmarker"

    private var listener: HandTrackerListener? = null

    override fun initialize(context: Context, listener: HandTrackerListener) {
        this.listener = listener
        try {
            val baseOptionsBuilder = BaseOptions.builder()
                .setModelAssetPath("hand_landmarker.task")

            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptionsBuilder.build())
                .setMinHandDetectionConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setMinHandPresenceConfidence(0.5f)
                .setNumHands(1)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener { result: HandLandmarkerResult, _ ->
                    handleResult(result)
                }
                .setErrorListener { error ->
                    listener.onError(error.message ?: "Erro desconhecido MediaPipe")
                }
                .build()

            handLandmarker = HandLandmarker.createFromOptions(context, options)
            isInitialized = true
        } catch (e: Exception) {
            isSupported = false
            isInitialized = false
            listener.onError("MediaPipe inicializado com fallback: ${e.message}")
        }
    }

    private fun handleResult(result: HandLandmarkerResult) {
        val landmarksList = result.landmarks()
        if (landmarksList.isNotEmpty() && landmarksList[0].isNotEmpty()) {
            val landmarks = landmarksList[0]
            val all = landmarks.map { Landmark3D(it.x(), it.y(), it.z()) }

            val pose = HandPose(
                isDetected = true,
                isLeftHand = false,
                wrist = all.getOrElse(0) { Landmark3D(0f, 0f, 0f) },
                thumbTip = all.getOrElse(4) { Landmark3D(0f, 0f, 0f) },
                indexMcp = all.getOrElse(5) { Landmark3D(0f, 0f, 0f) },
                indexPip = all.getOrElse(6) { Landmark3D(0f, 0f, 0f) },
                indexTip = all.getOrElse(8) { Landmark3D(0f, 0f, 0f) },
                middleTip = all.getOrElse(12) { Landmark3D(0f, 0f, 0f) },
                ringTip = all.getOrElse(16) { Landmark3D(0f, 0f, 0f) },
                pinkyTip = all.getOrElse(20) { Landmark3D(0f, 0f, 0f) },
                palmCenter = all.getOrElse(9) { Landmark3D(0f, 0f, 0f) },
                allLandmarks = all
            )
            listener?.onHandPoseUpdated(pose)
        } else {
            listener?.onHandPoseUpdated(HandPose.empty())
        }
    }

    override fun processImageProxy(imageProxy: ImageProxy) {
        if (!isInitialized || handLandmarker == null) {
            imageProxy.close()
            return
        }

        try {
            val bitmap = imageProxy.toBitmap()
            val mpImage = BitmapImageBuilder(bitmap).build()
            val timestampMs = System.currentTimeMillis()
            handLandmarker?.detectAsync(mpImage, timestampMs)
        } catch (e: Exception) {
            listener?.onError("Erro processando frame: ${e.message}")
        } finally {
            imageProxy.close()
        }
    }

    override fun release() {
        handLandmarker?.close()
        handLandmarker = null
        isInitialized = false
        listener = null
    }
}
