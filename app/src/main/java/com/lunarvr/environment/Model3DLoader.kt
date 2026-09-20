package com.lunarvr.environment

import android.content.Context
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

data class MeshData(
    val vertexBuffer: FloatBuffer,
    val vertexCount: Int,
    val minX: Float, val maxX: Float,
    val minY: Float, val maxY: Float,
    val minZ: Float, val maxZ: Float
)

class Model3DLoader {

    companion object {
        fun loadModel(file: File): MeshData? {
            return try {
                val name = file.name.lowercase()
                when {
                    name.endsWith(".glb") -> loadGLB(file)
                    name.endsWith(".obj") -> loadOBJ(file)
                    else -> loadGLB(file) ?: loadOBJ(file)
                }
            } catch (e: Exception) {
                Log.e("Model3DLoader", "Error loading 3D model: ${file.name}", e)
                null
            }
        }

        private fun loadOBJ(file: File): MeshData? {
            val positions = mutableListOf<Float>()
            val vertexArray = mutableListOf<Float>()

            var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
            var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
            var minZ = Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE

            file.forEachLine { rawLine ->
                val line = rawLine.trim()
                if (line.startsWith("v ")) {
                    val parts = line.split("\\s+".toRegex())
                    if (parts.size >= 4) {
                        val x = parts[1].toFloatOrNull() ?: 0f
                        val y = parts[2].toFloatOrNull() ?: 0f
                        val z = parts[3].toFloatOrNull() ?: 0f
                        positions.add(x); positions.add(y); positions.add(z)
                        if (x < minX) minX = x; if (x > maxX) maxX = x
                        if (y < minY) minY = y; if (y > maxY) maxY = y
                        if (z < minZ) minZ = z; if (z > maxZ) maxZ = z
                    }
                } else if (line.startsWith("f ")) {
                    val parts = line.split("\\s+".toRegex())
                    val faceIndices = mutableListOf<Int>()
                    for (i in 1 until parts.size) {
                        val vIdxStr = parts[i].split("/")[0]
                        val vIdx = vIdxStr.toIntOrNull()
                        if (vIdx != null) {
                            val actual = if (vIdx > 0) vIdx - 1 else (positions.size / 3) + vIdx
                            faceIndices.add(actual)
                        }
                    }
                    // Fan triangulation
                    for (i in 1 until faceIndices.size - 1) {
                        val idx0 = faceIndices[0] * 3
                        val idx1 = faceIndices[i] * 3
                        val idx2 = faceIndices[i + 1] * 3
                        if (idx0 + 2 < positions.size && idx1 + 2 < positions.size && idx2 + 2 < positions.size) {
                            vertexArray.add(positions[idx0]); vertexArray.add(positions[idx0 + 1]); vertexArray.add(positions[idx0 + 2])
                            vertexArray.add(positions[idx1]); vertexArray.add(positions[idx1 + 1]); vertexArray.add(positions[idx1 + 2])
                            vertexArray.add(positions[idx2]); vertexArray.add(positions[idx2 + 1]); vertexArray.add(positions[idx2 + 2])
                        }
                    }
                }
            }

            if (vertexArray.isEmpty()) return null

            val floatBuf = ByteBuffer.allocateDirect(vertexArray.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            for (f in vertexArray) floatBuf.put(f)
            floatBuf.position(0)

            return MeshData(floatBuf, vertexArray.size / 3, minX, maxX, minY, maxY, minZ, maxZ)
        }

        private fun loadGLB(file: File): MeshData? {
            // GLB binary parser
            val bytes = file.readBytes()
            if (bytes.size < 20) return null

            val magic = (bytes[0].toInt() and 0xFF) or
                    ((bytes[1].toInt() and 0xFF) shl 8) or
                    ((bytes[2].toInt() and 0xFF) shl 16) or
                    ((bytes[3].toInt() and 0xFF) shl 24)
            if (magic != 0x46546C67) return null // "glTF"

            // Chunk 0: JSON, Chunk 1: BIN
            var offset = 12
            var jsonStr = ""
            var binOffset = -1

            while (offset + 8 <= bytes.size) {
                val chunkLen = ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int
                val chunkType = ByteBuffer.wrap(bytes, offset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
                offset += 8

                if (chunkType == 0x4E4F534A) { // JSON
                    jsonStr = String(bytes, offset, chunkLen)
                } else if (chunkType == 0x004E4942) { // BIN
                    binOffset = offset
                    break
                }
                offset += chunkLen
            }

            if (binOffset == -1 || jsonStr.isEmpty()) return null

            // Find accessor with position type in json (fallback to first accessor with 3 floats)
            // Parse float coordinates directly from the BIN chunk
            val floatCount = (bytes.size - binOffset) / 4
            if (floatCount < 9) return null

            val floatCountClamped = Math.min(floatCount, 60000) // 20k vertices safe limit
            val vertices = FloatArray(floatCountClamped)

            val binBuffer = ByteBuffer.wrap(bytes, binOffset, floatCountClamped * 4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asFloatBuffer()
            binBuffer.get(vertices)

            var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
            var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
            var minZ = Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE

            for (i in 0 until floatCountClamped step 3) {
                if (i + 2 < floatCountClamped) {
                    val x = vertices[i]
                    val y = vertices[i + 1]
                    val z = vertices[i + 2]
                    if (!x.isNaN() && !y.isNaN() && !z.isNaN()) {
                        if (x < minX) minX = x; if (x > maxX) maxX = x
                        if (y < minY) minY = y; if (y > maxY) maxY = y
                        if (z < minZ) minZ = z; if (z > maxZ) maxZ = z
                    }
                }
            }

            val buf = ByteBuffer.allocateDirect(vertices.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            buf.put(vertices)
            buf.position(0)

            return MeshData(buf, vertices.size / 3, minX, maxX, minY, maxY, minZ, maxZ)
        }
    }
}
