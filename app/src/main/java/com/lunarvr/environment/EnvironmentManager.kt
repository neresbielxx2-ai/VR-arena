package com.lunarvr.environment

enum class VREnvironmentType(val displayName: String, val description: String) {
    BEACH_PARADISE("Praia Tropical 3D", "Oceano azul, areia dourada e coqueiros ao pôr do sol"),
    LUNAR_EARTH_VIEW("Órbita Lunar & Terra", "Superfície da Lua com a Terra flutuando ao fundo"),
    CYBER_SYNTHWAVE("Cyberpunk Synthwave", "Horizonte retro futurista com sol neon e montanhas"),
    ZEN_FOREST("Floresta Zen Noturna", "Aurora boreal, montanhas calmas e vaga-lumes"),
    PASSTHROUGH_CAM("Câmera Real (Passthrough)", "Visão do mundo real sem cenário virtual")
}

class EnvironmentManager {
    var currentEnvironment: VREnvironmentType = VREnvironmentType.BEACH_PARADISE
        private set

    fun setEnvironment(type: VREnvironmentType) {
        currentEnvironment = type
    }

    fun getClearColor(): FloatArray {
        return when (currentEnvironment) {
            VREnvironmentType.BEACH_PARADISE -> floatArrayOf(0.04f, 0.12f, 0.22f, 1.0f) // Tropical dusk
            VREnvironmentType.LUNAR_EARTH_VIEW -> floatArrayOf(0.015f, 0.020f, 0.035f, 1.0f) // Deep cosmos
            VREnvironmentType.CYBER_SYNTHWAVE -> floatArrayOf(0.080f, 0.015f, 0.090f, 1.0f) // Magenta dark
            VREnvironmentType.ZEN_FOREST -> floatArrayOf(0.010f, 0.040f, 0.030f, 1.0f) // Forest night
            VREnvironmentType.PASSTHROUGH_CAM -> floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f) // Transparent
        }
    }

    fun getStarColor(): FloatArray {
        return when (currentEnvironment) {
            VREnvironmentType.BEACH_PARADISE -> floatArrayOf(1.0f, 0.95f, 0.85f, 0.40f) // Warm tropical stars
            VREnvironmentType.LUNAR_EARTH_VIEW -> floatArrayOf(0.90f, 0.95f, 1.00f, 0.85f)
            VREnvironmentType.CYBER_SYNTHWAVE -> floatArrayOf(1.00f, 0.40f, 0.80f, 0.70f)
            VREnvironmentType.ZEN_FOREST -> floatArrayOf(0.40f, 1.00f, 0.70f, 0.80f)
            VREnvironmentType.PASSTHROUGH_CAM -> floatArrayOf(0f, 0f, 0f, 0f)
        }
    }
}
