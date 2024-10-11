package io.github.deltacv.papervision.platform

import io.github.deltacv.papervision.engine.bridge.PaperVisionEngineBridge

class PlatformSetup(val name: String) {
    var window: PlatformWindow? = null
    var textureFactory: PlatformTextureFactory? = null

    var keys: PlatformKeys? = null

    var engineBridge: PaperVisionEngineBridge? = null
}

data class PlatformSetupCallback(val name: String, val block: PlatformSetup.() -> Unit) {
    fun setup(): PlatformSetup {
        val setup = PlatformSetup(name)
        setup.block()
        return setup
    }
}

fun platformSetup(name: String, block: PlatformSetup.() -> Unit) = PlatformSetupCallback(name, block)