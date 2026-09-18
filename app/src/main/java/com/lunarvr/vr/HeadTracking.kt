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

    // Raw device rotation matrix (natural portrait)
    private val rawRotationMatrix = FloatArray(16)
    // Landscape remap
    private val landscapeMatrix = FloatArray(16)

    // Center offset yaw calibration
    private var yawOffsetDeg = 0.0f
    private val orientationVals = FloatArray(3)

    // Camera view matrix
    private val cameraViewMatrix = FloatArray(16)
    private var isCalibrated = false

    // Sensor Fallbacks
    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    init {
        Matrix.setIdentityM(rawRotationMatrix, 0)
        Matrix.setIdentityM(landscapeMatrix, 0)
        Matrix.setIdentityM(cameraViewMatrix, 0)
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
                sensorStatusMessage = "3DoF: Giroscópio VR Estável"
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
            SensorManager.getOrientation(landscapeMatrix, orientationVals)
            // Save current yaw in degrees as the new center forward
            yawOffsetDeg = Math.toDegrees(orientationVals[0].toDouble()).toFloat()
            isCalibrated = true
        }
        Log.d("LunarVR", "HeadTracking recentered. Yaw offset: $yawOffsetDeg")
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

            // Remap for landscape phone in headset (Surface.ROTATION_90 standard):
            // Phone top points to the left in headset: AXIS_Y maps to world X, AXIS_MINUS_X maps to world Y
            SensorManager.remapCoordinateSystem(
                rawRotationMatrix,
                SensorManager.AXIS_Y,
                SensorManager.AXIS_MINUS_X,
                landscapeMatrix
            )

            if (!isCalibrated) {
                SensorManager.getOrientation(landscapeMatrix, orientationVals)
                yawOffsetDeg = Math.toDegrees(orientationVals[0].toDouble()).toFloat()
                isCalibrated = true
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun getHeadMatrix(outputMatrix: FloatArray) {
        synchronized(this) {
            SensorManager.getOrientation(landscapeMatrix, orientationVals)

            // Absolute angles in degrees:
            // orientationVals[0]: Azimuth / Yaw (rotation around Z axis, pointing up)
            // orientationVals[1]: Pitch (rotation around X axis, pointing right)
            // orientationVals[2]: Roll (rotation around Y axis, pointing forward)
            val currentYaw = Math.toDegrees(orientationVals[0].toDouble()).toFloat() - yawOffsetDeg
            val currentPitch = Math.toDegrees(orientationVals[1].toDouble()).toFloat()
            val currentRoll = Math.toDegrees(orientationVals[2].toDouble()).toFloat()

            val yawSign = if (invertYaw) -1.0f else 1.0f
            val pitchSign = if (invertPitch) -1.0f else 1.0f

            // Clean Camera View Matrix:
            // In OpenGL View space:
            // Looking LEFT turns the camera view to the right (+Yaw)
            // Looking UP tilts the camera view down (+Pitch)
            Matrix.setIdentityM(cameraViewMatrix, 0)
            Matrix.rotateM(cameraViewMatrix, 0, currentRoll, 0f, 0f, 1f)
            Matrix.rotateM(cameraViewMatrix, 0, pitchSign * currentPitch, 1f, 0f, 0f)
            Matrix.rotateM(cameraViewMatrix, 0, yawSign * currentYaw, 0f, 1f, 0f)

            System.arraycopy(cameraViewMatrix, 0, outputMatrix, 0, 16)
        }
    }
}
