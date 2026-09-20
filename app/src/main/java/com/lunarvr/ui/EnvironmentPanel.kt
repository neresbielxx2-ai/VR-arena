package com.lunarvr.ui

import com.lunarvr.environment.CustomModelManager
import com.lunarvr.environment.EnvironmentManager
import com.lunarvr.environment.VREnvironmentType
import java.io.File

enum class EnvViewMode {
    MAIN_LIST,
    CAMERA_POS_CONFIG
}

class EnvironmentPanel(
    private val envManager: EnvironmentManager,
    val customModelManager: CustomModelManager,
    private val onEnvironmentChanged: () -> Unit,
    private val onCloseEnvironment: () -> Unit,
    private val onRequestNativeFilePicker: () -> Unit,
    private val onOpenKeyboardForPosition: (axis: String, currentVal: String, onValSubmitted: (String) -> Unit) -> Unit
) {
    var isVisible: Boolean = false
    var viewMode: EnvViewMode = EnvViewMode.MAIN_LIST

    val buttons = mutableListOf<AppButton>()

    private var currentCenterX = 0f
    private var currentCenterY = 0.10f

    var selectedFile: File? = null
    var inputPosX: String = "0.0"
    var inputPosY: String = "0.0"
    var inputPosZ: String = "0.0"

    init {
        setupButtons(0f, 0.10f)
    }

    fun setupButtons(centerX: Float = currentCenterX, centerY: Float = currentCenterY) {
        currentCenterX = centerX
        currentCenterY = centerY
        buttons.clear()

        val zPos = -1.30f

        // Top-left distinct close '✕' button for the panel (does not overlap content)
        buttons.add(
            AppButton("btn_close_env_top_left", "✕", centerX - 0.49f, centerY + 0.31f, zPos, 0.08f, 0.08f) {
                isVisible = false
                onCloseEnvironment()
            }
        )

        when (viewMode) {
            EnvViewMode.MAIN_LIST -> {
                val startY = centerY + 0.18f
                val btnW = 0.44f
                val btnH = 0.10f
                val colLeft = centerX - 0.24f
                val colRight = centerX + 0.24f

                val envs = VREnvironmentType.values()

                for (i in envs.indices) {
                    val env = envs[i]
                    val bx = if (i % 2 == 0) colLeft else colRight
                    val by = startY - (i / 2) * 0.12f
                    val isCurrent = !customModelManager.isCustomModelActive && (env == envManager.currentEnvironment)
                    val label = "${if (isCurrent) "✓ " else ""}${env.displayName}"

                    buttons.add(
                        AppButton("btn_env_${env.name}", label, bx, by, zPos, btnW, btnH) {
                            customModelManager.clearCustomModel()
                            envManager.setEnvironment(env)
                            customModelManager.configManager.activeEnvironmentName = env.name
                            setupButtons(currentCenterX, currentCenterY)
                            onEnvironmentChanged()
                        }
                    )
                }

                // List imported custom 3D models if any exist
                val importedModels = customModelManager.getImportedModels()
                val customStartY = startY - 0.25f

                for (i in 0 until Math.min(importedModels.size, 2)) {
                    val model = importedModels[i]
                    val isCur = customModelManager.isCustomModelActive && customModelManager.activeModelName == model.name
                    val label = "${if (isCur) "✓ 3D: " else "3D: "}${model.name.take(22)}"
                    val cyPos = customStartY - i * 0.085f

                    buttons.add(
                        AppButton("btn_custom_model_$i", label, centerX - 0.12f, cyPos, zPos, 0.50f, 0.075f) {
                            selectedFile = model.file
                            inputPosX = customModelManager.cameraOffset.posX.toString()
                            inputPosY = customModelManager.cameraOffset.posY.toString()
                            inputPosZ = customModelManager.cameraOffset.posZ.toString()
                            viewMode = EnvViewMode.CAMERA_POS_CONFIG
                            setupButtons(currentCenterX, currentCenterY)
                            onEnvironmentChanged()
                        }
                    )
                }

                // Import Button (triggers Android system file manager)
                val importY = if (importedModels.isEmpty()) customStartY else customStartY - Math.min(importedModels.size, 2) * 0.085f
                buttons.add(
                    AppButton("btn_import_native", "➕ Importar Modelo 3D (.glb / .obj)", centerX, importY, zPos, 0.52f, 0.075f) {
                        onRequestNativeFilePicker()
                    }
                )
            }

            EnvViewMode.CAMERA_POS_CONFIG -> {
                val startY = centerY + 0.10f
                val btnW = 0.24f
                val btnH = 0.08f

                // 3 Interactive Text Boxes for Camera Position (X, Y, Z) - Side by Side
                buttons.add(
                    AppButton("btn_pos_x", "Eixo X: $inputPosX", centerX - 0.28f, startY, zPos, btnW, btnH) {
                        onOpenKeyboardForPosition("X", inputPosX) { newVal ->
                            inputPosX = newVal
                            setupButtons(currentCenterX, currentCenterY)
                            onEnvironmentChanged()
                        }
                    }
                )

                buttons.add(
                    AppButton("btn_pos_y", "Eixo Y: $inputPosY", centerX, startY, zPos, btnW, btnH) {
                        onOpenKeyboardForPosition("Y", inputPosY) { newVal ->
                            inputPosY = newVal
                            setupButtons(currentCenterX, currentCenterY)
                            onEnvironmentChanged()
                        }
                    }
                )

                buttons.add(
                    AppButton("btn_pos_z", "Eixo Z: $inputPosZ", centerX + 0.28f, startY, zPos, btnW, btnH) {
                        onOpenKeyboardForPosition("Z", inputPosZ) { newVal ->
                            inputPosZ = newVal
                            setupButtons(currentCenterX, currentCenterY)
                            onEnvironmentChanged()
                        }
                    }
                )

                // Action buttons side-by-side: [ Confirmar Posições ]  [ Pular (Deixar VR Escolher) ]
                val actionY = startY - 0.15f
                val actionW = 0.42f
                val actionH = 0.085f

                // 1. Confirm and load with specified X, Y, Z coordinates
                buttons.add(
                    AppButton("btn_save_model", "💾 Confirmar Posições", centerX - 0.23f, actionY, zPos, actionW, actionH) {
                        selectedFile?.let { file ->
                            val x = inputPosX.toFloatOrNull() ?: 0f
                            val y = inputPosY.toFloatOrNull() ?: 0f
                            val z = inputPosZ.toFloatOrNull() ?: 0f
                            customModelManager.loadCustomModel(file, x, y, z)
                        }
                        viewMode = EnvViewMode.MAIN_LIST
                        setupButtons(currentCenterX, currentCenterY)
                        onEnvironmentChanged()
                    }
                )

                // 2. Button right next to Confirm: Let VR decide best camera position
                buttons.add(
                    AppButton("btn_skip_pos", "⚡ Pular (VR Escolhe)", centerX + 0.23f, actionY, zPos, actionW, actionH) {
                        selectedFile?.let { file ->
                            customModelManager.loadCustomModel(file, 0f, 0f, 0f)
                            customModelManager.autoPositionCamera()
                        }
                        viewMode = EnvViewMode.MAIN_LIST
                        setupButtons(currentCenterX, currentCenterY)
                        onEnvironmentChanged()
                    }
                )

                // Back / Cancel button below
                buttons.add(
                    AppButton("btn_cancel_pos", "◀ Voltar", centerX, actionY - 0.11f, zPos, 0.30f, 0.07f) {
                        viewMode = EnvViewMode.MAIN_LIST
                        setupButtons(currentCenterX, currentCenterY)
                        onEnvironmentChanged()
                    }
                )
            }
        }
    }
}
