package com.lunarvr.keyboard

interface VRKeyboardListener {
    fun onKeyPressed(character: String)
    fun onBackspace()
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

    val lowercaseRows = listOf(
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("SHIFT", "z", "x", "c", "v", "b", "n", "m", "DEL"),
        listOf("123", "SPACE", ".", "/", "ENTER")
    )

    val uppercaseRows = listOf(
        listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
        listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
        listOf("shift", "Z", "X", "C", "V", "B", "N", "M", "DEL"),
        listOf("123", "SPACE", ".", "/", "ENTER")
    )

    val symbolRows = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("@", "#", "$", "%", "&", "-", "+", "(", ")"),
        listOf("ABC", "*", "\"", "'", ":", ";", "!", "?", "DEL"),
        listOf("ABC", "SPACE", ",", ".", "ENTER")
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
            "123" -> currentMode = KeyboardMode.NUMBERS_SYMBOLS
            "ABC" -> currentMode = KeyboardMode.LOWERCASE
            "DEL" -> listener?.onBackspace()
            "SPACE" -> listener?.onSpace()
            "ENTER" -> listener?.onEnter()
            else -> listener?.onKeyPressed(key)
        }
    }

    fun show() {
        isVisible = true
    }

    fun hide() {
        isVisible = false
        listener?.onCloseKeyboard()
    }
}
