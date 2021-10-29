package io.github.deltacv.easyvision.io

import io.github.deltacv.easyvision.EasyVision

class KeyManager {

    private val pressedKeys = mutableMapOf<Int, Boolean>()
    private val pressingKeys = mutableMapOf<Int, Boolean>()
    private val releasedKeys = mutableMapOf<Int, Boolean>()

    fun update() {
        if(pressedKeys.isNotEmpty()) {
            for (key in pressedKeys.keys.toTypedArray()) {
                pressedKeys[key] = false
            }
        }

        if(releasedKeys.isNotEmpty()) {
            for (key in releasedKeys.keys.toTypedArray()) {
                releasedKeys[key] = false
            }
        }
    }

    fun updateKey(scancode: Int, action: KeyAction) {
        when (action) {
            KeyAction.PRESS -> {
                pressedKeys[scancode] = true
                pressingKeys[scancode] = false
                pressingKeys[scancode] = true
            }
            KeyAction.PRESSING -> {
                pressedKeys[scancode] = false
                pressingKeys[scancode] = true
                releasedKeys[scancode] = false
            }
            KeyAction.RELEASE -> {
                pressingKeys[scancode] = false
                pressingKeys[scancode] = false
                releasedKeys[scancode] = true
            }
            else -> {
                pressedKeys[scancode] = false
                pressingKeys[scancode] = false
                releasedKeys[scancode] = false
            }
        }
    }

    fun pressed(scancode: Int)  = pressedKeys[scancode]  ?: false
    fun pressing(scancode: Int) = pressingKeys[scancode] ?: false
    fun released(scancode: Int) = releasedKeys[scancode] ?: false

}

enum class KeyAction { PRESS, PRESSING, RELEASE, UNKNOWN }

val Keys = EasyVision.platformKeys