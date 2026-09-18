package com.lunarvr

import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.lunarvr.handtracking.HandPose
import com.lunarvr.handtracking.HandTracker
import com.lunarvr.handtracking.HandTrackerListener
import com.lunarvr.handtracking.HeuristicHandTracker
import com.lunarvr.handtracking.MediaPipeHandTracker
import com.lunarvr.system.PermissionManager
import com.lunarvr.vr.VRRenderer
import com.lunarvr.vr.VRSession
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var vrSession: VRSession
    private var vrRenderer: VRRenderer? = null
    private lateinit var permissionManager: PermissionManager
    private var glSurfaceView: GLSurfaceView? = null

    private var handTracker: HandTracker? = null
    private var cameraExecutor: ExecutorService? = null

    private var splashLayout: View? = null
    private var vrContainer: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("LunarVR", "Uncaught exception in thread ${thread.name}", throwable)
        }

        try {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            hideSystemUI()

            setContentView(R.layout.activity_main)

            vrSession = VRSession(this)
            permissionManager = PermissionManager(this)

            splashLayout = findViewById(R.id.splash_container)
            vrContainer = findViewById(R.id.vr_container)

            setupWelcomeScreen()
        } catch (e: Throwable) {
            Log.e("LunarVR", "Error in onCreate", e)
            Toast.makeText(this, "Erro ao iniciar Lunar VR: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun hideSystemUI() {
        try {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        } catch (_: Exception) {}
    }

    private fun setupWelcomeScreen() {
        val btnStart = findViewById<Button>(R.id.btn_start_vr)
        val tvSensors = findViewById<TextView>(R.id.tv_sensor_info)

        val report = vrSession.hardwareReport
        val gyroText = if (report.hasGyroscope) "✓ Giroscópio Detectado" else "⚠ Sem Giroscópio (Fallback Ativo)"
        val accelText = if (report.hasAccelerometer) "✓ Acelerômetro Detectado" else "✗ Sem Acelerômetro"
        val camText = if (report.hasCamera) "✓ Câmera Detectada" else "⚠ Sem Câmera"
        val perfText = "✦ Otimização: Modo ${report.recommendedPerformanceMode.name}"

        tvSensors.text = "$gyroText\n$accelText\n$camText\n$perfText\n\n${vrSession.headTracking.sensorStatusMessage}"

        btnStart.setOnClickListener {
            startVRExperience()
        }
    }

    private fun startVRExperience() {
        try {
            splashLayout?.visibility = View.GONE
            vrContainer?.visibility = View.VISIBLE

            val renderer = VRRenderer(this, vrSession)
            vrRenderer = renderer

            val surfaceView = GLSurfaceView(this).apply {
                setEGLContextClientVersion(2)
                setRenderer(renderer)
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            }
            glSurfaceView = surfaceView

            findViewById<android.widget.FrameLayout>(R.id.vr_surface_container).addView(surfaceView)

            vrSession.start()
            vrSession.recenterManager.triggerRecenter()

            // Check camera permission for Hand Tracking
            if (permissionManager.hasCameraPermission()) {
                initHandTracking()
            } else {
                permissionManager.requestCameraPermission()
            }
        } catch (e: Throwable) {
            Log.e("LunarVR", "Error starting VR Experience", e)
            Toast.makeText(this, "Erro na renderização VR: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initHandTracking() {
        try {
            cameraExecutor = Executors.newSingleThreadExecutor()

            // Try MediaPipe Hand Tracker first; fallback gracefully to Heuristic if assets/libs fail
            val mpTracker = MediaPipeHandTracker()
            mpTracker.initialize(this, object : HandTrackerListener {
                override fun onHandPoseUpdated(pose: HandPose) {
                    vrRenderer?.currentPose = pose
                }

                override fun onError(message: String) {
                    runOnUiThread {
                        fallbackToHeuristicTracker()
                    }
                }
            })

            if (mpTracker.isInitialized) {
                handTracker = mpTracker
            } else {
                fallbackToHeuristicTracker()
            }

            startCameraSource()
        } catch (e: Throwable) {
            Log.e("LunarVR", "Hand tracking init failed, continuing 3DoF", e)
            fallbackToHeuristicTracker()
        }
    }

    private fun fallbackToHeuristicTracker() {
        try {
            val heuristic = HeuristicHandTracker()
            heuristic.initialize(this, object : HandTrackerListener {
                override fun onHandPoseUpdated(pose: HandPose) {
                    vrRenderer?.currentPose = pose
                }

                override fun onError(message: String) {}
            })
            handTracker = heuristic
        } catch (_: Throwable) {}
    }

    private fun startCameraSource() {
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    val exec = cameraExecutor
                    if (exec != null && !exec.isShutdown) {
                        imageAnalysis.setAnalyzer(exec) { imageProxy ->
                            handTracker?.processImageProxy(imageProxy)
                        }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis)
                    }
                } catch (e: Throwable) {
                    Log.w("LunarVR", "Camera binding skipped: ${e.message}")
                }
            }, ContextCompat.getMainExecutor(this))
        } catch (e: Throwable) {
            Log.w("LunarVR", "ProcessCameraProvider failed: ${e.message}")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionManager.CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initHandTracking()
            } else {
                Toast.makeText(this, "Lunar VR ativo em 3DoF (Câmera dispensada)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        try {
            glSurfaceView?.onResume()
            if (vrSession.isSessionActive) {
                vrSession.resume()
            }
        } catch (e: Throwable) {
            Log.e("LunarVR", "onResume error", e)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            glSurfaceView?.onPause()
            vrSession.pause()
        } catch (e: Throwable) {
            Log.e("LunarVR", "onPause error", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            vrSession.stop()
            handTracker?.release()
            cameraExecutor?.shutdown()
        } catch (e: Throwable) {
            Log.e("LunarVR", "onDestroy error", e)
        }
    }
}
