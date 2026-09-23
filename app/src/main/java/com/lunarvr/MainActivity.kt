package com.lunarvr

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lunarvr.vr.VRRenderer
import com.lunarvr.vr.VRSession

class MainActivity : AppCompatActivity() {

    private lateinit var vrSession: VRSession
    private var vrRenderer: VRRenderer? = null
    private var glSurfaceView: GLSurfaceView? = null

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
        val perfText = "✦ Otimização: Modo ${report.recommendedPerformanceMode.name}"

        tvSensors.text = "$gyroText\n$accelText\n$perfText\n\n${vrSession.headTracking.sensorStatusMessage}"

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
            renderer.onRequestAudioPicker = {
                launchAudioPicker()
            }
            onAudioFileSelected = { uri, name ->
                renderer.handleAudioFileSelected(uri, name)
            }

            val surfaceView = GLSurfaceView(this).apply {
                setEGLContextClientVersion(2)
                setRenderer(renderer)
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            }
            glSurfaceView = surfaceView

            findViewById<android.widget.FrameLayout>(R.id.vr_surface_container).addView(surfaceView)

            vrSession.start()
            vrSession.recenterManager.triggerRecenter()
        } catch (e: Throwable) {
            Log.e("LunarVR", "Error starting VR Experience", e)
            Toast.makeText(this, "Erro na renderização VR: ${e.message}", Toast.LENGTH_LONG).show()
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
        } catch (e: Throwable) {
            Log.e("LunarVR", "onDestroy error", e)
        }
    }

    private val PICK_AUDIO_REQUEST = 303
    var onAudioFileSelected: ((android.net.Uri, String) -> Unit)? = null

    fun launchAudioPicker() {
        runOnUiThread {
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(android.content.Intent.CATEGORY_OPENABLE)
                    type = "audio/*"
                    putExtra(android.content.Intent.EXTRA_MIME_TYPES, arrayOf("audio/mpeg", "audio/mp3", "audio/*"))
                }
                startActivityForResult(intent, PICK_AUDIO_REQUEST)
            } catch (e: Exception) {
                android.util.Log.e("LunarVR", "Error launching audio picker", e)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_AUDIO_REQUEST && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            var displayName = "Música " + java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex != -1) {
                        val fn = cursor.getString(nameIndex)
                        if (!fn.isNullOrEmpty()) {
                            displayName = fn.removeSuffix(".mp3").removeSuffix(".MP3")
                        }
                    }
                }
            } catch (_: Exception) {}
            onAudioFileSelected?.invoke(uri, displayName)
        }
    }

}
