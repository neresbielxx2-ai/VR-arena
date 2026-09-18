package com.lunarvr.environment

enum class VREnvironmentType(val displayName: String, val description: String) {
    LUNAR_SPACE("Espaço Lunar", "Noite profunda com campo estelar brilhante"),
    NEBULA_DREAM("Nebulosa Cósmica", "Tons de violeta e ciano interestelar"),
    CYBER_GRID("Grid Sci-Fi", "Matrix geométrica minimalista estilo Tron"),
    DEEP_VOID("Vácuo Noturno", "Preto OLED absoluto para máxima concentração")
}

class EnvironmentManager {
    var currentEnvironment: VREnvironmentType = VREnvironmentType.LUNAR_SPACE
        private set

    fun setEnvironment(type: VREnvironmentType) {
        currentEnvironment = type
    }

    fun getClearColor(): FloatArray {
        return when (currentEnvironment) {
            VREnvironmentType.LUNAR_SPACE -> floatArrayOf(0.027f, 0.039f, 0.070f, 1.0f) // #070A12
            VREnvironmentType.NEBULA_DREAM -> floatArrayOf(0.060f, 0.020f, 0.090f, 1.0f) // Dark violet
            VREnvironmentType.CYBER_GRID -> floatArrayOf(0.010f, 0.040f, 0.060f, 1.0f)  // Dark cyan
            VREnvironmentType.DEEP_VOID -> floatArrayOf(0.002f, 0.002f, 0.005f, 1.0f)   // Pure OLED black
        }
    }

    fun getStarColor(): FloatArray {
        return when (currentEnvironment) {
            VREnvironmentType.LUNAR_SPACE -> floatArrayOf(0.80f, 0.90f, 1.00f, 0.75f)
            VREnvironmentType.NEBULA_DREAM -> floatArrayOf(0.95f, 0.60f, 0.98f, 0.85f)
            VREnvironmentType.CYBER_GRID -> floatArrayOf(0.00f, 0.95f, 1.00f, 0.80f)
            VREnvironmentType.DEEP_VOID -> floatArrayOf(0.40f, 0.50f, 0.60f, 0.30f)
        }
    }
}
