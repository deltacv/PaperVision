package io.github.deltacv.easyvision.platform

import imgui.ImGui
import io.github.deltacv.easyvision.id.DrawableIdElement
import io.github.deltacv.easyvision.id.DrawableIdElementBase
import io.github.deltacv.easyvision.id.IdElementContainer
import java.nio.ByteBuffer

abstract class PlatformTexture : DrawableIdElementBase<PlatformTexture>() {

    override val idElementContainer = textures

    abstract val width: Int
    abstract val height: Int

    abstract val textureId: Int

    abstract fun set(bytes: ByteArray, colorSpace: ColorSpace = ColorSpace.RGB)
    abstract fun set(bytes: ByteBuffer, colorSpace: ColorSpace = ColorSpace.RGB)

    override fun draw() {
        ImGui.image(textureId, width.toFloat(), height.toFloat())
    }

    override fun restore() {
        throw UnsupportedOperationException("Cannot restore texture after it has been deleted")
    }

    companion object {
        val textures = IdElementContainer<PlatformTexture>()
    }

}

interface PlatformTextureFactory {
    fun create(width: Int, height: Int, bytes: ByteArray, colorSpace: ColorSpace = ColorSpace.RGB): PlatformTexture

    fun create(width: Int, height: Int, bytes: ByteBuffer, colorSpace: ColorSpace = ColorSpace.RGB): PlatformTexture

    fun create(resource: String): PlatformTexture
}

enum class ColorSpace {
    RGB, RGBA, BGR, BGRA
}