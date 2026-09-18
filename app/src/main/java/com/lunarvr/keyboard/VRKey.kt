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
    onPress: () -> Unit
) : AppButton(id, character, x, y, z, width, height, onPress)
