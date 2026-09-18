package com.lunarvr.vr

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.Matrix
import android.view.Surface
import android.view.WindowManager

enum class TrackingSensorType {
    ROTATION_VECTOR,
    GAME_ROTATION_VECTOR,
    ACCEL_GYRO_FUSION,
    ACCELEROMETER_ONLY,
    NONE
}

class HeadTracking(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager

    var activeSensorType: TrackingSensorType = TrackingSensorType.NONE
        private set
    var sensorStatusMessage: String = "Iniciando sensores..."
        private set

    var invertPitch: Boolean = false
    var invertYaw: Boolean = false

    private val rawRotationMatrix = FloatArray(16)
    private val remappedMatrix = FloatArray(16)
    private val centerOffsetMatrix = FloatArray(16)
    private val finalHeadMatrix = FloatArray(16)

    // Fallback complementary filter variables
    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    init {
        Matrix.setIdentityM(rawRotationMatrix, 0)
        Matrix.setIdentityM(remappedMatrix, 0)
        Matrix.setIdentityM(centerOffsetMatrix, 0)
        Matrix.setIdentityM(finalHeadMatrix, 0)
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
            rotVectorSensor != null -> {
                sensorManager.registerListener(this, rotVectorSensor, SensorManager.SENSOR_DELAY_GAME)
                activeSensorType = TrackingSensorType.ROTATION_VECTOR
                sensorStatusMessage = "3DoF: Sensor Rotação Absoluto (Alta precisão)"
            }
            gameRotSensor != null -> {
                sensorManager.registerListener(this, gameRotSensor, SensorManager.SENSOR_DELAY_GAME)
                activeSensorType = TrackingSensorType.GAME_ROTATION_VECTOR
                sensorStatusMessage = "3DoF: Giroscópio + Acelerômetro (Sem bússola)"
            }
            gyroSensor != null && accelSensor != null -> {
                sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_GAME)
                sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_GAME)
                activeSensorType = TrackingSensorType.ACCEL_GYRO_FUSION
                sensorStatusMessage = "3DoF: Fusão Giroscópio + Acelerômetro"
            }
            accelSensor != null -> {
                sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_GAME)
                activeSensorType = TrackingSensorType.ACCELEROMETER_ONLY
                sensorStatusMessage = "3DoF Reduzido: Apenas acelerômetro (Inclinômetro)"
            }
            else -> {
                activeSensorType = TrackingSensorType.NONE
                sensorStatusMessage = "Nenhum sensor de orientação encontrado"
            }
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    fun recenter() {
        synchronized(this) {
            // Invert the current raw orientation to cancel it out as the new identity
            Matrix.invertM(centerOffsetMatrix, 0, rawRotationMatrix, 0)
        }
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
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun getHeadMatrix(outputMatrix: FloatArray) {
        synchronized(this) {
            val rotation = windowManager?.defaultDisplay?.rotation ?: Surface.ROTATION_90
            var axisX = SensorManager.AXIS_X
            var axisY = SensorManager.AXIS_Y

            // Landscape orientation compensation
            when (rotation) {
                Surface.ROTATION_0 -> {
                    axisX = SensorManager.AXIS_X
                    axisY = SensorManager.AXIS_Y
                }
                Surface.ROTATION_90 -> {
                    axisX = SensorManager.AXIS_Y
                    axisY = SensorManager.AXIS_MINUS_X
                }
                Surface.ROTATION_180 -> {
                    axisX = SensorManager.AXIS_MINUS_X
                    axisY = SensorManager.AXIS_MINUS_Y
                }
                Surface.ROTATION_270 -> {
                    axisX = SensorManager.AXIS_MINUS_Y
                    axisY = SensorManager.AXIS_X
                }
            }

            SensorManager.remapCoordinateSystem(rawRotationMatrix, axisX, axisY, remappedMatrix)

            // Multiply centerOffsetMatrix * remappedMatrix -> finalHeadMatrix
            Matrix.multiplyMM(finalHeadMatrix, 0, centerOffsetMatrix, 0, remappedMatrix, 0)

            // Invert axes if requested
            if (invertPitch || invertYaw) {
                val scaleX = if (invertYaw) -1.0f else 1.0f
                val scaleY = if (invertPitch) -1.0f else 1.0f
                Matrix.scaleM(finalHeadMatrix, 0, scaleX, scaleY, 1.0f)
            }

            System.arraycopy(finalHeadMatrix, 0, outputMatrix, 0, 16)
        }
    }
}
