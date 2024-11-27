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

package io.github.deltacv.papervision.platform.lwjgl.texture

import io.github.deltacv.papervision.platform.ColorSpace
import io.github.deltacv.papervision.platform.PlatformTexture
import io.github.deltacv.papervision.platform.lwjgl.texture.OpenGLTextureFactory.create
import org.lwjgl.opengl.GL12.*
import org.lwjgl.stb.STBImage.stbi_failure_reason
import org.lwjgl.stb.STBImage.stbi_image_free
import org.lwjgl.stb.STBImage.stbi_load_from_memory
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import kotlin.use

data class OpenGLTexture(
    override val textureId: Long,
    override val width: Int,
    override val height: Int
) : PlatformTexture() {

    override fun set(bytes: ByteArray, colorSpace: ColorSpace) {
        val expectedSize = width * height * colorSpace.channels
        if(expectedSize != bytes.size) {
            throw IllegalArgumentException("Buffer size does not match resolution (expected $expectedSize, got ${bytes.size}, channels: ${colorSpace.channels}, width: $width, height: $height)")
        }

        val buffer = MemoryUtil.memAlloc(bytes.size)

        try {
            buffer.put(bytes)
            buffer.flip()
            set(buffer, colorSpace)
        } finally {
            MemoryUtil.memFree(buffer) // Free the memory even if an exception occurs
        }
    }

    override fun set(bytes: ByteBuffer, colorSpace: ColorSpace) {
        val expectedSize = width * height * colorSpace.channels
        if(expectedSize != bytes.remaining()) {
            throw IllegalArgumentException("Buffer size does not match resolution (expected $expectedSize, got ${bytes.remaining()}, channels: ${colorSpace.channels}, width: $width, height: $height)")
        }

        glBindTexture(GL_TEXTURE_2D, textureId.toInt())

        val format = when(colorSpace) {
            ColorSpace.RGB -> GL_RGB
            ColorSpace.RGBA -> GL_RGBA
            ColorSpace.BGR -> GL_BGR
            ColorSpace.BGRA -> GL_BGRA
        }

        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, format, GL_UNSIGNED_BYTE, bytes)
        glBindTexture(GL_TEXTURE_2D, 0)
    }

    override fun setJpeg(bytes: ByteArray) {
        MemoryStack.stackPush().use {
            val buffer = it.malloc(bytes.size)
            buffer.put(bytes)
            buffer.flip()

            val comp = it.mallocInt(1)
            val w = it.mallocInt(1)
            val h = it.mallocInt(1)

            val img = stbi_load_from_memory(buffer, w, h, comp, 3)

            try {
                if(img == null) {
                    throw RuntimeException("Failed to load image due to ${stbi_failure_reason()}")
                }
                set(img, ColorSpace.RGB)
            } finally {
                if(img != null) {
                    stbi_image_free(img) // Ensure memory is freed
                }
            }
        }
    }

    override fun setJpeg(bytes: ByteBuffer) {
        MemoryStack.stackPush().use {
            bytes.flip()

            val comp = it.mallocInt(1)
            val w = it.mallocInt(1)
            val h = it.mallocInt(1)

            val img = stbi_load_from_memory(bytes, w, h, comp, 3)

            try {
                if(img == null) {
                    throw RuntimeException("Failed to load image due to ${stbi_failure_reason()}")
                }

                set(img, ColorSpace.RGB)
            } finally {
                if(img != null) {
                    stbi_image_free(img) // Ensure memory is freed
                }
            }
        }
    }

    override fun delete() {
        glDeleteTextures(textureId.toInt())
    }

}