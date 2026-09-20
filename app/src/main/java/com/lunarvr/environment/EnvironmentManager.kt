package com.lunarvr.environment

enum class VREnvironmentType(val displayName: String, val description: String) {
    LUNAR_EARTH_VIEW("Órbita Lunar & Terra", "Superfície da Lua com a Terra flutuando ao fundo"),
    CYBER_SYNTHWAVE("Cyberpunk Synthwave", "Horizonte retro futurista com sol neon e montanhas"),
    ZEN_FOREST("Floresta Zen Noturna", "Aurora boreal, montanhas calmas e vaga-lumes"),
    MINIMAL_LOFT("Sky Loft Moderno", "Piso de mirante de vidro em arranha-céu com horizonte urbano"),
    PASSTHROUGH_CAM("Câmera Real (Passthrough)", "Visão do mundo real sem cenário virtual")
}

class EnvironmentManager {
    var currentEnvironment: VREnvironmentType = VREnvironmentType.LUNAR_EARTH_VIEW
        private set

    fun setEnvironment(type: VREnvironmentType) {
        currentEnvironment = type
    }

    fun getClearColor(): FloatArray {
        return when (currentEnvironment) {
            VREnvironmentType.LUNAR_EARTH_VIEW -> floatArrayOf(0.015f, 0.020f, 0.035f, 1.0f) // Deep cosmos
            VREnvironmentType.CYBER_SYNTHWAVE -> floatArrayOf(0.080f, 0.015f, 0.090f, 1.0f) // Magenta dark
            VREnvironmentType.ZEN_FOREST -> floatArrayOf(0.010f, 0.040f, 0.030f, 1.0f) // Forest night
            VREnvironmentType.MINIMAL_LOFT -> floatArrayOf(0.020f, 0.030f, 0.045f, 1.0f) // Penthouse night
            VREnvironmentType.PASSTHROUGH_CAM -> floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f) // Transparent for real camera passthrough
        }
    }

    fun getStarColor(): FloatArray {
        return when (currentEnvironment) {
            VREnvironmentType.LUNAR_EARTH_VIEW -> floatArrayOf(0.90f, 0.95f, 1.00f, 0.85f)
            VREnvironmentType.CYBER_SYNTHWAVE -> floatArrayOf(1.00f, 0.40f, 0.80f, 0.70f)
            VREnvironmentType.ZEN_FOREST -> floatArrayOf(0.40f, 1.00f, 0.70f, 0.80f)
            VREnvironmentType.MINIMAL_LOFT -> floatArrayOf(0.80f, 0.85f, 0.95f, 0.60f)
            VREnvironmentType.PASSTHROUGH_CAM -> floatArrayOf(0f, 0f, 0f, 0f)
        }
    }
}
