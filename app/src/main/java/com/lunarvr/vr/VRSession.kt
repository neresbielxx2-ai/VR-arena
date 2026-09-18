package com.lunarvr.vr

import android.content.Context
import com.lunarvr.system.DeviceCapabilities
import com.lunarvr.system.HardwareReport
import com.lunarvr.system.PerformanceLevel
import com.lunarvr.system.PerformanceManager

class VRSession(context: Context) {

    val deviceCapabilities = DeviceCapabilities(context)
    val hardwareReport: HardwareReport = deviceCapabilities.inspect()
    val performanceManager = PerformanceManager()
    val headTracking = HeadTracking(context)
    val stereoCamera = StereoCamera()
    val recenterManager = RecenterManager(headTracking)

    var isSessionActive: Boolean = false
        private set

    init {
        performanceManager.applyLevel(hardwareReport.recommendedPerformanceMode, isAutomatic = true)
    }

    fun start() {
        headTracking.start()
        isSessionActive = true
    }

    fun pause() {
        headTracking.stop()
        isSessionActive = false
    }

    fun resume() {
        headTracking.start()
        isSessionActive = true
    }

    fun stop() {
        headTracking.stop()
        isSessionActive = false
    }
}
