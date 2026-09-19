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
import com.lunarvr.browser.BrowserTouchElement
import com.lunarvr.browser.BrowserView
import com.lunarvr.browser.URLBar
import com.lunarvr.environment.EnvironmentBackdrop
import com.lunarvr.environment.EnvironmentManager
import com.lunarvr.handtracking.InteractionManager
import com.lunarvr.handtracking.Ray3D
import com.lunarvr.keyboard.TextInputManager
import com.lunarvr.keyboard.VRKey
import com.lunarvr.keyboard.VRKeyboard
import com.lunarvr.keyboard.VRKeyboardListener
import com.lunarvr.ui.BarStyle
import com.lunarvr.ui.BarColorTheme
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

    // Environment System with Real 3D Scenery Backdrops
    val environmentManager = EnvironmentManager()
    private val backdrops = mutableMapOf<com.lunarvr.environment.VREnvironmentType, EnvironmentBackdrop>()

    // Subsystems
    val lunarBar = LunarBar(
        onNavigate = { dest -> handleNavigation(dest) },
        onRecenter = { vrSession.recenterManager.triggerRecenter() }
    )

    lateinit var settingsPanel: SettingsPanel

    init {
        settingsPanel = SettingsPanel(vrSession, lunarBar) {
            interactionManager.userDwellTimeMs = settingsPanel.getDwellTimeMs()
            refreshInteractiveElements()
        }
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
        onHomeClick = { browserView?.loadUrl("https://html.duckduckgo.com/html/") },
        onResizeClick = { cycleBrowserScale() }
    )

    // Interactive Touch surface for clicking links/buttons directly inside the web browser with 2s gaze!
    private var browserTouchElement = BrowserTouchElement(
        "browser_touch_surface", 0f, 0.08f, -1.45f, 1.60f, 1.00f
    ) { normX, normY ->
        browserView?.dispatchClick(normX, normY)
        showNotification("Clique no Navegador")
    }

    // Dynamic scale levels for apps: 1.0x (Standard), 1.25x (Large), 1.5x (Cinema)
    // Base dimensions: 1.60m width x 1.00m height (comfortable 16:10 spacious desktop aspect ratio)
    private val browserScales = listOf(1.0f, 1.25f, 1.5f)
    private var currentBrowserScaleIdx = 0

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
    private var browserGrabHandle = GrabHandle("grab_browser", 0.0f, -0.36f, -1.35f, 0.42f, 0.06f)
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

    // Navigation State
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
            initBackdrops()

            browserView = BrowserView(context, browserController)
            interactionManager.userDwellTimeMs = settingsPanel.getDwellTimeMs()

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

                applyBrowserTransform(bx, height, bz, -yaw)
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

    private fun cycleBrowserScale() {
        currentBrowserScaleIdx = (currentBrowserScaleIdx + 1) % browserScales.size
        val scale = browserScales[currentBrowserScaleIdx]
        urlBar.scaleName = "${scale}x"

        val baseW = 1.60f * scale
        val baseH = 1.00f * scale

        browserPanel?.setDimensions(baseW, baseH)
        urlPanel?.setDimensions(baseW, 0.15f * scale)

        browserTouchElement.width = baseW
        browserTouchElement.height = baseH

        val rad = Math.toRadians(browserYawDeg.toDouble())
        val dist = 1.45f
        val bx = (dist * Math.sin(rad)).toFloat()
        val bz = (-dist * Math.cos(rad)).toFloat()

        applyBrowserTransform(bx, browserHeightY, bz, -browserYawDeg)
        showNotification("Escala Navegador: ${scale}x")
        refreshInteractiveElements()
    }

    private fun applyBrowserTransform(bx: Float, by: Float, bz: Float, rotY: Float) {
        val scale = browserScales[currentBrowserScaleIdx]
        val panelH = 1.00f * scale
        val urlH = 0.15f * scale

        browserPanel?.let {
            it.x = bx
            it.y = by
            it.z = bz
            it.rotationYDeg = rotY
        }

        urlPanel?.let {
            it.x = bx
            it.y = by + (panelH / 2f) + (urlH / 2f) + 0.02f
            it.z = bz
            it.rotationYDeg = rotY
        }

        browserTouchElement.x = bx
        browserTouchElement.y = by
        browserTouchElement.z = bz

        browserGrabHandle.x = bx
        browserGrabHandle.y = by - (panelH / 2f) - 0.06f
        browserGrabHandle.z = bz

        urlBar.setupButtons(bx, by + (panelH / 2f) + (urlH / 2f) + 0.02f)
    }

    private fun initBackdrops() {
        for (env in com.lunarvr.environment.VREnvironmentType.values()) {
            val bd = EnvironmentBackdrop(env)
            bd.panel.initGL()
            bd.renderBackdrop()
            backdrops[env] = bd
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
            val fwdX = -headViewMatrix[2]
            val fwdY = -headViewMatrix[6]
            val fwdZ = -headViewMatrix[10]
            val gazeRay = Ray3D(0f, 0f, 0f, fwdX, fwdY, fwdZ)

            // Update web touch coordinates dynamically on raycast
            if (currentDestination == LunarNavDestination.BROWSER) {
                browserTouchElement.updateHitCoordinate(gazeRay)
            }

            interactionManager.update(gazeRay)

            // Spherical coordinate tracking (Yaw angle in degrees and vertical height Y)
            val gazeYawDeg = Math.toDegrees(Math.atan2(fwdX.toDouble(), -fwdZ.toDouble())).toFloat()
            val gazeHeightY = fwdY * 1.35f

            // Update any active grab handles:
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

        // Draw Scenic Environment 3D Backdrop (Moon with Earth, Cyber Synthwave, Zen Forest, Skyline)
        val curBackdrop = backdrops[environmentManager.currentEnvironment]
        curBackdrop?.panel?.bindAndRender(panelProgram, vpMatrix, aPosHandle, aTexHandle, uMvpHandle)

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

        // Always render reticle on top without depth occluding
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
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

        val progress = interactionManager.currentProgress
        val isHovering = progress > 0f || lunarBar.grabHandle.isGrabbed
        
        // Dynamic reticle color transitioning from sleek Lunar violet to emerald neon when dwelling
        if (isHovering) {
            // Smoothly shift to intense emerald green/cyan: #10B981 -> #00FFCC
            val r = 0.06f + (1f - progress) * 0.40f
            val g = 0.85f + progress * 0.15f
            val b = 0.55f + progress * 0.40f
            GLES20.glUniform4f(color, r, g, b, 1.0f)
        } else {
            // Sleek holographic pearl lavender: #C084FC
            GLES20.glUniform4f(color, 0.75f, 0.52f, 0.99f, 0.85f)
        }

        rBuf.position(0)
        GLES20.glEnableVertexAttribArray(pos)
        GLES20.glVertexAttribPointer(pos, 3, GLES20.GL_FLOAT, false, 0, rBuf)

        GLES20.glLineWidth(3.8f)
        GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 0, 32)
        // Center focal pip
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 32, 32)

        GLES20.glDisableVertexAttribArray(pos)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }

    private fun initReticle() {
        val segments = 32
        val radius = 0.016f
        val innerRadius = 0.005f
        // Outer ring + inner dot
        val coords = FloatArray((segments + segments + 2) * 3)
        var idx = 0
        // Outer circle loop
        for (i in 0 until segments) {
            val angle = 2.0 * Math.PI * i / segments
            coords[idx++] = (radius * Math.cos(angle)).toFloat()
            coords[idx++] = (radius * Math.sin(angle)).toFloat()
            coords[idx++] = 0f
        }
        // Inner center dot
        for (i in 0 until segments) {
            val angle = 2.0 * Math.PI * i / segments
            coords[idx++] = (innerRadius * Math.cos(angle)).toFloat()
            coords[idx++] = (innerRadius * Math.sin(angle)).toFloat()
            coords[idx++] = 0f
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
                urlBar.setupButtons(urlPanel?.x ?: 0f, urlPanel?.y ?: 0.58f)
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

        // Register Browser buttons, web click touch surface, and drag handle
        if (currentDestination == LunarNavDestination.BROWSER) {
            for (btn in urlBar.buttons) {
                interactionManager.register(btn)
            }
            interactionManager.register(browserTouchElement)
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

            val theme = lunarBar.currentColorTheme
            val style = lunarBar.currentStyle

            // Custom Shell according to chosen Style & Theme
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor(theme.bgHex)

            val pillCorner = when (style) {
                BarStyle.META_QUEST -> 50f // Ultra-smooth rounded Meta Quest 3S aesthetic
                BarStyle.LUNAR_COSMIC -> 32f // Angular cosmic aesthetic
                BarStyle.MINIMAL_CYBER -> 12f // Sharp cybernetic aesthetic
            }
            val shellRect = RectF(14f, 14f, 1010f, 206f)
            canvas.drawRoundRect(shellRect, pillCorner, pillCorner, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = if (style == BarStyle.META_QUEST) 2.0f else 2.8f
            paint.color = Color.parseColor(theme.borderHex)
            canvas.drawRoundRect(shellRect, pillCorner, pillCorner, paint)

            // Header status line: Title, Status, Battery Icon, Time
            paint.style = Paint.Style.FILL
            paint.textSize = 23f
            paint.color = Color.parseColor("#94A3B8")
            val statusTxt = if (notificationMessage != null && System.currentTimeMillis() < notificationEndTime) {
                "✨ ${notificationMessage}"
            } else {
                "LUNAR OS  |  ${lunarBar.vrStatus}  •  ${style.displayName}"
            }
            canvas.drawText(statusTxt, 36f, 46f, paint)
            canvas.drawText(lunarBar.currentTimeString, 910f, 46f, paint)

            // Clean vector battery indicator
            ModernIcons.drawBatteryIcon(canvas, paint, 790f, 40f, lunarBar.batteryPercentage)

            // Buttons: 5 actions (Início, Navegador, Cenários, Centralizar, Ajustes)
            val btnW = 186f
            val btnH = 120f
            val by = 68f

            // Sleek color accents per category (Emerald, Purple, Rose, Violet, Cyan)
            val accentColors = listOf(
                Pair("#10B981", "#064E3B"), // Home: Emerald Green
                Pair("#6366F1", "#312E81"), // Browser: Cosmic Indigo
                Pair("#EC4899", "#831843"), // Environments: Sunset Rose
                Pair("#8B5CF6", "#4C1D95"), // Recenter: Royal Violet
                Pair("#14B8A6", "#134E4A")  // Settings: Teal Cyan
            )

            for (i in lunarBar.buttons.indices) {
                val btn = lunarBar.buttons[i]
                val bx = 26f + i * 196f
                val (accentBorder, accentHoverBg) = accentColors[i % accentColors.size]

                // Card corner radius matches bar style
                val cardCorner = when (style) {
                    BarStyle.META_QUEST -> 30f // Meta Quest rounded card pill
                    BarStyle.LUNAR_COSMIC -> 20f
                    BarStyle.MINIMAL_CYBER -> 8f
                }

                // Rounded rect card
                paint.style = Paint.Style.FILL
                if (btn.isHovered) {
                    paint.color = Color.parseColor(accentHoverBg)
                } else {
                    paint.color = Color.parseColor("#171F33")
                }
                canvas.drawRoundRect(RectF(bx, by, bx + btnW, by + btnH), cardCorner, cardCorner, paint)

                paint.style = Paint.Style.STROKE
                paint.strokeWidth = if (btn.isHovered) 3.5f else 1.8f
                paint.color = if (btn.isHovered) Color.parseColor(accentBorder) else Color.parseColor("#2E3A52")
                canvas.drawRoundRect(RectF(bx, by, bx + btnW, by + btnH), cardCorner, cardCorner, paint)

                // Hover progress bar with distinct category accent
                if (btn.isHovered && btn.hoverProgress > 0f) {
                    paint.color = Color.parseColor(accentBorder)
                    paint.strokeWidth = 7f
                    val progressW = (btnW - 24f) * btn.hoverProgress
                    canvas.drawLine(bx + 12f, by + btnH - 8f, bx + 12f + progressW, by + btnH - 8f, paint)
                }

                // Clean Vector Icons
                val iconCx = bx + btnW / 2f
                val iconCy = by + 45f
                val iconColor = if (btn.isHovered) Color.parseColor(accentBorder) else Color.parseColor("#E2E8F0")

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
        // Draw URL bar with spacious modern design (1280x120 texture)
        urlPanel?.drawCustom { canvas, paint ->
            canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)

            // Outer shell with rich dark violet/slate gradient look
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#F5131728")
            canvas.drawRoundRect(RectF(10f, 10f, 1270f, 110f), 28f, 28f, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.5f
            paint.color = Color.parseColor("#475569")
            canvas.drawRoundRect(RectF(10f, 10f, 1270f, 110f), 28f, 28f, paint)

            // Navigation icons (spacious layout)
            paint.style = Paint.Style.FILL
            paint.textSize = 34f
            paint.color = Color.parseColor("#E2E8F0")
            canvas.drawText("◀    ▶    ↻    ✦", 42f, 72f, paint)

            // Interactive Search/URL field (expanded width: 340f to 1040f)
            paint.color = Color.parseColor("#1E293B")
            canvas.drawRoundRect(RectF(340f, 20f, 1040f, 100f), 20f, 20f, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            paint.color = Color.parseColor("#818CF8")
            canvas.drawRoundRect(RectF(340f, 20f, 1040f, 100f), 20f, 20f, paint)

            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#38BDF8")
            paint.textSize = 28f
            val displayTxt = if (urlBar.displayUrl.length > 46) urlBar.displayUrl.take(46) + "..." else urlBar.displayUrl
            canvas.drawText("🌐  $displayTxt", 365f, 68f, paint)

            // Resize pill button at the right (accented purple/cyan)
            paint.color = Color.parseColor("#312E81")
            canvas.drawRoundRect(RectF(1060f, 20f, 1250f, 100f), 20f, 20f, paint)

            paint.style = Paint.Style.STROKE
            paint.color = Color.parseColor("#A855F7")
            canvas.drawRoundRect(RectF(1060f, 20f, 1250f, 100f), 20f, 20f, paint)

            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#F8FAFC")
            paint.textSize = 26f
            canvas.drawText("⤢ ${urlBar.scaleName}", 1085f, 68f, paint)
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

                // Distinct color scheme for each environment card
                val cardTheme = when (env) {
                    com.lunarvr.environment.VREnvironmentType.LUNAR_EARTH_VIEW -> Pair("#1E3A8A", "#38BDF8")
                    com.lunarvr.environment.VREnvironmentType.CYBER_SYNTHWAVE -> Pair("#831843", "#F43F5E")
                    com.lunarvr.environment.VREnvironmentType.ZEN_FOREST -> Pair("#064E3B", "#10B981")
                    com.lunarvr.environment.VREnvironmentType.MINIMAL_LOFT -> Pair("#312E81", "#A855F7")
                }

                paint.style = Paint.Style.FILL
                paint.color = if (isCurrent) Color.parseColor(cardTheme.first) else Color.parseColor("#151D2A")
                canvas.drawRoundRect(RectF(bx, by, bx + bw, by + bh), 20f, 20f, paint)

                paint.style = Paint.Style.STROKE
                paint.strokeWidth = if (isCurrent) 3.5f else 1.8f
                paint.color = if (isCurrent) Color.parseColor(cardTheme.second) else Color.parseColor("#334155")
                canvas.drawRoundRect(RectF(bx, by, bx + bw, by + bh), 20f, 20f, paint)

                // Title
                paint.style = Paint.Style.FILL
                paint.textSize = 27f
                paint.color = if (isCurrent) Color.parseColor(cardTheme.second) else Color.parseColor("#F8FAFC")
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
            canvas.drawText("CONFIGURAÇÃO DO SISTEMA", 100f, 75f, paint)

            // System info with clean colored categories
            paint.textSize = 24f
            val report = vrSession.hardwareReport
            val infoLines = settingsPanel.getSystemInfoText(report).lines()
            var textY = 130f
            for (line in infoLines) {
                paint.color = when {
                    line.startsWith("Bateria") -> Color.parseColor("#34D399") // Emerald
                    line.startsWith("Modo") -> Color.parseColor("#38BDF8") // Sky
                    line.startsWith("FPS") -> Color.parseColor("#FBBF24") // Amber
                    line.startsWith("Tempo Dwell") -> Color.parseColor("#C084FC") // Violet
                    else -> Color.parseColor("#94A3B8")
                }
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
                paint.color = if (btn.isHovered) Color.parseColor("#2E1065") else Color.parseColor("#171F33")
                canvas.drawRoundRect(RectF(bx, by, bx + bw, by + bh), 16f, 16f, paint)

                paint.style = Paint.Style.STROKE
                paint.strokeWidth = if (btn.isHovered) 3.5f else 1.8f
                paint.color = if (btn.isHovered) Color.parseColor("#A855F7") else Color.parseColor("#334155")
                canvas.drawRoundRect(RectF(bx, by, bx + bw, by + bh), 16f, 16f, paint)

                if (btn.isHovered && btn.hoverProgress > 0f) {
                    paint.color = Color.parseColor("#C084FC")
                    paint.strokeWidth = 6f
                    canvas.drawLine(bx + 10f, by + bh - 6f, bx + 10f + (bw - 20f) * btn.hoverProgress, by + bh - 6f, paint)
                }

                paint.style = Paint.Style.FILL
                paint.textSize = 24f
                paint.color = Color.parseColor("#F8FAFC")
                canvas.drawText(btn.label, bx + 22f, by + 44f, paint)
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

            // Meta Quest OS virtual keyboard glass container with sleek Purple/Indigo accents
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#F5101424")
            canvas.drawRoundRect(RectF(10f, 10f, 1014f, 502f), 32f, 32f, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.5f
            paint.color = Color.parseColor("#6366F1")
            canvas.drawRoundRect(RectF(10f, 10f, 1014f, 502f), 32f, 32f, paint)

            // Live Text Input Bar
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#1B2236")
            canvas.drawRoundRect(RectF(30f, 25f, 994f, 85f), 16f, 16f, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.8f
            paint.color = Color.parseColor("#818CF8")
            canvas.drawRoundRect(RectF(30f, 25f, 994f, 85f), 16f, 16f, paint)

            paint.style = Paint.Style.FILL
            paint.textSize = 30f
            paint.color = Color.parseColor("#F472B6") // Soft Rose prompt
            canvas.drawText("Digitar: ", 50f, 65f, paint)

            paint.color = Color.parseColor("#F8FAFC")
            canvas.drawText("${textInputManager.getCurrentText()}_", 175f, 65f, paint)

            // Close button top-right (Coral/Red accent)
            paint.color = Color.parseColor("#FB7185")
            paint.textSize = 24f
            canvas.drawText("✕ Fechar", 880f, 65f, paint)

            // Draw virtual keys (Dwell click according to user setting)
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

                    val isSpecial = keyChar in listOf("ENTER", "SPACE", "SHIFT", "shift", "?123", "ABC", "DEL")

                    // Key background with richer palette
                    paint.style = Paint.Style.FILL
                    paint.color = when {
                        keyChar == "ENTER" -> if (isHovered) Color.parseColor("#059669") else Color.parseColor("#047857")
                        keyChar == "DEL" -> if (isHovered) Color.parseColor("#E11D48") else Color.parseColor("#BE123C")
                        isSpecial -> if (isHovered) Color.parseColor("#4338CA") else Color.parseColor("#312E81")
                        isHovered -> Color.parseColor("#334155")
                        else -> Color.parseColor("#1E293B")
                    }
                    canvas.drawRoundRect(RectF(kx + 4f, ky + 4f, kx + kw - 4f, ky + kh - 4f), 14f, 14f, paint)

                    // Key border
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = if (isHovered) 3.5f else 1.5f
                    paint.color = when {
                        keyChar == "ENTER" -> Color.parseColor("#10B981")
                        keyChar == "DEL" -> Color.parseColor("#F43F5E")
                        isHovered -> Color.parseColor("#A855F7")
                        else -> Color.parseColor("#334155")
                    }
                    canvas.drawRoundRect(RectF(kx + 4f, ky + 4f, kx + kw - 4f, ky + kh - 4f), 14f, 14f, paint)

                    // Dwell progress bar inside key (radiant violet/emerald)
                    if (isHovered && btn != null && btn.hoverProgress > 0f) {
                        paint.color = if (keyChar == "ENTER") Color.parseColor("#34D399") else Color.parseColor("#C084FC")
                        paint.strokeWidth = 6f
                        val progW = (kw - 16f) * btn.hoverProgress
                        canvas.drawLine(kx + 8f, ky + kh - 6f, kx + 8f + progW, ky + kh - 6f, paint)
                    }

                    // Key Text
                    paint.style = Paint.Style.FILL
                    paint.textSize = if (keyChar.length > 2) 22f else 28f
                    paint.color = if (isHovered) Color.parseColor("#FFFFFF") else Color.parseColor("#F1F5F9")
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
        // Lunar Bar
        barPanel = VRPanel("lunar_bar", 0.0f, -0.28f, -1.35f, 1.15f, 0.28f, 1024, 256).also { it.initGL() }

        // Browser & URL Panels (16:10 spacious wide layout with native 1280x800 resolution)
        urlPanel = VRPanel("url_panel", 0.0f, 0.58f, -1.45f, 1.60f, 0.15f, 1280, 120).also { it.initGL() }
        browserPanel = VRPanel("browser_panel", 0.0f, 0.08f, -1.45f, 1.60f, 1.00f, 1280, 800).also { it.initGL() }

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
