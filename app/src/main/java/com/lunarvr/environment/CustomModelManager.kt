package com.lunarvr.environment

import android.os.Environment
import android.util.Log
import java.io.File

data class CameraPosition(
    var posX: Float = 0f,
    var posY: Float = 0f,
    var posZ: Float = 0f
)

class CustomModelManager {

    var activeCustomModel: MeshData? = null
        private set
    var activeModelName: String? = null
        private set
    var cameraOffset = CameraPosition(0f, 0f, 0f)

    var isCustomModelActive: Boolean = false

    fun getAvailableModelFiles(): List<File> {
        val files = mutableListOf<File>()
        try {
            // Check Downloads
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir != null && downloadsDir.exists()) {
                downloadsDir.listFiles()?.forEach { file ->
                    val name = file.name.lowercase()
                    if (name.endsWith(".glb") || name.endsWith(".obj") || name.endsWith(".gltf")) {
                        files.add(file)
                    }
                }
            }

            // Check Documents
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (docsDir != null && docsDir.exists()) {
                docsDir.listFiles()?.forEach { file ->
                    val name = file.name.lowercase()
                    if (name.endsWith(".glb") || name.endsWith(".obj") || name.endsWith(".gltf")) {
                        files.add(file)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CustomModelManager", "Error querying device directories", e)
        }
        return files
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
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("CustomModelManager", "Failed to load model file", e)
            false
        }
    }

    fun autoPositionCamera(): CameraPosition {
        val mesh = activeCustomModel
        if (mesh == null) {
            return CameraPosition(0f, 0f, 0f)
        }
        // Place camera at center looking into the model scene
        val cx = (mesh.minX + mesh.maxX) / 2f
        val cy = (mesh.minY + mesh.maxY) / 2f
        val cz = (mesh.minZ + mesh.maxZ) / 2f
        return CameraPosition(
            if (cx.isNaN()) 0f else cx,
            if (cy.isNaN()) 0f else cy,
            if (cz.isNaN()) 0f else cz
        )
    }

    fun clearCustomModel() {
        activeCustomModel = null
        activeModelName = null
        isCustomModelActive = false
    }
}
