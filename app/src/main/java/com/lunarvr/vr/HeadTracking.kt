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
    private val initialRotationMatrix = FloatArray(16)
    private val finalHeadViewMatrix = FloatArray(16)
    private var hasCalibratedBaseline = false

    // Fallback variables
    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    init {
        Matrix.setIdentityM(rawRotationMatrix, 0)
        Matrix.setIdentityM(landscapeRotationMatrix, 0)
        Matrix.setIdentityM(initialRotationMatrix, 0)
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
                sensorManager.registerListener(this, gameRotSensor, SensorManager.SENSOR_DELAY_FASTEST)
                activeSensorType = TrackingSensorType.GAME_ROTATION_VECTOR
                sensorStatusMessage = "3DoF: Giroscópio + Acelerômetro (Estável)"
            }
            rotVectorSensor != null -> {
                sensorManager.registerListener(this, rotVectorSensor, SensorManager.SENSOR_DELAY_FASTEST)
                activeSensorType = TrackingSensorType.ROTATION_VECTOR
                sensorStatusMessage = "3DoF: Sensor Rotação Absoluto"
            }
            gyroSensor != null && accelSensor != null -> {
                sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_FASTEST)
                sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_FASTEST)
                activeSensorType = TrackingSensorType.ACCEL_GYRO_FUSION
                sensorStatusMessage = "3DoF: Fusão Giro + Acelerômetro"
            }
            accelSensor != null -> {
                sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_FASTEST)
                activeSensorType = TrackingSensorType.ACCELEROMETER_ONLY
                sensorStatusMessage = "3DoF: Acelerômetro"
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
            System.arraycopy(landscapeRotationMatrix, 0, initialRotationMatrix, 0, 16)
            hasCalibratedBaseline = true
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

            // Remap for landscape orientation (phone held horizontally in headset)
            // Portrait X -> Landscape -Y, Portrait Y -> Landscape X
            SensorManager.remapCoordinateSystem(
                rawRotationMatrix,
                SensorManager.AXIS_Y,
                SensorManager.AXIS_MINUS_X,
                landscapeRotationMatrix
            )

            if (!hasCalibratedBaseline) {
                System.arraycopy(landscapeRotationMatrix, 0, initialRotationMatrix, 0, 16)
                hasCalibratedBaseline = true
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun getHeadMatrix(outputMatrix: FloatArray) {
        synchronized(this) {
            // Compute relative device orientation matrix R_rel = (R_init)^T * R_current
            val initTransposed = FloatArray(16)
            Matrix.transposeM(initTransposed, 0, initialRotationMatrix, 0)

            val relativeOrientation = FloatArray(16)
            Matrix.multiplyMM(relativeOrientation, 0, initTransposed, 0, landscapeRotationMatrix, 0)

            // View Matrix is the inverse (transpose) of the camera's orientation in world space
            Matrix.transposeM(finalHeadViewMatrix, 0, relativeOrientation, 0)

            if (invertPitch || invertYaw) {
                val scaleX = if (invertYaw) -1.0f else 1.0f
                val scaleY = if (invertPitch) -1.0f else 1.0f
                Matrix.scaleM(finalHeadViewMatrix, 0, scaleX, scaleY, 1.0f)
            }

            System.arraycopy(finalHeadViewMatrix, 0, outputMatrix, 0, 16)
        }
    }
}
