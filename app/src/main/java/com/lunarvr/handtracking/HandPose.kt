package com.lunarvr.handtracking

data class Landmark3D(
    val x: Float,
    val y: Float,
    val z: Float
)

data class HandPose(
    val isDetected: Boolean,
    val isLeftHand: Boolean,
    val wrist: Landmark3D,
    val indexTip: Landmark3D,
    val indexPip: Landmark3D,
    val indexMcp: Landmark3D,
    val thumbTip: Landmark3D,
    val middleTip: Landmark3D,
    val ringTip: Landmark3D,
    val pinkyTip: Landmark3D,
    val palmCenter: Landmark3D,
    val allLandmarks: List<Landmark3D> = emptyList()
) {
    companion object {
        fun empty(): HandPose = HandPose(
            isDetected = false,
            isLeftHand = false,
            wrist = Landmark3D(0f, 0f, 0f),
            indexTip = Landmark3D(0f, 0f, 0f),
            indexPip = Landmark3D(0f, 0f, 0f),
            indexMcp = Landmark3D(0f, 0f, 0f),
            thumbTip = Landmark3D(0f, 0f, 0f),
            middleTip = Landmark3D(0f, 0f, 0f),
            ringTip = Landmark3D(0f, 0f, 0f),
            pinkyTip = Landmark3D(0f, 0f, 0f),
            palmCenter = Landmark3D(0f, 0f, 0f)
        )
    }
}
