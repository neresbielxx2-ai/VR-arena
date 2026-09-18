package com.lunarvr.vr

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import com.lunarvr.browser.BrowserController
import com.lunarvr.browser.BrowserView
import com.lunarvr.browser.URLBar
import com.lunarvr.environment.EnvironmentManager
import com.lunarvr.handtracking.InteractionManager
import com.lunarvr.handtracking.Ray3D
import com.lunarvr.keyboard.TextInputManager
import com.lunarvr.keyboard.VRKey
import com.lunarvr.keyboard.VRKeyboard
import com.lunarvr.keyboard.VRKeyboardListener
import com.lunarvr.ui.EnvironmentPanel
import com.lunarvr.ui.GrabHandle
import com.lunarvr.ui.LunarBar
import com.lunarvr.ui.LunarNavDestination
import com.lunarvr.ui.ModernIcons
import com.lunarvr.ui.SettingsPanel
import com.lunarvr.ui.VRPanel
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.Random
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class VRRenderer(
    private val context: Context,
    private val vrSession: VRSession
) : GLSurfaceView.Renderer {

    // Interaction & Gaze Pointing
    val interactionManager = InteractionManager()

    // Environment System
    val environmentManager = EnvironmentManager()

    // Subsystems
    val lunarBar = LunarBar(
        onNavigate = { dest -> handleNavigation(dest) },
        onRecenter = { vrSession.recenterManager.triggerRecenter() }
    )

    val settingsPanel = SettingsPanel(vrSession) {
        refreshInteractiveElements()
    }

    val environmentPanel = EnvironmentPanel(environmentManager) {
        refreshInteractiveElements()
    }

    val browserController = BrowserController()
    var browserView: BrowserView? = null
    val urlBar = URLBar(
        onUrlClick = { openKeyboardForUrl() },
        onBackClick = { browserView?.goBack() },
        onForwardClick = { browserView?.goForward() },
        onRefreshClick = { browserView?.reload() },
        onHomeClick = { browserView?.loadUrl("https://www.google.com") }
    )

    val vrKeyboard = VRKeyboard()
    val textInputManager = TextInputManager()
    private val keyboardButtons = mutableListOf<VRKey>()

    // VR UI Panels
    private var barPanel: VRPanel? = null
    private var browserPanel: VRPanel? = null
    private var urlPanel: VRPanel? = null
    private var settingsVRPanel: VRPanel? = null
    private var envVRPanel: VRPanel? = null
    private var keyboardVRPanel: VRPanel? = null

    // Meta Quest style App Window Drag Handles: independent move handle under EACH open app window!
    private var browserGrabHandle = GrabHandle("grab_browser", 0.0f, -0.32f, -1.35f, 0.42f, 0.06f)
    private var settingsGrabHandle = GrabHandle("grab_settings", 0.0f, -0.38f, -1.30f, 0.42f, 0.06f)
    private var envGrabHandle = GrabHandle("grab_env", 0.0f, -0.38f, -1.30f, 0.42f, 0.06f)
    private var keyboardGrabHandle = GrabHandle("grab_keyboard", 0.0f, -0.34f, -1.25f, 0.42f, 0.06f)

    // Spherical window coordinates (free 360 rotation around user, height up/down, no walls!)
    private var browserYawDeg: Float = 0f
    private var browserHeightY: Float = 0.08f
    private var settingsYawDeg: Float = 0f
    private var settingsHeightY: Float = 0.10f
    private var envYawDeg: Float = 0f
    private var envHeightY: Float = 0.10f
    private var keyboardYawDeg: Float = 0f
    private var keyboardHeightY: Float = -0.05f

    // Starfield background
    private var starCount = 350
    private var starBuffer: FloatBuffer? = null
    private var starProgram = 0

    // GL Panel Shaders
    private var panelProgram = 0
    private var aPosHandle = 0
    private var aTexHandle = 0
    private var uMvpHandle = 0

    // Dimensions
    private var screenWidth = 1920
    private var screenHeight = 1080

    // Navigation State (single active window visible at a time to prevent overlap!)
    private var currentDestination = LunarNavDestination.HOME

    private val headViewMatrix = FloatArray(16)
    private val viewProjectionMatrix = FloatArray(16)

    // Center Crosshair / Gaze Reticle
    private var reticleProgram = 0
    private var reticleBuffer: FloatBuffer? = null

    // Self-healing notification toast in VR
    private var notificationMessage: String? = null
    private var notificationEndTime: Long = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        try {
            val clear = environmentManager.getClearColor()
            GLES20.glClearColor(clear[0], clear[1], clear[2], clear[3])
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)

            initShaders()
            initStarfield()
            initReticle()
            initPanels()

            browserView = BrowserView(context, browserController)

            // Dynamic Spherical Drag Handlers: smoothly reposition panels 360° around user without invisible walls!
            lunarBar.grabHandle.onDragUpdateSpherical = { yaw, height, dist ->
                lunarBar.setSphericalPosition(yaw, height, dist)
                barPanel?.let {
                    it.x = lunarBar.posX
                    it.y = lunarBar.posY
                    it.z = lunarBar.posZ
                    it.rotationYDeg = -yaw
                }
            }

            browserGrabHandle.onDragUpdateSpherical = { yaw, height, dist ->
                browserYawDeg = yaw
                browserHeightY = height
                val rad = Math.toRadians(yaw.toDouble())
                val bx = (dist * Math.sin(rad)).toFloat()
                val bz = (-dist * Math.cos(rad)).toFloat()

                browserPanel?.let {
                    it.x = bx
                    it.y = height
                    it.z = bz
                    it.rotationYDeg = -yaw
                }
                urlPanel?.let {
                    it.x = bx
                    it.y = height + 0.38f
                    it.z = bz
                    it.rotationYDeg = -yaw
                }
                browserGrabHandle.x = bx
                browserGrabHandle.y = height - 0.38f
                browserGrabHandle.z = bz
                urlBar.setupButtons(bx, height + 0.38f)
            }

            settingsGrabHandle.onDragUpdateSpherical = { yaw, height, dist ->
                settingsYawDeg = yaw
                settingsHeightY = height
                val rad = Math.toRadians(yaw.toDouble())
                val sx = (dist * Math.sin(rad)).toFloat()
                val sz = (-dist * Math.cos(rad)).toFloat()

                settingsVRPanel?.let {
                    it.x = sx
                    it.y = height
                    it.z = sz
                    it.rotationYDeg = -yaw
                }
                settingsGrabHandle.x = sx
                settingsGrabHandle.y = height - 0.44f
                settingsGrabHandle.z = sz
                settingsPanel.setupButtons(sx, height)
            }

            envGrabHandle.onDragUpdateSpherical = { yaw, height, dist ->
                envYawDeg = yaw
                envHeightY = height
                val rad = Math.toRadians(yaw.toDouble())
                val ex = (dist * Math.sin(rad)).toFloat()
                val ez = (-dist * Math.cos(rad)).toFloat()

                envVRPanel?.let {
                    it.x = ex
                    it.y = height
                    it.z = ez
                    it.rotationYDeg = -yaw
                }
                envGrabHandle.x = ex
                envGrabHandle.y = height - 0.38f
                envGrabHandle.z = ez
                environmentPanel.setupButtons(ex, height)
            }

            keyboardGrabHandle.onDragUpdateSpherical = { yaw, height, dist ->
                keyboardYawDeg = yaw
                keyboardHeightY = height
                val rad = Math.toRadians(yaw.toDouble())
                val kx = (dist * Math.sin(rad)).toFloat()
                val kz = (-dist * Math.cos(rad)).toFloat()

                keyboardVRPanel?.let {
                    it.x = kx
                    it.y = height
                    it.z = kz
                    it.rotationYDeg = -yaw
                }
                keyboardGrabHandle.x = kx
                keyboardGrabHandle.y = height - 0.28f
                keyboardGrabHandle.z = kz
                setupKeyboardButtons(kx, height, kz)
            }

            vrKeyboard.listener = object : VRKeyboardListener {
                override fun onKeyPressed(character: String) {
                    textInputManager.append(character)
                }
                override fun onBackspace() {
                    textInputManager.backspace()
                }
                override fun onSpace() {
                    textInputManager.appendSpace()
                }
                override fun onEnter() {
                    textInputManager.submit()
                }
                override fun onCloseKeyboard() {
                    vrKeyboard.isVisible = false
                    refreshInteractiveElements()
                }
            }

            refreshInteractiveElements()
        } catch (e: Throwable) {
            Log.e("LunarVR", "Error in onSurfaceCreated", e)
            showNotification("Recuperando subsistema gráfico...")
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        screenWidth = if (width > 0) width else 1920
        screenHeight = if (height > 0) height else 1080
    }

    fun showNotification(msg: String) {
        notificationMessage = msg
        notificationEndTime = System.currentTimeMillis() + 3000L
    }

    override fun onDrawFrame(gl: GL10?) {
        try {
            val clear = environmentManager.getClearColor()
            GLES20.glClearColor(clear[0], clear[1], clear[2], clear[3])
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

            // Health monitor & auto error recovery
            val healthError = vrSession.headTracking.checkSensorHealth()
            if (healthError != null) {
                showNotification(healthError)
            }

            // Read sensor orientation (View Matrix)
            vrSession.headTracking.getHeadMatrix(headViewMatrix)

            // Gaze Ray in World Space:
            // Camera position is at world (0,0,0).
            // Camera forward vector in world coordinates is row 2 of View Matrix negated:
            val fwdX = -headViewMatrix[2]
            val fwdY = -headViewMatrix[6]
            val fwdZ = -headViewMatrix[10]
            val gazeRay = Ray3D(0f, 0f, 0f, fwdX, fwdY, fwdZ)

            interactionManager.update(gazeRay)

            // Spherical coordinate tracking (Yaw angle in degrees and vertical height Y)
            // No invisible wall collision! Freely turns around 360° and raises/lowers cleanly.
            val gazeYawDeg = Math.toDegrees(Math.atan2(fwdX.toDouble(), -fwdZ.toDouble())).toFloat()
            val gazeHeightY = fwdY * 1.35f

            // Update any active grab handles:
            // If looked at for 2 seconds, grab handle locks and tracks spherical gaze, then automatically unlocks after 2s
            lunarBar.grabHandle.updateGrabSpherical(gazeYawDeg, (gazeHeightY - 0.15f).coerceIn(-0.7f, 0.5f), 1.35f)
            browserGrabHandle.updateGrabSpherical(gazeYawDeg, gazeHeightY.coerceIn(-0.6f, 0.6f), 1.35f)
            settingsGrabHandle.updateGrabSpherical(gazeYawDeg, gazeHeightY.coerceIn(-0.6f, 0.6f), 1.30f)
            envGrabHandle.updateGrabSpherical(gazeYawDeg, gazeHeightY.coerceIn(-0.6f, 0.6f), 1.30f)
            keyboardGrabHandle.updateGrabSpherical(gazeYawDeg, gazeHeightY.coerceIn(-0.6f, 0.4f), 1.25f)

            // Update dynamic UI textures
            updateBarPanel()
            if (currentDestination == LunarNavDestination.BROWSER) {
                updateBrowserPanels()
            } else if (currentDestination == LunarNavDestination.SETTINGS) {
                updateSettingsPanel()
            } else if (currentDestination == LunarNavDestination.ENVIRONMENTS) {
                updateEnvironmentPanel()
            }

            if (vrKeyboard.isVisible) {
                updateKeyboardPanel()
            }

            val halfWidth = screenWidth / 2

            // Left Eye Render
            GLES20.glViewport(0, 0, halfWidth, screenHeight)
            vrSession.stereoCamera.updateProjection(halfWidth, screenHeight)
            vrSession.stereoCamera.computeEyeMatrices(headViewMatrix)
            Matrix.multiplyMM(
                viewProjectionMatrix, 0,
                vrSession.stereoCamera.getProjectionMatrix(), 0,
                vrSession.stereoCamera.getLeftEyeViewMatrix(), 0
            )
            renderScene(viewProjectionMatrix, vrSession.stereoCamera.getProjectionMatrix())

            // Right Eye Render
            GLES20.glViewport(halfWidth, 0, halfWidth, screenHeight)
            Matrix.multiplyMM(
                viewProjectionMatrix, 0,
                vrSession.stereoCamera.getProjectionMatrix(), 0,
                vrSession.stereoCamera.getRightEyeViewMatrix(), 0
            )
            renderScene(viewProjectionMatrix, vrSession.stereoCamera.getProjectionMatrix())
        } catch (e: Throwable) {
            Log.e("LunarVR", "Error in onDrawFrame", e)
            showNotification("Auto-recuperação do renderizador...")
        }
    }

    private fun renderScene(vpMatrix: FloatArray, projMatrix: FloatArray) {
        // Draw Starfield in World space
        drawStarfield(vpMatrix)

        if (panelProgram == 0) return

        // Draw Panels in World space
        GLES20.glUseProgram(panelProgram)

        // Floating Lunar Bar
        barPanel?.bindAndRender(panelProgram, vpMatrix, aPosHandle, aTexHandle, uMvpHandle)

        // Only ONE app window open at a time to prevent any overlap!
        if (currentDestination == LunarNavDestination.BROWSER) {
            urlPanel?.bindAndRender(panelProgram, vpMatrix, aPosHandle, aTexHandle, uMvpHandle)
            browserPanel?.bindAndRender(panelProgram, vpMatrix, aPosHandle, aTexHandle, uMvpHandle)
        } else if (currentDestination == LunarNavDestination.SETTINGS) {
            settingsVRPanel?.bindAndRender(panelProgram, vpMatrix, aPosHandle, aTexHandle, uMvpHandle)
        } else if (currentDestination == LunarNavDestination.ENVIRONMENTS) {
            envVRPanel?.bindAndRender(panelProgram, vpMatrix, aPosHandle, aTexHandle, uMvpHandle)
        }

        // Virtual Keyboard
        if (vrKeyboard.isVisible) {
            keyboardVRPanel?.bindAndRender(panelProgram, vpMatrix, aPosHandle, aTexHandle, uMvpHandle)
        }

        // Draw Gaze Pointer in camera view space
        drawReticle(projMatrix)
    }

    private fun drawReticle(projMatrix: FloatArray) {
        val rBuf = reticleBuffer ?: return
        if (reticleProgram == 0) return

        GLES20.glUseProgram(reticleProgram)
        val mvp = GLES20.glGetUniformLocation(reticleProgram, "uMVPMatrix")
        val color = GLES20.glGetUniformLocation(reticleProgram, "vColor")
        val pos = GLES20.glGetAttribLocation(reticleProgram, "vPosition")

        val model = FloatArray(16)
        Matrix.setIdentityM(model, 0)
        Matrix.translateM(model, 0, 0f, 0f, -1.0f)

        val mvpMatrix = FloatArray(16)
        Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, model, 0)

        GLES20.glUniformMatrix4fv(mvp, 1, false, mvpMatrix, 0)

        val isHovering = interactionManager.currentProgress > 0f || lunarBar.grabHandle.isGrabbed
        if (isHovering) {
            GLES20.glUniform4f(color, 0.0f, 1.0f, 0.85f, 1.0f)
        } else {
            GLES20.glUniform4f(color, 0.0f, 0.90f, 1.0f, 0.8f)
        }

        rBuf.position(0)
        GLES20.glEnableVertexAttribArray(pos)
        GLES20.glVertexAttribPointer(pos, 3, GLES20.GL_FLOAT, false, 0, rBuf)

        GLES20.glLineWidth(3.5f)
        GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 0, 24)
        GLES20.glDisableVertexAttribArray(pos)
    }

    private fun initReticle() {
        val segments = 24
        val radius = 0.016f
        val coords = FloatArray(segments * 3)
        for (i in 0 until segments) {
            val angle = 2.0 * Math.PI * i / segments
            coords[i * 3] = (radius * Math.cos(angle)).toFloat()
            coords[i * 3 + 1] = (radius * Math.sin(angle)).toFloat()
            coords[i * 3 + 2] = 0f
        }

        val buf = ByteBuffer.allocateDirect(coords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        buf.put(coords).position(0)
        reticleBuffer = buf

        val vs = """
            uniform mat4 uMVPMatrix;
            attribute vec4 vPosition;
            void main() {
                gl_Position = uMVPMatrix * vPosition;
            }
        """.trimIndent()

        val fs = """
            precision mediump float;
            uniform vec4 vColor;
            void main() {
                gl_FragColor = vColor;
            }
        """.trimIndent()

        reticleProgram = createProgram(vs, fs)
    }

    private fun drawStarfield(vpMatrix: FloatArray) {
        val sBuf = starBuffer ?: return
        if (starProgram == 0) return

        GLES20.glUseProgram(starProgram)
        val mvp = GLES20.glGetUniformLocation(starProgram, "uMVPMatrix")
        val color = GLES20.glGetUniformLocation(starProgram, "vColor")
        val pos = GLES20.glGetAttribLocation(starProgram, "vPosition")

        val starCol = environmentManager.getStarColor()
        GLES20.glUniformMatrix4fv(mvp, 1, false, vpMatrix, 0)
        GLES20.glUniform4f(color, starCol[0], starCol[1], starCol[2], starCol[3])

        sBuf.position(0)
        GLES20.glEnableVertexAttribArray(pos)
        GLES20.glVertexAttribPointer(pos, 3, GLES20.GL_FLOAT, false, 0, sBuf)

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, starCount)
        GLES20.glDisableVertexAttribArray(pos)
    }

    private fun handleNavigation(dest: LunarNavDestination) {
        if (currentDestination == dest && dest != LunarNavDestination.HOME) {
            currentDestination = LunarNavDestination.HOME
        } else {
            currentDestination = dest
        }

        settingsPanel.isVisible = (currentDestination == LunarNavDestination.SETTINGS)
        environmentPanel.isVisible = (currentDestination == LunarNavDestination.ENVIRONMENTS)
        if (currentDestination != LunarNavDestination.BROWSER) {
            vrKeyboard.isVisible = false
        }
        refreshInteractiveElements()
    }

    private fun openKeyboardForUrl() {
        vrKeyboard.show()
        textInputManager.bindTarget(object : TextInputManager.TextInputTarget {
            override fun onTextUpdated(text: String) {
                urlBar.displayUrl = text
                urlBar.setupButtons(urlPanel?.x ?: 0f, urlPanel?.y ?: 0.45f)
            }

            override fun onInputSubmitted(text: String) {
                vrKeyboard.hide()
                browserController.updateUrl(text)
                browserView?.loadUrl(browserController.currentState.currentUrl)
                refreshInteractiveElements()
            }
        }, urlBar.displayUrl)
        setupKeyboardButtons(keyboardVRPanel?.x ?: 0f, keyboardVRPanel?.y ?: -0.05f, keyboardVRPanel?.z ?: -1.25f)
        refreshInteractiveElements()
    }

    private fun refreshInteractiveElements() {
        interactionManager.clear()

        // Always register Lunar Bar buttons and its grab handle
        for (btn in lunarBar.buttons) {
            interactionManager.register(btn)
        }
        interactionManager.register(lunarBar.grabHandle)

        // Register Browser buttons and drag handle if Browser is open
        if (currentDestination == LunarNavDestination.BROWSER) {
            for (btn in urlBar.buttons) {
                interactionManager.register(btn)
            }
            interactionManager.register(browserGrabHandle)
        }

        // Register Settings buttons and drag handle if Settings is open
        if (currentDestination == LunarNavDestination.SETTINGS) {
            for (btn in settingsPanel.buttons) {
                interactionManager.register(btn)
            }
            interactionManager.register(settingsGrabHandle)
        }

        // Register Environments buttons and drag handle if Environments is open
        if (currentDestination == LunarNavDestination.ENVIRONMENTS) {
            for (btn in environmentPanel.buttons) {
                interactionManager.register(btn)
            }
            interactionManager.register(envGrabHandle)
        }

        // Register Keyboard buttons and its drag handle if visible
        if (vrKeyboard.isVisible) {
            for (btn in keyboardButtons) {
                interactionManager.register(btn)
            }
            interactionManager.register(keyboardGrabHandle)
        }
    }

    private fun setupKeyboardButtons(centerX: Float = 0f, centerY: Float = -0.05f, centerZ: Float = -1.25f) {
        keyboardButtons.clear()
        val rows = vrKeyboard.getCurrentRows()
        val startY = centerY + 0.16f
        val btnH = 0.065f

        for (r in rows.indices) {
            val row = rows[r]
            val btnW = 0.94f / row.size
            val startX = centerX - 0.47f + (btnW / 2.0f)
            val y = startY - (r * 0.075f)

            for (c in row.indices) {
                val key = row[c]
                val x = startX + (c * btnW)
                keyboardButtons.add(
                    VRKey("key_${key}_$r", key, x, y, centerZ, btnW * 0.92f, btnH) {
                        vrKeyboard.handleKeyPress(key)
                        if (key in listOf("SHIFT", "shift", "?123", "ABC")) {
                            setupKeyboardButtons(centerX, centerY, centerZ)
                            refreshInteractiveElements()
                        }
                    }
                )
            }
        }
    }

    private fun updateBarPanel() {
        lunarBar.updateClock()
        barPanel?.drawCustom { canvas, paint ->
            canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)

            // Meta Quest inspired floating dock pill
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#EE101625")
            canvas.drawRoundRect(RectF(14f, 14f, 1010f, 206f), 38f, 38f, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.5f
            paint.color = Color.parseColor("#38BDF8")
            canvas.drawRoundRect(RectF(14f, 14f, 1010f, 206f), 38f, 38f, paint)

            // Header status line: Title, Status, Battery Icon, Time
            paint.style = Paint.Style.FILL
            paint.textSize = 23f
            paint.color = Color.parseColor("#94A3B8")
            val statusTxt = if (notificationMessage != null && System.currentTimeMillis() < notificationEndTime) {
                "⚡ ${notificationMessage}"
            } else {
                "LUNAR OS  |  ${lunarBar.vrStatus}"
            }
            canvas.drawText(statusTxt, 36f, 46f, paint)
            canvas.drawText(lunarBar.currentTimeString, 910f, 46f, paint)

            // Clean vector battery indicator
            ModernIcons.drawBatteryIcon(canvas, paint, 790f, 40f, lunarBar.batteryPercentage)

            // Buttons: 5 actions (Início, Navegador, Cenários, Centralizar, Ajustes)
            val btnW = 186f
            val btnH = 120f
            val by = 68f

            for (i in lunarBar.buttons.indices) {
                val btn = lunarBar.buttons[i]
                val bx = 26f + i * 196f

                // Meta Quest rounded rect card
                paint.style = Paint.Style.FILL
                if (btn.isHovered) {
                    paint.color = Color.parseColor("#1E2D4A")
                } else {
                    paint.color = Color.parseColor("#151E32")
                }
                canvas.drawRoundRect(RectF(bx, by, bx + btnW, by + btnH), 22f, 22f, paint)

                paint.style = Paint.Style.STROKE
                paint.strokeWidth = if (btn.isHovered) 3.5f else 1.8f
                paint.color = if (btn.isHovered) Color.parseColor("#00E5FF") else Color.parseColor("#25344F")
                canvas.drawRoundRect(RectF(bx, by, bx + btnW, by + btnH), 22f, 22f, paint)

                // Hover progress
                if (btn.isHovered && btn.hoverProgress > 0f) {
                    paint.color = Color.parseColor("#00E5FF")
                    paint.strokeWidth = 7f
                    val progressW = (btnW - 24f) * btn.hoverProgress
                    canvas.drawLine(bx + 12f, by + btnH - 8f, bx + 12f + progressW, by + btnH - 8f, paint)
                }

                // Clean Vector Icons
                val iconCx = bx + btnW / 2f
                val iconCy = by + 45f
                val iconColor = if (btn.isHovered) Color.parseColor("#00E5FF") else Color.parseColor("#E2E8F0")

                when (i) {
                    0 -> ModernIcons.drawHomeIcon(canvas, paint, iconCx, iconCy, 36f, iconColor)
                    1 -> ModernIcons.drawGlobeIcon(canvas, paint, iconCx, iconCy, 36f, iconColor)
                    2 -> ModernIcons.drawEnvironmentIcon(canvas, paint, iconCx, iconCy, 36f, iconColor)
                    3 -> ModernIcons.drawRecenterIcon(canvas, paint, iconCx, iconCy, 36f, iconColor)
                    4 -> ModernIcons.drawSettingsIcon(canvas, paint, iconCx, iconCy, 36f, iconColor)
                }

                // Label
                paint.style = Paint.Style.FILL
                paint.textSize = 21f
                paint.color = Color.parseColor("#F8FAFC")
                val textW = paint.measureText(btn.label)
                canvas.drawText(btn.label, bx + (btnW - textW) / 2f, by + 100f, paint)
            }

            // Drag Handle Bar (2 seconds lock)
            val grab = lunarBar.grabHandle
            ModernIcons.drawDragHandle(canvas, paint, 512f, 230f, 280f, 22f, grab.isHovered, grab.isGrabbed, grab.hoverProgress)
        }
    }

    private fun updateBrowserPanels() {
        // Draw URL bar with interactive address bar
        urlPanel?.drawCustom { canvas, paint ->
            canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)

            // Shell
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#F0101625")
            canvas.drawRoundRect(RectF(10f, 10f, 1014f, 118f), 24f, 24f, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.5f
            paint.color = Color.parseColor("#2D3C58")
            canvas.drawRoundRect(RectF(10f, 10f, 1014f, 118f), 24f, 24f, paint)

            // Navigation icons
            paint.style = Paint.Style.FILL
            paint.textSize = 34f
            paint.color = Color.parseColor("#CBD5E1")
            canvas.drawText("◀   ▶   ↻   ✦", 40f, 75f, paint)

            // Interactive Search/URL field (click to open VR keyboard)
            paint.color = Color.parseColor("#18233A")
            canvas.drawRoundRect(RectF(320f, 25f, 990f, 100f), 18f, 18f, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            paint.color = Color.parseColor("#38BDF8")
            canvas.drawRoundRect(RectF(320f, 25f, 990f, 100f), 18f, 18f, paint)

            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#00E5FF")
            paint.textSize = 28f
            val displayTxt = if (urlBar.displayUrl.length > 38) urlBar.displayUrl.take(38) + "..." else urlBar.displayUrl
            canvas.drawText("🔍  $displayTxt", 345f, 72f, paint)
        }

        // Draw WebView content
        val bmp = browserView?.captureBitmap()
        if (bmp != null) {
            browserPanel?.copyBitmap(bmp)
        }
    }

    private fun updateEnvironmentPanel() {
        envVRPanel?.drawCustom { canvas, paint ->
            canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)

            // Background panel
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#F50D1322")
            canvas.drawRoundRect(RectF(10f, 10f, 1014f, 620f), 35f, 35f, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            paint.color = Color.parseColor("#38BDF8")
            canvas.drawRoundRect(RectF(10f, 10f, 1014f, 620f), 35f, 35f, paint)

            // Header Icon and Title
            ModernIcons.drawEnvironmentIcon(canvas, paint, 60f, 65f, 34f, Color.parseColor("#00E5FF"))

            paint.style = Paint.Style.FILL
            paint.textSize = 34f
            paint.color = Color.parseColor("#00E5FF")
            canvas.drawText("CENÁRIOS VIRTUAIS VR", 100f, 75f, paint)

            // Description
            paint.textSize = 24f
            paint.color = Color.parseColor("#94A3B8")
            canvas.drawText("Escolha o tema imersivo do seu ambiente espacial:", 50f, 130f, paint)

            // Environment cards
            val envs = com.lunarvr.environment.VREnvironmentType.values()
            for (i in envs.indices) {
                val env = envs[i]
                val col = i % 2
                val row = i / 2
                val bx = 50f + col * 480f
                val by = 170f + row * 160f
                val bw = 440f
                val bh = 135f

                val isCurrent = (env == environmentManager.currentEnvironment)

                paint.style = Paint.Style.FILL
                paint.color = if (isCurrent) Color.parseColor("#1E3A8A") else Color.parseColor("#152033")
                canvas.drawRoundRect(RectF(bx, by, bx + bw, by + bh), 20f, 20f, paint)

                paint.style = Paint.Style.STROKE
                paint.strokeWidth = if (isCurrent) 3.5f else 1.8f
                paint.color = if (isCurrent) Color.parseColor("#00E5FF") else Color.parseColor("#25344F")
                canvas.drawRoundRect(RectF(bx, by, bx + bw, by + bh), 20f, 20f, paint)

                // Title
                paint.style = Paint.Style.FILL
                paint.textSize = 28f
                paint.color = Color.parseColor("#F8FAFC")
                val activeTag = if (isCurrent) " (Ativo)" else ""
                canvas.drawText("${env.displayName}$activeTag", bx + 24f, by + 48f, paint)

                // Subtitle
                paint.textSize = 20f
                paint.color = Color.parseColor("#94A3B8")
                canvas.drawText(env.description, bx + 24f, by + 90f, paint)
            }

            // Drag handle at bottom
            ModernIcons.drawDragHandle(
                canvas, paint, 512f, 644f, 260f, 20f,
                envGrabHandle.isHovered, envGrabHandle.isGrabbed, envGrabHandle.hoverProgress
            )
        }
    }

    private fun updateSettingsPanel() {
        settingsVRPanel?.drawCustom { canvas, paint ->
            canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)

            // Background panel
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#F50D1322")
            canvas.drawRoundRect(RectF(10f, 10f, 1014f, 720f), 35f, 35f, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            paint.color = Color.parseColor("#38BDF8")
            canvas.drawRoundRect(RectF(10f, 10f, 1014f, 720f), 35f, 35f, paint)

            // Header Icon and Title
            ModernIcons.drawSettingsIcon(canvas, paint, 60f, 65f, 34f, Color.parseColor("#00E5FF"))

            paint.style = Paint.Style.FILL
            paint.textSize = 34f
            paint.color = Color.parseColor("#00E5FF")
            canvas.drawText("AJUSTES DO SISTEMA", 100f, 75f, paint)

            // System info
            paint.textSize = 24f
            paint.color = Color.parseColor("#94A3B8")
            val report = vrSession.hardwareReport
            val infoLines = settingsPanel.getSystemInfoText(report).lines()
            var textY = 130f
            for (line in infoLines) {
                canvas.drawText(line, 50f, textY, paint)
                textY += 36f
            }

            // Buttons grid
            for (i in settingsPanel.buttons.indices) {
                val btn = settingsPanel.buttons[i]
                val col = i % 2
                val row = i / 2
                val bx = 50f + col * 480f
                val by = 420f + row * 80f
                val bw = 440f
                val bh = 65f

                paint.style = Paint.Style.FILL
                paint.color = if (btn.isHovered) Color.parseColor("#223354") else Color.parseColor("#152033")
                canvas.drawRoundRect(RectF(bx, by, bx + bw, by + bh), 16f, 16f, paint)

                paint.style = Paint.Style.STROKE
                paint.strokeWidth = if (btn.isHovered) 3.5f else 1.8f
                paint.color = if (btn.isHovered) Color.parseColor("#00E5FF") else Color.parseColor("#25344F")
                canvas.drawRoundRect(RectF(bx, by, bx + bw, by + bh), 16f, 16f, paint)

                if (btn.isHovered && btn.hoverProgress > 0f) {
                    paint.color = Color.parseColor("#00E5FF")
                    paint.strokeWidth = 6f
                    canvas.drawLine(bx + 10f, by + bh - 6f, bx + 10f + (bw - 20f) * btn.hoverProgress, by + bh - 6f, paint)
                }

                paint.style = Paint.Style.FILL
                paint.textSize = 25f
                paint.color = Color.parseColor("#F8FAFC")
                canvas.drawText(btn.label, bx + 25f, by + 44f, paint)
            }

            // Drag handle at the bottom of settings panel
            ModernIcons.drawDragHandle(
                canvas, paint, 512f, 744f, 260f, 20f,
                settingsGrabHandle.isHovered, settingsGrabHandle.isGrabbed, settingsGrabHandle.hoverProgress
            )
        }
    }

    private fun updateKeyboardPanel() {
        keyboardVRPanel?.drawCustom { canvas, paint ->
            canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)

            // Meta Quest OS virtual keyboard glass container
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#F00E1424")
            canvas.drawRoundRect(RectF(10f, 10f, 1014f, 502f), 32f, 32f, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.5f
            paint.color = Color.parseColor("#38BDF8")
            canvas.drawRoundRect(RectF(10f, 10f, 1014f, 502f), 32f, 32f, paint)

            // Live Text Input Bar
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#162035")
            canvas.drawRoundRect(RectF(30f, 25f, 994f, 85f), 16f, 16f, paint)

            paint.textSize = 30f
            paint.color = Color.parseColor("#00E5FF")
            canvas.drawText("Digitar: ${textInputManager.getCurrentText()}_", 50f, 65f, paint)

            // Close button top-right
            paint.color = Color.parseColor("#94A3B8")
            paint.textSize = 24f
            canvas.drawText("✕ Fechar", 880f, 65f, paint)

            // Draw virtual keys (2 seconds dwell click)
            val rows = vrKeyboard.getCurrentRows()
            var startKeyY = 105f
            var keyIdx = 0
            for (r in rows.indices) {
                val row = rows[r]
                val kw = 940f / row.size
                val kh = 68f
                for (c in row.indices) {
                    val keyChar = row[c]
                    val kx = 42f + c * kw
                    val ky = startKeyY

                    val btn = keyboardButtons.getOrNull(keyIdx)
                    val isHovered = btn?.isHovered == true

                    // Key background
                    paint.style = Paint.Style.FILL
                    paint.color = when {
                        keyChar in listOf("ENTER", "SPACE", "SHIFT", "shift", "?123", "ABC", "DEL") -> {
                            if (isHovered) Color.parseColor("#1E3A8A") else Color.parseColor("#172554")
                        }
                        isHovered -> Color.parseColor("#1E2D4A")
                        else -> Color.parseColor("#151E32")
                    }
                    canvas.drawRoundRect(RectF(kx + 4f, ky + 4f, kx + kw - 4f, ky + kh - 4f), 12f, 12f, paint)

                    // Key border
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = if (isHovered) 3.5f else 1.5f
                    paint.color = if (isHovered) Color.parseColor("#00E5FF") else Color.parseColor("#25344F")
                    canvas.drawRoundRect(RectF(kx + 4f, ky + 4f, kx + kw - 4f, ky + kh - 4f), 12f, 12f, paint)

                    // 2-second Dwell progress bar inside the key
                    if (isHovered && btn != null && btn.hoverProgress > 0f) {
                        paint.color = Color.parseColor("#00E5FF")
                        paint.strokeWidth = 6f
                        val progW = (kw - 16f) * btn.hoverProgress
                        canvas.drawLine(kx + 8f, ky + kh - 6f, kx + 8f + progW, ky + kh - 6f, paint)
                    }

                    // Key Text
                    paint.style = Paint.Style.FILL
                    paint.textSize = if (keyChar.length > 2) 22f else 28f
                    paint.color = if (isHovered) Color.parseColor("#00E5FF") else Color.parseColor("#F8FAFC")
                    val tw = paint.measureText(keyChar)
                    canvas.drawText(keyChar, kx + (kw - tw) / 2f, ky + kh / 2f + 9f, paint)

                    keyIdx++
                }
                startKeyY += 76f
            }

            // Bottom grab handle for keyboard
            ModernIcons.drawDragHandle(
                canvas, paint, 512f, 490f, 260f, 18f,
                keyboardGrabHandle.isHovered, keyboardGrabHandle.isGrabbed, keyboardGrabHandle.hoverProgress
            )
        }
    }

    private fun initPanels() {
        // Lunar Bar: right in front, comfortable natural eye rest
        barPanel = VRPanel("lunar_bar", 0.0f, -0.28f, -1.35f, 1.15f, 0.28f, 1024, 256).also { it.initGL() }

        // Browser & URL Panels
        urlPanel = VRPanel("url_panel", 0.0f, 0.46f, -1.35f, 1.15f, 0.14f, 1024, 128).also { it.initGL() }
        browserPanel = VRPanel("browser_panel", 0.0f, 0.08f, -1.35f, 1.15f, 0.65f, 1024, 768).also { it.initGL() }

        // Settings Panel
        settingsVRPanel = VRPanel("settings_panel", 0.0f, 0.10f, -1.30f, 1.10f, 0.82f, 1024, 768).also { it.initGL() }

        // Environment Panel
        envVRPanel = VRPanel("env_panel", 0.0f, 0.10f, -1.30f, 1.10f, 0.72f, 1024, 640).also { it.initGL() }

        // Virtual 3D Keyboard
        keyboardVRPanel = VRPanel("keyboard_panel", 0.0f, -0.05f, -1.25f, 1.10f, 0.52f, 1024, 512).also { it.initGL() }
    }

    private fun initStarfield() {
        val random = Random(42)
        val coords = FloatArray(starCount * 3)
        for (i in 0 until starCount) {
            val theta = random.nextFloat() * 2f * Math.PI.toFloat()
            val phi = Math.acos((2f * random.nextFloat() - 1f).toDouble()).toFloat()
            val radius = 15f + random.nextFloat() * 20f

            coords[i * 3] = (radius * Math.sin(phi.toDouble()) * Math.cos(theta.toDouble())).toFloat()
            coords[i * 3 + 1] = (radius * Math.sin(phi.toDouble()) * Math.sin(theta.toDouble())).toFloat()
            coords[i * 3 + 2] = (radius * Math.cos(phi.toDouble())).toFloat()
        }

        val buf = ByteBuffer.allocateDirect(coords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        buf.put(coords).position(0)
        starBuffer = buf

        val vs = """
            uniform mat4 uMVPMatrix;
            attribute vec4 vPosition;
            void main() {
                gl_Position = uMVPMatrix * vPosition;
                gl_PointSize = 3.5;
            }
        """.trimIndent()

        val fs = """
            precision mediump float;
            uniform vec4 vColor;
            void main() {
                gl_FragColor = vColor;
            }
        """.trimIndent()

        starProgram = createProgram(vs, fs)
    }

    private fun initShaders() {
        val vs = """
            uniform mat4 uMVPMatrix;
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = uMVPMatrix * aPosition;
                vTexCoord = aTexCoord;
            }
        """.trimIndent()

        val fs = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D sTexture;
            void main() {
                gl_FragColor = texture2D(sTexture, vTexCoord);
            }
        """.trimIndent()

        panelProgram = createProgram(vs, fs)
        aPosHandle = GLES20.glGetAttribLocation(panelProgram, "aPosition")
        aTexHandle = GLES20.glGetAttribLocation(panelProgram, "aTexCoord")
        uMvpHandle = GLES20.glGetUniformLocation(panelProgram, "uMVPMatrix")
    }

    private fun createProgram(vsCode: String, fsCode: String): Int {
        val vs = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER).also {
            GLES20.glShaderSource(it, vsCode)
            GLES20.glCompileShader(it)
        }
        val fs = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER).also {
            GLES20.glShaderSource(it, fsCode)
            GLES20.glCompileShader(it)
        }
        return GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vs)
            GLES20.glAttachShader(it, fs)
            GLES20.glLinkProgram(it)
        }
    }
}
