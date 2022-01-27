package io.github.deltacv.easyvision.platform.lwjgl.texture

import io.github.deltacv.easyvision.platform.ColorSpace
import io.github.deltacv.easyvision.platform.PlatformTexture
import org.lwjgl.opengl.GL12.*
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

data class OpenGLTexture(
    override val textureId: Int,
    override val width: Int,
    override val height: Int
) : PlatformTexture() {

    override fun set(bytes: ByteArray, colorSpace: ColorSpace) {
        val buffer = MemoryUtil.memAlloc(bytes.size)
        buffer.put(bytes)
        buffer.flip()

        try {
            set(buffer, colorSpace)
        } finally {
            MemoryUtil.memFree(buffer)
        }
    }

    override fun set(bytes: ByteBuffer, colorSpace: ColorSpace) {
        glBindTexture(GL_TEXTURE_2D, textureId)

        val format = when(colorSpace) {
            ColorSpace.RGB -> GL_RGB
            ColorSpace.RGBA -> GL_RGBA
            ColorSpace.BGR -> GL_BGR
            ColorSpace.BGRA -> GL_BGRA
        }

        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, format, GL_UNSIGNED_BYTE, bytes)
        glBindTexture(GL_TEXTURE_2D, 0)
    }

    override fun delete() {
        glDeleteTextures(textureId)
    }


}