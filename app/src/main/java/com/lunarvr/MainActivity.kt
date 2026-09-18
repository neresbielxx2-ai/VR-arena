package com.lunarvr

import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import android.os.Bundle
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
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var vrSession: VRSession
    private lateinit var vrRenderer: VRRenderer
    private lateinit var permissionManager: PermissionManager
    private var glSurfaceView: GLSurfaceView? = null

    private var handTracker: HandTracker? = null
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private var splashLayout: View? = null
    private var vrContainer: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on & immersive landscape fullscreen
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()

        setContentView(R.layout.activity_main)

        vrSession = VRSession(this)
        permissionManager = PermissionManager(this)

        splashLayout = findViewById(R.id.splash_container)
        vrContainer = findViewById(R.id.vr_container)

        setupWelcomeScreen()
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
        )
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
        splashLayout?.visibility = View.GONE
        vrContainer?.visibility = View.VISIBLE

        vrRenderer = VRRenderer(this, vrSession)

        glSurfaceView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(2)
            setRenderer(vrRenderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }

        findViewById<android.widget.FrameLayout>(R.id.vr_surface_container).addView(glSurfaceView)

        vrSession.start()
        vrSession.recenterManager.triggerRecenter()

        // Check camera permission for Hand Tracking
        if (permissionManager.hasCameraPermission()) {
            initHandTracking()
        } else {
            permissionManager.requestCameraPermission()
        }
    }

    private fun initHandTracking() {
        // Try MediaPipe Hand Tracker first; fallback gracefully to Heuristic if assets/libs fail
        val mpTracker = MediaPipeHandTracker()
        mpTracker.initialize(this, object : HandTrackerListener {
            override fun onHandPoseUpdated(pose: HandPose) {
                vrRenderer.currentPose = pose
            }

            override fun onError(message: String) {
                // Switch to fallback tracker on error
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Lunar VR: Usando rastreamento de mão adaptativo", Toast.LENGTH_SHORT).show()
                }
                fallbackToHeuristicTracker()
            }
        })

        if (mpTracker.isInitialized) {
            handTracker = mpTracker
        } else {
            fallbackToHeuristicTracker()
        }

        startCameraSource()
    }

    private fun fallbackToHeuristicTracker() {
        val heuristic = HeuristicHandTracker()
        heuristic.initialize(this, object : HandTrackerListener {
            override fun onHandPoseUpdated(pose: HandPose) {
                vrRenderer.currentPose = pose
            }

            override fun onError(message: String) {}
        })
        handTracker = heuristic
    }

    private fun startCameraSource() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    handTracker?.processImageProxy(imageProxy)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis)
            } catch (e: Exception) {
                // Device might have no camera or camera in use
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionManager.CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initHandTracking()
            } else {
                Toast.makeText(this, "Lunar VR continua em 3DoF sem rastreamento de mão", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        glSurfaceView?.onResume()
        if (vrSession.isSessionActive) {
            vrSession.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView?.onPause()
        vrSession.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        vrSession.stop()
        handTracker?.release()
        cameraExecutor.shutdown()
    }
}
