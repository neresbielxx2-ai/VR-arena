package com.lunarvr.vr

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.Matrix
import android.util.Log

enum class TrackingSensorType {
    ROTATION_VECTOR,
    GAME_ROTATION_VECTOR,
    ACCEL_GYRO_FUSION,
    ACCELEROMETER_ONLY,
    NONE
}

class HeadTracking(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    var activeSensorType: TrackingSensorType = TrackingSensorType.NONE
        private set
    var sensorStatusMessage: String = "Iniciando sensores..."
        private set

    var invertPitch: Boolean = false
    var invertYaw: Boolean = false

    private val rawRotationMatrix = FloatArray(16)
    private val landscapeRotationMatrix = FloatArray(16)
    private val centerOffsetMatrix = FloatArray(16)
    private val finalHeadViewMatrix = FloatArray(16)

    // Fallback complementary filter variables
    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    init {
        Matrix.setIdentityM(rawRotationMatrix, 0)
        Matrix.setIdentityM(landscapeRotationMatrix, 0)
        Matrix.setIdentityM(centerOffsetMatrix, 0)
        Matrix.setIdentityM(finalHeadViewMatrix, 0)
    }

    fun start() {
        if (sensorManager == null) {
            sensorStatusMessage = "SensorManager indisponível"
            return
        }

        val rotVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val gameRotSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        when {
            gameRotSensor != null -> {
                // Game Rotation Vector is preferred for mobile VR (no magnetic field interference/jump)
                sensorManager.registerListener(this, gameRotSensor, SensorManager.SENSOR_DELAY_FASTEST)
                activeSensorType = TrackingSensorType.GAME_ROTATION_VECTOR
                sensorStatusMessage = "3DoF: Giroscópio + Acelerômetro (VR Modo Estável)"
            }
            rotVectorSensor != null -> {
                sensorManager.registerListener(this, rotVectorSensor, SensorManager.SENSOR_DELAY_FASTEST)
                activeSensorType = TrackingSensorType.ROTATION_VECTOR
                sensorStatusMessage = "3DoF: Sensor Rotação Absoluto (Alta precisão)"
            }
            gyroSensor != null && accelSensor != null -> {
                sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_FASTEST)
                sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_FASTEST)
                activeSensorType = TrackingSensorType.ACCEL_GYRO_FUSION
                sensorStatusMessage = "3DoF: Fusão Giroscópio + Acelerômetro"
            }
            accelSensor != null -> {
                sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_FASTEST)
                activeSensorType = TrackingSensorType.ACCELEROMETER_ONLY
                sensorStatusMessage = "3DoF Reduzido: Apenas acelerômetro"
            }
            else -> {
                activeSensorType = TrackingSensorType.NONE
                sensorStatusMessage = "Nenhum sensor de orientação encontrado"
            }
        }
        Log.d("LunarVR", "HeadTracking started: $sensorStatusMessage")
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    fun recenter() {
        synchronized(this) {
            // Store current landscape world orientation as the zero baseline
            System.arraycopy(landscapeRotationMatrix, 0, centerOffsetMatrix, 0, 16)
        }
        Log.d("LunarVR", "HeadTracking recentered")
    }

    override fun onSensorChanged(event: SensorEvent) {
        synchronized(this) {
            when (event.sensor.type) {
                Sensor.TYPE_ROTATION_VECTOR,
                Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                    SensorManager.getRotationMatrixFromVector(rawRotationMatrix, event.values)
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    System.arraycopy(event.values, 0, gravity, 0, 3)
                    hasGravity = true
                    if (activeSensorType == TrackingSensorType.ACCELEROMETER_ONLY ||
                        (activeSensorType == TrackingSensorType.ACCEL_GYRO_FUSION && !hasGeomagnetic)) {
                        SensorManager.getRotationMatrix(rawRotationMatrix, null, gravity, floatArrayOf(0f, 1f, 0f))
                    }
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                    hasGeomagnetic = true
                    if (hasGravity) {
                        SensorManager.getRotationMatrix(rawRotationMatrix, null, gravity, geomagnetic)
                    }
                }
            }

            // Standard Android VR Landscape coordinate remapping:
            // Device natural portrait X (right) becomes -Y, and Y (up) becomes X (right) in landscape
            SensorManager.remapCoordinateSystem(
                rawRotationMatrix,
                SensorManager.AXIS_Y,
                SensorManager.AXIS_MINUS_X,
                landscapeRotationMatrix
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun getHeadMatrix(outputMatrix: FloatArray) {
        synchronized(this) {
            // Compute relative rotation: R_rel = (R_center)^T * R_current
            val centerTransposed = FloatArray(16)
            Matrix.transposeM(centerTransposed, 0, centerOffsetMatrix, 0)

            val relativeRotation = FloatArray(16)
            Matrix.multiplyMM(relativeRotation, 0, centerTransposed, 0, landscapeRotationMatrix, 0)

            // Convert World-to-Device rotation into Camera View Matrix (invert/transpose of camera pose)
            Matrix.transposeM(finalHeadViewMatrix, 0, relativeRotation, 0)

            // Invert axes if user toggled in settings
            if (invertPitch || invertYaw) {
                val scaleX = if (invertYaw) -1.0f else 1.0f
                val scaleY = if (invertPitch) -1.0f else 1.0f
                Matrix.scaleM(finalHeadViewMatrix, 0, scaleX, scaleY, 1.0f)
            }

            System.arraycopy(finalHeadViewMatrix, 0, outputMatrix, 0, 16)
        }
    }
}
