/*
 * PaperVision
 * Copyright (C) 2024 Sebastian Erives, deltacv

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.deltacv.papervision.io

import io.github.deltacv.papervision.platform.PlatformKeys

class KeyManager(val keys: PlatformKeys) {

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