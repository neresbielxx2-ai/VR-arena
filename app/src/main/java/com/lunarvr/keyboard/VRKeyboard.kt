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

    // Modal safety confirmation state when user long-dwells on DEL
    var showClearConfirmationModal: Boolean = false

    // Enterprise VR layout (smooth, refined keys matching Meta Quest / VisionOS)
    val lowercaseRows = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("SHIFT", "z", "x", "c", "v", "b", "n", "m", "DEL"),
        listOf("?123", "SPACE", ".", "/", "ENTER")
    )

    val uppercaseRows = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
        listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
        listOf("shift", "Z", "X", "C", "V", "B", "N", "M", "DEL"),
        listOf("?123", "SPACE", ".", "/", "ENTER")
    )

    val symbolRows = listOf(
        listOf("!", "@", "#", "$", "%", "^", "&", "*", "(", ")"),
        listOf("~", "`", "+", "=", "-", "_", "{", "}", "[", "]"),
        listOf("|", "\\", ":", ";", "\"", "'", "<", ">", "DEL"),
        listOf("ABC", "SPACE", ",", ".com", "ENTER")
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
            "?123" -> currentMode = KeyboardMode.NUMBERS_SYMBOLS
            "ABC" -> currentMode = KeyboardMode.LOWERCASE
            "DEL" -> listener?.onBackspace()
            "SPACE" -> listener?.onSpace()
            "ENTER" -> listener?.onEnter()
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
