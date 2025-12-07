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

import imgui.ImGui
import io.github.deltacv.papervision.id.DrawableIdElementBase
import io.github.deltacv.papervision.id.IdContainerStacks
import io.github.deltacv.papervision.io.TextureProcessorQueue
import java.nio.ByteBuffer

abstract class PlatformTexture : DrawableIdElementBase<PlatformTexture>() {

    override val idContainer = IdContainerStacks.local.peekNonNull<PlatformTexture>()

    val textureProcessorQueue = IdContainerStacks.local.peekSingleNonNull<TextureProcessorQueue>()

    abstract val width: Int
    abstract val height: Int

    abstract val textureId: Long

    abstract fun set(bytes: ByteArray, colorSpace: ColorSpace = ColorSpace.RGB)
    abstract fun set(bytes: ByteBuffer, colorSpace: ColorSpace = ColorSpace.RGB)

    abstract fun setJpeg(bytes: ByteArray)
    abstract fun setJpeg(bytes: ByteBuffer)

    override fun draw() {
        ImGui.image(textureId, width.toFloat(), height.toFloat())
    }

    override fun restore() {
        throw UnsupportedOperationException("Cannot restore texture after it has been deleted")
    }

}

interface PlatformTextureFactory {
    fun create(width: Int, height: Int, bytes: ByteArray, colorSpace: ColorSpace = ColorSpace.RGB): PlatformTexture

    fun create(width: Int, height: Int, bytes: ByteBuffer, colorSpace: ColorSpace = ColorSpace.RGB): PlatformTexture

    fun create(resource: String): PlatformTexture

    fun createFromJpegBytes(bytes: ByteBuffer): PlatformTexture
}

enum class ColorSpace(val channels: Int) {
    RGB(3), RGBA(4), BGR(3), BGRA(4)
}