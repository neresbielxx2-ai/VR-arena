package com.lunarvr.handtracking

import android.content.Context
import androidx.camera.core.ImageProxy

interface HandTrackerListener {
    fun onHandPoseUpdated(pose: HandPose)
    fun onError(message: String)
}

interface HandTracker {
    val isInitialized: Boolean
    val isSupported: Boolean
    val trackerName: String

    fun initialize(context: Context, listener: HandTrackerListener)
    fun processImageProxy(imageProxy: ImageProxy)
    fun release()
}
