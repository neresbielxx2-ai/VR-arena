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
import com.lunarvr.handtracking.InteractionManager
import com.lunarvr.handtracking.Ray3D
import com.lunarvr.keyboard.TextInputManager
import com.lunarvr.keyboard.VRKey
import com.lunarvr.keyboard.VRKeyboard
import com.lunarvr.keyboard.VRKeyboardListener
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

    // Subsystems
    val lunarBar = LunarBar(
        onNavigate = { dest -> handleNavigation(dest) },
        onRecenter = { vrSession.recenterManager.triggerRecenter() }
    )

    val settingsPanel = SettingsPanel(vrSession) {
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
    private var keyboardVRPanel: VRPanel? = null

    // App Window Drag Handles: independent move handle under EACH open app window!
    private var browserGrabHandle = GrabHandle("grab_browser", 0.0f, -0.32f, -1.35f, 0.40f, 0.06f)
    private var settingsGrabHandle = GrabHandle("grab_settings", 0.0f, -0.38f, -1.30f, 0.40f, 0.06f)

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
            GLES20.glClearColor(0.027f, 0.039f, 0.070f, 1.0f) // Lunar space black (#070A12)
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)

            initShaders()
            initStarfield()
            initReticle()
            initPanels()

            browserView = BrowserView(context, browserController)

            // Setup Grab Handlers to dynamically reposition panels
            lunarBar.grabHandle.onDragUpdate = { newX, newY ->
                lunarBar.updatePosition(newX, newY)
                barPanel?.let {
                    it.x = newX
                    it.y = newY
                }
            }

            browserGrabHandle.onDragUpdate = { newX, newY ->
                browserGrabHandle.x = newX
                browserGrabHandle.y = newY
                // Reposition both URL bar and Browser view together
                urlPanel?.let {
                    it.x = newX
                    it.y = newY + 0.72f
                }
                browserPanel?.let {
                    it.x = newX
                    it.y = newY + 0.35f
                }
                urlBar.setupButtons(newX, newY + 0.72f)
            }

            settingsGrabHandle.onDragUpdate = { newX, newY ->
                settingsGrabHandle.x = newX
                settingsGrabHandle.y = newY
                settingsVRPanel?.let {
                    it.x = newX
                    it.y = newY + 0.44f
                }
                settingsPanel.setupButtons(newX, newY + 0.44f)
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
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

            // Health monitor & auto error recovery
            val healthError = vrSession.headTracking.checkSensorHealth()
            if (healthError != null) {
                showNotification(healthError)
            }

            // Read sensor orientation (View Matrix)
            vrSession.headTracking.getHeadMatrix(headViewMatrix)

            // Gaze Ray in World Space:
            // The camera position is at world (0,0,0).
            // Camera forward vector in world coordinates is row 2 of View Matrix negated:
            val fwdX = -headViewMatrix[2]
            val fwdY = -headViewMatrix[6]
            val fwdZ = -headViewMatrix[10]
            val gazeRay = Ray3D(0f, 0f, 0f, fwdX, fwdY, fwdZ)

            interactionManager.update(gazeRay)

            // Calculate look target on panel plane (Z = -1.35f)
            val lookPlaneZ = -1.35f
            if (fwdZ != 0f) {
                val t = lookPlaneZ / fwdZ
                val gazePlaneX = t * fwdX
                val gazePlaneY = t * fwdY

                // Update any active grab handles:
                // If looked at for 2 seconds, grab handle tracks gaze, then automatically unlocks
                lunarBar.grabHandle.updateGrab(gazePlaneX, gazePlaneY + 0.15f)
                browserGrabHandle.updateGrab(gazePlaneX, gazePlaneY)
                settingsGrabHandle.updateGrab(gazePlaneX, gazePlaneY)
            }

            // Update dynamic UI textures
            updateBarPanel()
            if (currentDestination == LunarNavDestination.BROWSER) {
                updateBrowserPanels()
            } else if (currentDestination == LunarNavDestination.SETTINGS) {
                updateSettingsPanel()
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

        // Floating Lunar Bar (always in front)
        barPanel?.bindAndRender(panelProgram, vpMatrix, aPosHandle, aTexHandle, uMvpHandle)

        // Only ONE app window open at a time to prevent any overlap!
        if (currentDestination == LunarNavDestination.BROWSER) {
            urlPanel?.bindAndRender(panelProgram, vpMatrix, aPosHandle, aTexHandle, uMvpHandle)
            browserPanel?.bindAndRender(panelProgram, vpMatrix, aPosHandle, aTexHandle, uMvpHandle)
        } else if (currentDestination == LunarNavDestination.SETTINGS) {
            settingsVRPanel?.bindAndRender(panelProgram, vpMatrix, aPosHandle, aTexHandle, uMvpHandle)
        }

        // Virtual Keyboard
        if (vrKeyboard.isVisible) {
            keyboardVRPanel?.bindAndRender(panelProgram, vpMatrix, aPosHandle, aTexHandle, uMvpHandle)
        }

        // Draw Gaze Pointer in camera view space (always locked dead-center to eyes)
        drawReticle(projMatrix)
    }

    private fun drawReticle(projMatrix: FloatArray) {
        val rBuf = reticleBuffer ?: return
        if (reticleProgram == 0) return

        GLES20.glUseProgram(reticleProgram)
        val mvp = GLES20.glGetUniformLocation(reticleProgram, "uMVPMatrix")
        val color = GLES20.glGetUniformLocation(reticleProgram, "vColor")
        val pos = GLES20.glGetAttribLocation(reticleProgram, "vPosition")

        // Draw center reticle in camera view at Z = -1.0
        val model = FloatArray(16)
        Matrix.setIdentityM(model, 0)
        Matrix.translateM(model, 0, 0f, 0f, -1.0f)

        val mvpMatrix = FloatArray(16)
        Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, model, 0)

        GLES20.glUniformMatrix4fv(mvp, 1, false, mvpMatrix, 0)

        // Reticle glows high-visibility lunar cyan
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

        GLES20.glUniformMatrix4fv(mvp, 1, false, vpMatrix, 0)
        GLES20.glUniform4f(color, 0.8f, 0.9f, 1.0f, 0.75f)

        sBuf.position(0)
        GLES20.glEnableVertexAttribArray(pos)
        GLES20.glVertexAttribPointer(pos, 3, GLES20.GL_FLOAT, false, 0, sBuf)

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, starCount)
        GLES20.glDisableVertexAttribArray(pos)
    }

    private fun handleNavigation(dest: LunarNavDestination) {
        if (currentDestination == dest && dest != LunarNavDestination.HOME) {
            // Toggle off if already opened
            currentDestination = LunarNavDestination.HOME
        } else {
            currentDestination = dest
        }

        settingsPanel.isVisible = (currentDestination == LunarNavDestination.SETTINGS)
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
        setupKeyboardButtons()
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

        // Register Keyboard buttons
        if (vrKeyboard.isVisible) {
            for (btn in keyboardButtons) {
                interactionManager.register(btn)
            }
        }
    }

    private fun setupKeyboardButtons() {
        keyboardButtons.clear()
        val rows = vrKeyboard.getCurrentRows()
        val startY = -0.05f
        val zPos = -1.25f
        val btnH = 0.07f

        for (r in rows.indices) {
            val row = rows[r]
            val btnW = 0.90f / row.size
            val startX = -0.45f + (btnW / 2.0f)
            val y = startY - (r * 0.08f)

            for (c in row.indices) {
                val key = row[c]
                val x = startX + (c * btnW)
                keyboardButtons.add(
                    VRKey("key_${key}_$r", key, x, y, zPos, btnW * 0.92f, btnH) {
                        vrKeyboard.handleKeyPress(key)
                        if (key in listOf("SHIFT", "shift", "123", "ABC")) {
                            setupKeyboardButtons()
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

            // Sleek translucent glass capsule
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#EE0B0F19")
            canvas.drawRoundRect(RectF(14f, 14f, 1010f, 206f), 38f, 38f, paint)

            // Crisp Lunar Cyan Border
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            paint.color = Color.parseColor("#22D3EE")
            canvas.drawRoundRect(RectF(14f, 14f, 1010f, 206f), 38f, 38f, paint)

            // Header status line
            paint.style = Paint.Style.FILL
            paint.textSize = 24f
            paint.color = Color.parseColor("#94A3B8")
            val statusTxt = if (notificationMessage != null && System.currentTimeMillis() < notificationEndTime) {
                "⚡ ${notificationMessage}"
            } else {
                "LUNAR VR  |  ${lunarBar.vrStatus}"
            }
            canvas.drawText(statusTxt, 36f, 46f, paint)
            canvas.drawText("${lunarBar.currentTimeString}   🔋 ${lunarBar.batteryPercentage}%", 820f, 46f, paint)

            // Buttons: 4 actions with custom vector icons
            val btnW = 226f
            val btnH = 120f
            val by = 68f

            for (i in lunarBar.buttons.indices) {
                val btn = lunarBar.buttons[i]
                val bx = 28f + i * 242f

                // Button back fill
                paint.style = Paint.Style.FILL
                if (btn.isHovered) {
                    paint.color = Color.parseColor("#1E2D4A")
                } else {
                    paint.color = Color.parseColor("#121929")
                }
                canvas.drawRoundRect(RectF(bx, by, bx + btnW, by + btnH), 20f, 20f, paint)

                // Button Border
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = if (btn.isHovered) 4f else 2f
                paint.color = if (btn.isHovered) Color.parseColor("#00E5FF") else Color.parseColor("#1E2E48")
                canvas.drawRoundRect(RectF(bx, by, bx + btnW, by + btnH), 20f, 20f, paint)

                // Dwell Progress bar on hover
                if (btn.isHovered && btn.hoverProgress > 0f) {
                    paint.color = Color.parseColor("#00E5FF")
                    paint.strokeWidth = 8f
                    val progressW = (btnW - 24f) * btn.hoverProgress
                    canvas.drawLine(bx + 12f, by + btnH - 8f, bx + 12f + progressW, by + btnH - 8f, paint)
                }

                // Custom vector icons (Clean, un-generic, no emojis)
                val iconCx = bx + btnW / 2f
                val iconCy = by + 45f
                val iconColor = if (btn.isHovered) Color.parseColor("#00E5FF") else Color.parseColor("#E2E8F0")

                when (i) {
                    0 -> ModernIcons.drawHomeIcon(canvas, paint, iconCx, iconCy, 36f, iconColor)
                    1 -> ModernIcons.drawGlobeIcon(canvas, paint, iconCx, iconCy, 36f, iconColor)
                    2 -> ModernIcons.drawRecenterIcon(canvas, paint, iconCx, iconCy, 36f, iconColor)
                    3 -> ModernIcons.drawSettingsIcon(canvas, paint, iconCx, iconCy, 36f, iconColor)
                }

                // Button Label
                paint.style = Paint.Style.FILL
                paint.textSize = 24f
                paint.color = Color.parseColor("#F8FAFC")
                val textW = paint.measureText(btn.label)
                canvas.drawText(btn.label, bx + (btnW - textW) / 2f, by + 100f, paint)
            }

            // Bottom Drag Handle Bar: Look at it for 2 seconds to grab and reposition!
            val grab = lunarBar.grabHandle
            ModernIcons.drawDragHandle(canvas, paint, 512f, 230f, 280f, 22f, grab.isGrabbed)
        }
    }

    private fun updateBrowserPanels() {
        // Draw URL bar
        urlPanel?.drawCustom { canvas, paint ->
            canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)

            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#F00D121F")
            canvas.drawRoundRect(RectF(10f, 10f, 1014f, 118f), 20f, 20f, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            paint.color = Color.parseColor("#2A3B5C")
            canvas.drawRoundRect(RectF(10f, 10f, 1014f, 118f), 20f, 20f, paint)

            paint.style = Paint.Style.FILL
            paint.textSize = 34f
            paint.color = Color.parseColor("#E2E8F0")

            // Modern icons text
            canvas.drawText("◀   ▶   ↻   ✦", 40f, 75f, paint)

            // Address bar field
            paint.color = Color.parseColor("#1A2238")
            canvas.drawRoundRect(RectF(320f, 25f, 990f, 100f), 15f, 15f, paint)

            paint.color = Color.parseColor("#00E5FF")
            paint.textSize = 30f
            val displayTxt = if (urlBar.displayUrl.length > 38) urlBar.displayUrl.take(38) + "..." else urlBar.displayUrl
            canvas.drawText(displayTxt, 350f, 72f, paint)
        }

        // Draw WebView content
        val bmp = browserView?.captureBitmap()
        if (bmp != null) {
            browserPanel?.copyBitmap(bmp)
        }
    }

    private fun updateSettingsPanel() {
        settingsVRPanel?.drawCustom { canvas, paint ->
            canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)

            // Background panel
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#F50A0E1A")
            canvas.drawRoundRect(RectF(10f, 10f, 1014f, 720f), 35f, 35f, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            paint.color = Color.parseColor("#3A86FF")
            canvas.drawRoundRect(RectF(10f, 10f, 1014f, 720f), 35f, 35f, paint)

            // Title with modern vector icon
            ModernIcons.drawSettingsIcon(canvas, paint, 60f, 65f, 34f, Color.parseColor("#00E5FF"))

            paint.style = Paint.Style.FILL
            paint.textSize = 34f
            paint.color = Color.parseColor("#00E5FF")
            canvas.drawText("AJUSTES LUNAR VR", 100f, 75f, paint)

            // System info
            paint.textSize = 25f
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
                paint.color = if (btn.isHovered) Color.parseColor("#2A3B5C") else Color.parseColor("#16233B")
                canvas.drawRoundRect(RectF(bx, by, bx + bw, by + bh), 16f, 16f, paint)

                paint.style = Paint.Style.STROKE
                paint.strokeWidth = if (btn.isHovered) 4f else 2f
                paint.color = if (btn.isHovered) Color.parseColor("#00E5FF") else Color.parseColor("#2A3B5C")
                canvas.drawRoundRect(RectF(bx, by, bx + bw, by + bh), 16f, 16f, paint)

                if (btn.isHovered && btn.hoverProgress > 0f) {
                    paint.color = Color.parseColor("#00E5FF")
                    paint.strokeWidth = 6f
                    canvas.drawLine(bx + 10f, by + bh - 6f, bx + 10f + (bw - 20f) * btn.hoverProgress, by + bh - 6f, paint)
                }

                paint.style = Paint.Style.FILL
                paint.textSize = 26f
                paint.color = Color.parseColor("#F8FAFC")
                canvas.drawText(btn.label, bx + 25f, by + 44f, paint)
            }

            // Drag handle at the bottom of settings panel
            ModernIcons.drawDragHandle(canvas, paint, 512f, 744f, 260f, 20f, settingsGrabHandle.isGrabbed)
        }
    }

    private fun updateKeyboardPanel() {
        keyboardVRPanel?.drawCustom { canvas, paint ->
            canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)

            // Background
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#E60D121F")
            canvas.drawRoundRect(RectF(10f, 10f, 1014f, 502f), 30f, 30f, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            paint.color = Color.parseColor("#4DEEEA")
            canvas.drawRoundRect(RectF(10f, 10f, 1014f, 502f), 30f, 30f, paint)

            // Current Text Buffer Preview
            paint.style = Paint.Style.FILL
            paint.textSize = 34f
            paint.color = Color.parseColor("#00E5FF")
            canvas.drawText("Texto: ${textInputManager.getCurrentText()}_", 50f, 60f, paint)

            // Draw virtual keys
            val rows = vrKeyboard.getCurrentRows()
            var startKeyY = 100f
            for (r in rows.indices) {
                val row = rows[r]
                val kw = 920f / row.size
                val kh = 80f
                for (c in row.indices) {
                    val keyChar = row[c]
                    val kx = 50f + c * kw
                    val ky = startKeyY

                    paint.style = Paint.Style.FILL
                    paint.color = Color.parseColor("#1A2238")
                    canvas.drawRoundRect(RectF(kx + 4f, ky + 4f, kx + kw - 4f, ky + kh - 4f), 12f, 12f, paint)

                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 2f
                    paint.color = Color.parseColor("#2A3B5C")
                    canvas.drawRoundRect(RectF(kx + 4f, ky + 4f, kx + kw - 4f, ky + kh - 4f), 12f, 12f, paint)

                    paint.style = Paint.Style.FILL
                    paint.textSize = 30f
                    paint.color = Color.parseColor("#F8FAFC")
                    canvas.drawText(keyChar, kx + kw / 2f - 12f, ky + kh / 2f + 10f, paint)
                }
                startKeyY += 95f
            }
        }
    }

    private fun initPanels() {
        // Lunar Bar: right in front, comfortable natural eye rest (y = -0.26f, z = -1.35f)
        barPanel = VRPanel("lunar_bar", 0.0f, -0.26f, -1.35f, 1.15f, 0.28f, 1024, 256).also { it.initGL() }

        // Browser & URL Panels: centered right in front of user
        urlPanel = VRPanel("url_panel", 0.0f, 0.45f, -1.35f, 1.15f, 0.14f, 1024, 128).also { it.initGL() }
        browserPanel = VRPanel("browser_panel", 0.0f, 0.08f, -1.35f, 1.15f, 0.65f, 1024, 768).also { it.initGL() }

        // Settings Panel
        settingsVRPanel = VRPanel("settings_panel", 0.0f, 0.10f, -1.30f, 1.10f, 0.82f, 1024, 768).also { it.initGL() }

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
