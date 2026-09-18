package com.lunarvr.system

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.os.Build

data class HardwareReport(
    val hasGyroscope: Boolean,
    val hasAccelerometer: Boolean,
    val hasRotationVector: Boolean,
    val hasCamera: Boolean,
    val totalRamMb: Long,
    val osVersion: String,
    val deviceModel: String,
    val recommendedPerformanceMode: PerformanceLevel
)

enum class PerformanceLevel {
    ECONOMIC,
    NORMAL,
    QUALITY
}

class DeviceCapabilities(private val context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    fun inspect(): HardwareReport {
        val hasGyro = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
        val hasAccel = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
        val hasRotVec = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null ||
                sensorManager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) != null

        val hasCam = try {
            val list = cameraManager?.cameraIdList
            list != null && list.isNotEmpty()
        } catch (_: Exception) {
            false
        }

        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)
        val ramMb = memInfo.totalMem / (1024 * 1024)

        // Heuristic for performance
        val perfMode = when {
            ramMb <= 3000 || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> PerformanceLevel.ECONOMIC
            ramMb >= 6000 -> PerformanceLevel.QUALITY
            else -> PerformanceLevel.NORMAL
        }

        return HardwareReport(
            hasGyroscope = hasGyro,
            hasAccelerometer = hasAccel,
            hasRotationVector = hasRotVec,
            hasCamera = hasCam,
            totalRamMb = ramMb,
            osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            recommendedPerformanceMode = perfMode
        )
    }
}
