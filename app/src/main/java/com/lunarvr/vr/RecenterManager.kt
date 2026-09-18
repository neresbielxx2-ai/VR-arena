package com.lunarvr.vr

class RecenterManager(private val headTracking: HeadTracking) {

    private var recenterListener: (() -> Unit)? = null

    fun setOnRecenterListener(listener: () -> Unit) {
        this.recenterListener = listener
    }

    fun triggerRecenter() {
        headTracking.recenter()
        recenterListener?.invoke()
    }
}
