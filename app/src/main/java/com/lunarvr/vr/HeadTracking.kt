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
    var is6DofEnabled: Boolean = false
    var posX: Float = 0f
    var posY: Float = 0f
    var posZ: Float = 0f
    private var velX: Float = 0f
    private var velY: Float = 0f
    private var velZ: Float = 0f
    private var lastAccelTimestamp: Long = 0L

    // Raw device rotation matrix (natural portrait)
    private val rawRotationMatrix = FloatArray(16)
    // Landscape remap
    private val landscapeMatrix = FloatArray(16)

    // Center offset calibration: zero both yaw AND pitch on startup/recenter!
    // This guarantees the camera starts looking dead-center forward at 0° eye-level horizon!
    private var yawOffsetDeg = 0.0f
    private var pitchOffsetDeg = 0.0f
    private val orientationVals = FloatArray(3)

    // Camera view matrix
    private val cameraViewMatrix = FloatArray(16)
    private var isCalibrated = false
    private var initialFramesCountdown = 5 // Wait 5 sensor events for stable readings before initial zeroing

    // Sensor Fallbacks
    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    // Self-healing / Error recovery
    var trackingErrorDetected = false
        private set
    var recoveryMessage: String? = null
        private set
    private var lastValidEventTime = System.currentTimeMillis()

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
            // Save both yaw and pitch so whenever you recenter (or start),
            // your forward view is instantly leveled straight ahead without neck strain!
            yawOffsetDeg = Math.toDegrees(orientationVals[0].toDouble()).toFloat()
            pitchOffsetDeg = Math.toDegrees(orientationVals[1].toDouble()).toFloat()
            isCalibrated = true
            posX = 0f
            posY = 0f
            posZ = 0f
            velX = 0f
            velY = 0f
            velZ = 0f
            recoveryMessage = "Visão centralizada e nivelada"
        }
        Log.d("LunarVR", "HeadTracking recentered. Yaw offset: $yawOffsetDeg, Pitch offset: $pitchOffsetDeg")
    }

    override fun onSensorChanged(event: SensorEvent) {
        synchronized(this) {
            lastValidEventTime = System.currentTimeMillis()
            trackingErrorDetected = false

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

                    if (is6DofEnabled) {
                        val now = event.timestamp
                        if (lastAccelTimestamp != 0L) {
                            val dt = (now - lastAccelTimestamp) * 1e-9f
                            if (dt in 0.001f..0.1f) {
                                // Linear acceleration estimate removing gravity component
                                val ax = event.values[0] - (gravity[0] * 0.98f)
                                val ay = event.values[1] - (gravity[1] * 0.98f)
                                val az = event.values[2] - (gravity[2] * 0.98f)
                                velX = (velX + ax * dt) * 0.92f
                                velY = (velY + ay * dt) * 0.92f
                                velZ = (velZ + az * dt) * 0.92f
                                posX = (posX + velX * dt).coerceIn(-1.5f, 1.5f)
                                posY = (posY + velY * dt).coerceIn(-0.8f, 0.8f)
                                posZ = (posZ + velZ * dt).coerceIn(-1.5f, 1.5f)
                            }
                        }
                        lastAccelTimestamp = now
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

            // Automatic clean initial calibration after first few frames of reading
            if (!isCalibrated) {
                if (initialFramesCountdown > 0) {
                    initialFramesCountdown--
                } else {
                    SensorManager.getOrientation(landscapeMatrix, orientationVals)
                    yawOffsetDeg = Math.toDegrees(orientationVals[0].toDouble()).toFloat()
                    pitchOffsetDeg = Math.toDegrees(orientationVals[1].toDouble()).toFloat()
                    isCalibrated = true
            posX = 0f
            posY = 0f
            posZ = 0f
            velX = 0f
            velY = 0f
            velZ = 0f
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun checkSensorHealth(): String? {
        val elapsed = System.currentTimeMillis() - lastValidEventTime
        if (elapsed > 2000L && activeSensorType != TrackingSensorType.NONE) {
            // Auto-heal: restart listener
            stop()
            start()
            recenter()
            return "Reconectando sensores VR..."
        }
        return null
    }

    fun getHeadMatrix(outputMatrix: FloatArray) {
        synchronized(this) {
            // Detect if orientation values are NaN or degenerate
            try {
                SensorManager.getOrientation(landscapeMatrix, orientationVals)

                val rawYaw = Math.toDegrees(orientationVals[0].toDouble()).toFloat()
                val rawPitch = Math.toDegrees(orientationVals[1].toDouble()).toFloat()
                val rawRoll = Math.toDegrees(orientationVals[2].toDouble()).toFloat()

                // If values are NaN, self-heal:
                if (rawYaw.isNaN() || rawPitch.isNaN() || rawRoll.isNaN()) {
                    Matrix.setIdentityM(outputMatrix, 0)
                    return
                }

                // Apply initial calibrated offsets:
                // Looking straight ahead -> relative angles are zero!
                val currentYaw = rawYaw - yawOffsetDeg
                val currentPitch = rawPitch - pitchOffsetDeg
                val currentRoll = rawRoll

                val yawSign = if (invertYaw) -1.0f else 1.0f
                val pitchSign = if (invertPitch) -1.0f else 1.0f

                // Clean Camera View Matrix:
                Matrix.setIdentityM(cameraViewMatrix, 0)
                if (is6DofEnabled) {
                    Matrix.translateM(cameraViewMatrix, 0, -posX, -posY, -posZ)
                }
                Matrix.rotateM(cameraViewMatrix, 0, currentRoll, 0f, 0f, 1f)
                Matrix.rotateM(cameraViewMatrix, 0, pitchSign * currentPitch, 1f, 0f, 0f)
                Matrix.rotateM(cameraViewMatrix, 0, yawSign * currentYaw, 0f, 1f, 0f)

                System.arraycopy(cameraViewMatrix, 0, outputMatrix, 0, 16)
            } catch (e: Throwable) {
                Log.e("LunarVR", "Error updating head matrix", e)
                Matrix.setIdentityM(outputMatrix, 0)
            }
        }
    }
}
