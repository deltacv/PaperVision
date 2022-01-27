package io.github.deltacv.easyvision.platform.animation

import io.github.deltacv.easyvision.id.IdElement
import io.github.deltacv.easyvision.id.IdElementContainer
import io.github.deltacv.easyvision.platform.ColorSpace
import io.github.deltacv.easyvision.platform.PlatformTexture
import java.lang.UnsupportedOperationException
import java.nio.ByteBuffer

abstract class PlatformTextureAnimation : PlatformTexture() {

    abstract var frame: Int

    abstract var isActive: Boolean

    override val id by animations.nextId()

    var isPaused: Boolean
        get() = !isActive
        set(value) {
            isActive = !value
        }

    abstract fun next()

    abstract fun update()

    override fun draw() {
        update()
        super.draw()
    }

    override fun set(bytes: ByteArray, colorSpace: ColorSpace) =
        throw UnsupportedOperationException("set() is not supported on animations")

    override fun set(bytes: ByteBuffer, colorSpace: ColorSpace) =
        throw UnsupportedOperationException("set() is not supported on animations")

    companion object {
        val animations = IdElementContainer<PlatformTextureAnimation>()
    }

}