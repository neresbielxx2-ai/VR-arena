package com.lunarvr.keyboard

interface VRKeyboardListener {
    fun onKeyPressed(character: String)
    fun onBackspace()
    fun onClearAll()
    fun onSpace()
    fun onEnter()
    fun onCloseKeyboard()
}

enum class KeyboardMode {
    LOWERCASE,
    UPPERCASE,
    NUMBERS_SYMBOLS
}

class VRKeyboard {

    var listener: VRKeyboardListener? = null
    var isVisible: Boolean = false
    var currentMode: KeyboardMode = KeyboardMode.LOWERCASE
    var showClearConfirmationModal: Boolean = false

    // Meta Quest Horizon OS System Keyboard Layout (matching reference screenshot)
    val lowercaseRows = listOf(
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p", "BACKSPACE"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l", "ENTER"),
        listOf("SHIFT", "z", "x", "c", "v", "b", "n", "m", ",", ".", "SHIFT"),
        listOf("!123", "EMOJI", "SPACE", "LANG", "SETTINGS", "HIDE_KB")
    )

    val uppercaseRows = listOf(
        listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "BACKSPACE"),
        listOf("A", "S", "D", "F", "G", "H", "J", "K", "L", "ENTER"),
        listOf("shift", "Z", "X", "C", "V", "B", "N", "M", ",", ".", "shift"),
        listOf("!123", "EMOJI", "SPACE", "LANG", "SETTINGS", "HIDE_KB")
    )

    val symbolRows = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "BACKSPACE"),
        listOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "ENTER"),
        listOf("ABC", "*", "\"", "'", ":", ";", "!", "?", "/", "\\", "ABC"),
        listOf("ABC", "EMOJI", "SPACE", "LANG", "SETTINGS", "HIDE_KB")
    )

    fun getCurrentRows(): List<List<String>> {
        return when (currentMode) {
            KeyboardMode.LOWERCASE -> lowercaseRows
            KeyboardMode.UPPERCASE -> uppercaseRows
            KeyboardMode.NUMBERS_SYMBOLS -> symbolRows
        }
    }

    fun handleKeyPress(key: String) {
        when (key) {
            "SHIFT" -> currentMode = KeyboardMode.UPPERCASE
            "shift" -> currentMode = KeyboardMode.LOWERCASE
            "!123" -> currentMode = KeyboardMode.NUMBERS_SYMBOLS
            "ABC" -> currentMode = KeyboardMode.LOWERCASE
            "BACKSPACE", "DEL" -> listener?.onBackspace()
            "SPACE" -> listener?.onSpace()
            "ENTER" -> listener?.onEnter()
            "HIDE_KB" -> hide()
            "EMOJI", "LANG", "SETTINGS" -> {
                // System utility keys
            }
            else -> listener?.onKeyPressed(key)
        }
    }

    fun requestClearAllPrompt() {
        showClearConfirmationModal = true
    }

    fun confirmClearAll() {
        showClearConfirmationModal = false
        listener?.onClearAll()
    }

    fun dismissClearModal() {
        showClearConfirmationModal = false
    }

    fun show() {
        isVisible = true
        showClearConfirmationModal = false
    }

    fun hide() {
        isVisible = false
        showClearConfirmationModal = false
        listener?.onCloseKeyboard()
    }
}
