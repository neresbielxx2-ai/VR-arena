package com.lunarvr.keyboard

class TextInputManager {

    interface TextInputTarget {
        fun onTextUpdated(text: String)
        fun onInputSubmitted(text: String)
    }

    private var activeTarget: TextInputTarget? = null
    private var buffer = StringBuilder()

    fun bindTarget(target: TextInputTarget, initialText: String = "") {
        activeTarget = target
        buffer.clear()
        buffer.append(initialText)
    }

    fun unbind() {
        activeTarget = null
        buffer.clear()
    }

    fun append(char: String) {
        buffer.append(char)
        activeTarget?.onTextUpdated(buffer.toString())
    }

    fun backspace() {
        if (buffer.isNotEmpty()) {
            buffer.deleteCharAt(buffer.length - 1)
            activeTarget?.onTextUpdated(buffer.toString())
        }
    }

    fun appendSpace() {
        buffer.append(" ")
        activeTarget?.onTextUpdated(buffer.toString())
    }

    fun submit() {
        activeTarget?.onInputSubmitted(buffer.toString())
    }

    fun getCurrentText(): String = buffer.toString()
}
