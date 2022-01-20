package io.github.deltacv.easyvision.platform

class PlatformSetup(val name: String) {
    var window: PlatformWindow? = null
    var textureFactory: PlatformTextureFactory? = null

    var keys: PlatformKeys? = null
}

data class PlatformSetupCallback(val name: String, val block: PlatformSetup.() -> Unit) {
    fun setup(): PlatformSetup {
        val setup = PlatformSetup(name)
        setup.block()
        return setup
    }
}

fun platformSetup(name: String, block: PlatformSetup.() -> Unit) = PlatformSetupCallback(name, block)