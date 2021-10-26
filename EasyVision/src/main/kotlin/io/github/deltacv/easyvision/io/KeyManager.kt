package io.github.deltacv.easyvision.io

import org.lwjgl.glfw.GLFW.*

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

    fun updateKey(scancode: Int, action: Int) {
        when (action) {
            GLFW_PRESS -> {
                pressedKeys[scancode] = true
                pressingKeys[scancode] = true
            }
            GLFW_RELEASE -> {
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