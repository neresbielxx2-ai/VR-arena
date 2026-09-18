package com.lunarvr.system

class PerformanceManager {

    var currentLevel: PerformanceLevel = PerformanceLevel.NORMAL
        private set

    var isAuto: Boolean = true
    var targetFps: Int = 60
        private set
    var enableAnimations: Boolean = true
        private set
    var starCount: Int = 400
        private set
    var renderScale: Float = 1.0f
        private set

    fun applyLevel(level: PerformanceLevel, isAutomatic: Boolean = false) {
        this.currentLevel = level
        this.isAuto = isAutomatic

        when (level) {
            PerformanceLevel.ECONOMIC -> {
                targetFps = 30
                enableAnimations = false
                starCount = 150
                renderScale = 0.75f
            }
            PerformanceLevel.NORMAL -> {
                targetFps = 60
                enableAnimations = true
                starCount = 350
                renderScale = 1.0f
            }
            PerformanceLevel.QUALITY -> {
                targetFps = 90
                enableAnimations = true
                starCount = 700
                renderScale = 1.0f
            }
        }
    }
}
