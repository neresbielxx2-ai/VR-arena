package com.lunarvr.ui

import com.lunarvr.environment.CustomModelManager
import com.lunarvr.environment.EnvironmentManager
import com.lunarvr.environment.VREnvironmentType
import java.io.File

enum class EnvViewMode {
    GRID,
    FILE_PICKER,
    CAMERA_POS_CONFIG
}

class EnvironmentPanel(
    private val envManager: EnvironmentManager,
    val customModelManager: CustomModelManager,
    private val onEnvironmentChanged: () -> Unit,
    private val onOpenKeyboardForPosition: (axis: String, currentVal: String, onValSubmitted: (String) -> Unit) -> Unit
) {
    var isVisible: Boolean = false
    var viewMode: EnvViewMode = EnvViewMode.GRID

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

        when (viewMode) {
            EnvViewMode.GRID -> {
                val startY = centerY + 0.18f
                val btnW = 0.44f
                val btnH = 0.11f
                val colLeft = centerX - 0.24f
                val colRight = centerX + 0.24f

                val envs = VREnvironmentType.values()

                for (i in envs.indices) {
                    val env = envs[i]
                    val bx = if (i % 2 == 0) colLeft else colRight
                    val by = startY - (i / 2) * 0.13f
                    val isCurrent = !customModelManager.isCustomModelActive && (env == envManager.currentEnvironment)
                    val label = "${if (isCurrent) "✓ " else ""}${env.displayName}"

                    buttons.add(
                        AppButton("btn_env_${env.name}", label, bx, by, zPos, btnW, btnH) {
                            customModelManager.clearCustomModel()
                            envManager.setEnvironment(env)
                            setupButtons(currentCenterX, currentCenterY)
                            onEnvironmentChanged()
                        }
                    )
                }

                // Add "+" Button to import custom 3D model (GLB, OBJ)
                val plusY = startY - 0.27f
                buttons.add(
                    AppButton("btn_add_custom_env", "➕ Adicionar Cenário 3D (.glb / .obj)", centerX - 0.14f, plusY, zPos, 0.44f, 0.08f) {
                        viewMode = EnvViewMode.FILE_PICKER
                        setupButtons(currentCenterX, currentCenterY)
                        onEnvironmentChanged()
                    }
                )

                // Close button at bottom
                buttons.add(
                    AppButton("btn_close_env", "✕ Fechar", centerX + 0.26f, plusY, zPos, 0.22f, 0.08f) {
                        isVisible = false
                        onEnvironmentChanged()
                    }
                )
            }

            EnvViewMode.FILE_PICKER -> {
                val startY = centerY + 0.18f
                val files = customModelManager.getAvailableModelFiles()

                if (files.isEmpty()) {
                    // Back button
                    buttons.add(
                        AppButton("btn_file_back", "◀ Voltar aos Cenários", centerX, centerY - 0.15f, zPos, 0.45f, 0.08f) {
                            viewMode = EnvViewMode.GRID
                            setupButtons(currentCenterX, currentCenterY)
                            onEnvironmentChanged()
                        }
                    )
                } else {
                    for (i in 0 until Math.min(files.size, 4)) {
                        val file = files[i]
                        val by = startY - i * 0.09f
                        val label = "📁 ${file.name.take(28)}"

                        buttons.add(
                            AppButton("btn_select_file_$i", label, centerX, by, zPos, 0.65f, 0.075f) {
                                selectedFile = file
                                viewMode = EnvViewMode.CAMERA_POS_CONFIG
                                setupButtons(currentCenterX, currentCenterY)
                                onEnvironmentChanged()
                            }
                        )
                    }

                    // Back button at bottom
                    buttons.add(
                        AppButton("btn_file_back", "◀ Voltar", centerX, centerY - 0.24f, zPos, 0.35f, 0.075f) {
                            viewMode = EnvViewMode.GRID
                            setupButtons(currentCenterX, currentCenterY)
                            onEnvironmentChanged()
                        }
                    )
                }
            }

            EnvViewMode.CAMERA_POS_CONFIG -> {
                val startY = centerY + 0.08f
                val btnW = 0.24f
                val btnH = 0.075f

                // 3 Interactive Text Boxes for Camera Position (X, Y, Z)
                buttons.add(
                    AppButton("btn_pos_x", "X: $inputPosX", centerX - 0.28f, startY, zPos, btnW, btnH) {
                        onOpenKeyboardForPosition("X", inputPosX) { newVal ->
                            inputPosX = newVal
                            setupButtons(currentCenterX, currentCenterY)
                            onEnvironmentChanged()
                        }
                    }
                )

                buttons.add(
                    AppButton("btn_pos_y", "Y: $inputPosY", centerX, startY, zPos, btnW, btnH) {
                        onOpenKeyboardForPosition("Y", inputPosY) { newVal ->
                            inputPosY = newVal
                            setupButtons(currentCenterX, currentCenterY)
                            onEnvironmentChanged()
                        }
                    }
                )

                buttons.add(
                    AppButton("btn_pos_z", "Z: $inputPosZ", centerX + 0.28f, startY, zPos, btnW, btnH) {
                        onOpenKeyboardForPosition("Z", inputPosZ) { newVal ->
                            inputPosZ = newVal
                            setupButtons(currentCenterX, currentCenterY)
                            onEnvironmentChanged()
                        }
                    }
                )

                // Save button
                buttons.add(
                    AppButton("btn_save_model", "💾 Salvar e Carregar Cenário", centerX - 0.18f, startY - 0.16f, zPos, 0.40f, 0.08f) {
                        selectedFile?.let { file ->
                            val x = inputPosX.toFloatOrNull() ?: 0f
                            val y = inputPosY.toFloatOrNull() ?: 0f
                            val z = inputPosZ.toFloatOrNull() ?: 0f
                            customModelManager.loadCustomModel(file, x, y, z)
                        }
                        viewMode = EnvViewMode.GRID
                        setupButtons(currentCenterX, currentCenterY)
                        onEnvironmentChanged()
                    }
                )

                // Skip / Auto button (Pular e deixar o VR decidir)
                buttons.add(
                    AppButton("btn_skip_pos", "⚡ Pular (Auto Posição)", centerX + 0.24f, startY - 0.16f, zPos, 0.38f, 0.08f) {
                        selectedFile?.let { file ->
                            customModelManager.loadCustomModel(file, 0f, 0f, 0f)
                            val autoPos = customModelManager.autoPositionCamera()
                            customModelManager.cameraOffset = autoPos
                        }
                        viewMode = EnvViewMode.GRID
                        setupButtons(currentCenterX, currentCenterY)
                        onEnvironmentChanged()
                    }
                )

                // Cancel button
                buttons.add(
                    AppButton("btn_cancel_pos", "✕ Cancelar", centerX, startY - 0.26f, zPos, 0.28f, 0.07f) {
                        viewMode = EnvViewMode.FILE_PICKER
                        setupButtons(currentCenterX, currentCenterY)
                        onEnvironmentChanged()
                    }
                )
            }
        }
    }
}
