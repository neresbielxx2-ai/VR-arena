package com.lunarvr.keyboard

import com.lunarvr.ui.AppButton

class VRKey(
    id: String,
    val character: String,
    x: Float,
    y: Float,
    z: Float,
    width: Float,
    height: Float,
    val onLongDwell: (() -> Unit)? = null,
    onPress: () -> Unit
) : AppButton(id, character, x, y, z, width, height, onPress) {

    private var longDwellTriggered = false

    override fun onHoverEnter() {
        super.onHoverEnter()
        longDwellTriggered = false
    }

    override fun onHoverProgress(progress: Float) {
        super.onHoverProgress(progress)
        if (onLongDwell != null && !longDwellTriggered && progress >= 0.70f) {
            longDwellTriggered = true
            onLongDwell.invoke()
        }
    }

    override fun onHoverExit() {
        super.onHoverExit()
        longDwellTriggered = false
    }

    override fun onClick() {
        if (!longDwellTriggered) {
            super.onClick()
        }
    }
}
