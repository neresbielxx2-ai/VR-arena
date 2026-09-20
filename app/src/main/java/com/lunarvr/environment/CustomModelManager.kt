package com.lunarvr.environment

import android.content.Context
import android.net.Uri
import android.util.Log
import com.lunarvr.system.ConfigManager
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

data class CameraPosition(
    var posX: Float = 0f,
    var posY: Float = 0f,
    var posZ: Float = 0f
)

data class SavedCustomModel(
    val id: String,
    val name: String,
    val file: File
)

class CustomModelManager(private val context: Context, val configManager: ConfigManager) {

    var activeCustomModel: MeshData? = null
        private set
    var activeModelName: String? = null
        private set
    var cameraOffset = CameraPosition(0f, 0f, 0f)

    var isCustomModelActive: Boolean = false

    init {
        restorePersistedModel()
    }

    private fun getModelsDir(): File {
        val dir = File(context.filesDir, "custom_3d_models")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getImportedModels(): List<SavedCustomModel> {
        val list = mutableListOf<SavedCustomModel>()
        try {
            val dir = getModelsDir()
            dir.listFiles()?.forEach { file ->
                val name = file.name.lowercase()
                if (name.endsWith(".glb") || name.endsWith(".obj") || name.endsWith(".gltf")) {
                    list.add(SavedCustomModel(file.name, file.name, file))
                }
            }
        } catch (e: Exception) {
            Log.e("CustomModelManager", "Error reading imported models", e)
        }
        return list
    }

    fun importModelFromUri(uri: Uri, displayName: String?): SavedCustomModel? {
        return try {
            val cleanName = (displayName ?: "custom_model_${System.currentTimeMillis()}.glb")
                .replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val targetFile = File(getModelsDir(), cleanName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            SavedCustomModel(targetFile.name, targetFile.name, targetFile)
        } catch (e: Exception) {
            Log.e("CustomModelManager", "Failed to import model from uri", e)
            null
        }
    }

    fun loadCustomModel(file: File, posX: Float, posY: Float, posZ: Float): Boolean {
        return try {
            val mesh = Model3DLoader.loadModel(file)
            if (mesh != null) {
                activeCustomModel = mesh
                activeModelName = file.name
                cameraOffset.posX = posX
                cameraOffset.posY = posY
                cameraOffset.posZ = posZ
                isCustomModelActive = true

                // Persist selection and camera offset
                configManager.isCustomModelActive = true
                configManager.customModelName = file.name
                configManager.customModelPath = file.absolutePath
                configManager.cameraPosX = posX
                configManager.cameraPosY = posY
                configManager.cameraPosZ = posZ
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("CustomModelManager", "Failed to load model file", e)
            false
        }
    }

    private fun restorePersistedModel() {
        try {
            if (configManager.isCustomModelActive) {
                val path = configManager.customModelPath
                if (path != null) {
                    val file = File(path)
                    if (file.exists()) {
                        val posX = configManager.cameraPosX
                        val posY = configManager.cameraPosY
                        val posZ = configManager.cameraPosZ
                        val mesh = Model3DLoader.loadModel(file)
                        if (mesh != null) {
                            activeCustomModel = mesh
                            activeModelName = file.name
                            cameraOffset.posX = posX
                            cameraOffset.posY = posY
                            cameraOffset.posZ = posZ
                            isCustomModelActive = true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CustomModelManager", "Error restoring persisted custom model", e)
        }
    }

    fun autoPositionCamera(): CameraPosition {
        val mesh = activeCustomModel
        if (mesh == null) {
            return CameraPosition(0f, 0f, 0f)
        }
        val cx = (mesh.minX + mesh.maxX) / 2f
        val cy = (mesh.minY + mesh.maxY) / 2f
        val cz = (mesh.minZ + mesh.maxZ) / 2f
        val autoPos = CameraPosition(
            if (cx.isNaN()) 0f else cx,
            if (cy.isNaN()) 0f else cy,
            if (cz.isNaN()) 0f else cz
        )
        cameraOffset = autoPos
        configManager.cameraPosX = autoPos.posX
        configManager.cameraPosY = autoPos.posY
        configManager.cameraPosZ = autoPos.posZ
        return autoPos
    }

    fun clearCustomModel() {
        activeCustomModel = null
        activeModelName = null
        isCustomModelActive = false
        configManager.isCustomModelActive = false
        configManager.customModelName = null
        configManager.customModelPath = null
    }
}
