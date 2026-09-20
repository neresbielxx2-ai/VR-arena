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
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.result.contract.ActivityResultContracts
import com.lunarvr.vr.VRRenderer
import com.lunarvr.vr.VRSession

class MainActivity : AppCompatActivity() {

    private lateinit var vrSession: VRSession
    private var vrRenderer: VRRenderer? = null
    private var glSurfaceView: GLSurfaceView? = null

    private var splashLayout: View? = null
    private var vrContainer: View? = null
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val fileName = queryFileName(uri)
                vrRenderer?.handleModelImported(uri, fileName)
            }
        }
    }

    private fun queryFileName(uri: Uri): String {
        var name = "custom_model.glb"
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (e: Exception) {
            Log.e("LunarVR", "Error querying file name", e)
        }
        return name
    }

    private fun launchNativeFilePicker() {
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("model/gltf-binary", "application/octet-stream", "*/*"))
            }
            filePickerLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e("LunarVR", "Failed to launch native file picker", e)
            Toast.makeText(this, "Não foi possível abrir o gerenciador de arquivos", Toast.LENGTH_SHORT).show()
        }
    }


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
            renderer.onModelImportRequested = {
                launchNativeFilePicker()
            }
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
}
