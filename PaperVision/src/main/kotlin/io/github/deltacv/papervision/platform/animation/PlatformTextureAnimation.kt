package io.github.deltacv.papervision.platform.animation

import io.github.deltacv.papervision.id.IdElementContainer
import io.github.deltacv.papervision.platform.ColorSpace
import io.github.deltacv.papervision.platform.PlatformTexture
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