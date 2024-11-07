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

package io.github.deltacv.papervision.platform

import io.github.deltacv.papervision.engine.bridge.PaperVisionEngineBridge
import io.github.deltacv.papervision.engine.client.ByteMessageReceiver

class PlatformSetup(val name: String) {
    var window: PlatformWindow? = null
    var textureFactory: PlatformTextureFactory? = null

    var keys: PlatformKeys? = null

    var showWelcomeWindow = false

    var engineBridge: PaperVisionEngineBridge? = null
    var previzByteMessageReceiverProvider: (() -> ByteMessageReceiver)? = null

    var config: PlatformConfig = DefaultFilePlatformConfig()
}

data class PlatformSetupCallback(val name: String, val block: PlatformSetup.() -> Unit) {
    fun setup(): PlatformSetup {
        val setup = PlatformSetup(name)
        setup.block()
        return setup
    }
}

fun platformSetup(name: String, block: PlatformSetup.() -> Unit) = PlatformSetupCallback(name, block)