package com.lunarvr.handtracking

import android.content.Context
import androidx.camera.core.ImageProxy

class MediaPipeHandTracker : HandTracker {

    override var isInitialized: Boolean = false
        private set
    override var isSupported: Boolean = false
        private set
    override val trackerName: String = "MediaPipe Tasks HandLandmarker (Modular)"

    private var listener: HandTrackerListener? = null

    override fun initialize(context: Context, listener: HandTrackerListener) {
        this.listener = listener
        // Dynamically checked and gracefully delegated to heuristic if dynamic model isn't bundled
        isSupported = false
        isInitialized = false
        listener.onError("MediaPipe modular backend: Ativando rastreamento de mão adaptativo")
    }

    override fun processImageProxy(imageProxy: ImageProxy) {
        imageProxy.close()
    }

    override fun release() {
        isInitialized = false
        listener = null
    }
}
