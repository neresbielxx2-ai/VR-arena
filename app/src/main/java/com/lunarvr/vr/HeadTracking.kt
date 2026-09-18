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

    // Raw sensor matrix (portrait space)
    private val rawRotationMatrix = FloatArray(16)
    // Landscape remap (X -> Y, Y -> -X)
    private val landscapeMatrix = FloatArray(16)
    // Calibration zero baseline
    private val baselineMatrix = FloatArray(16)
    // Relative rotation in world
    private val relativeRotation = FloatArray(16)
    // Final camera view matrix
    private val viewMatrix = FloatArray(16)
    private var isCalibrated = false

    // Fallbacks
    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    init {
        Matrix.setIdentityM(rawRotationMatrix, 0)
        Matrix.setIdentityM(landscapeMatrix, 0)
        Matrix.setIdentityM(baselineMatrix, 0)
        Matrix.setIdentityM(relativeRotation, 0)
        Matrix.setIdentityM(viewMatrix, 0)
    }

    fun start() {
        if (sensorManager == null) {
            sensorStatusMessage = "SensorManager indisponível"
            return
        }

        val gameRot = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        val rotVec = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        when {
            gameRot != null -> {
                sensorManager.registerListener(this, gameRot, SensorManager.SENSOR_DELAY_FASTEST)
                activeSensorType = TrackingSensorType.GAME_ROTATION_VECTOR
                sensorStatusMessage = "3DoF: Giroscópio VR Ativo"
            }
            rotVec != null -> {
                sensorManager.registerListener(this, rotVec, SensorManager.SENSOR_DELAY_FASTEST)
                activeSensorType = TrackingSensorType.ROTATION_VECTOR
                sensorStatusMessage = "3DoF: Sensor Rotação Absoluto"
            }
            gyro != null && accel != null -> {
                sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_FASTEST)
                sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_FASTEST)
                activeSensorType = TrackingSensorType.ACCEL_GYRO_FUSION
                sensorStatusMessage = "3DoF: Fusão Giro + Acelerômetro"
            }
            accel != null -> {
                sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_FASTEST)
                activeSensorType = TrackingSensorType.ACCELEROMETER_ONLY
                sensorStatusMessage = "3DoF: Acelerômetro"
            }
            else -> {
                activeSensorType = TrackingSensorType.NONE
                sensorStatusMessage = "Sensores de rotação indisponíveis"
            }
        }
        Log.d("LunarVR", "HeadTracking started: $sensorStatusMessage")
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    fun recenter() {
        synchronized(this) {
            System.arraycopy(landscapeMatrix, 0, baselineMatrix, 0, 16)
            isCalibrated = true
        }
        Log.d("LunarVR", "HeadTracking recentered")
    }

    override fun onSensorChanged(event: SensorEvent) {
        synchronized(this) {
            when (event.sensor.type) {
                Sensor.TYPE_GAME_ROTATION_VECTOR,
                Sensor.TYPE_ROTATION_VECTOR -> {
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

            // Remap coordinate system for landscape orientation (Landscape standard: X->Y, Y->-X)
            SensorManager.remapCoordinateSystem(
                rawRotationMatrix,
                SensorManager.AXIS_Y,
                SensorManager.AXIS_MINUS_X,
                landscapeMatrix
            )

            if (!isCalibrated) {
                System.arraycopy(landscapeMatrix, 0, baselineMatrix, 0, 16)
                isCalibrated = true
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun getHeadMatrix(outputMatrix: FloatArray) {
        synchronized(this) {
            // R_rel = (R_baseline)^T * R_current
            val baseTransposed = FloatArray(16)
            Matrix.transposeM(baseTransposed, 0, baselineMatrix, 0)

            Matrix.multiplyMM(relativeRotation, 0, baseTransposed, 0, landscapeMatrix, 0)

            // Convert World-to-Camera: ViewMatrix = Transpose(R_rel)
            Matrix.transposeM(viewMatrix, 0, relativeRotation, 0)

            if (invertPitch || invertYaw) {
                val scaleX = if (invertYaw) -1.0f else 1.0f
                val scaleY = if (invertPitch) -1.0f else 1.0f
                Matrix.scaleM(viewMatrix, 0, scaleX, scaleY, 1.0f)
            }

            System.arraycopy(viewMatrix, 0, outputMatrix, 0, 16)
        }
    }
}
