package io.github.deltacv.easyvision.platform.lwjgl.texture

import io.github.deltacv.easyvision.platform.PlatformTextureFactory
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil

object OpenGLTextureFactory : PlatformTextureFactory {

    override fun create(width: Int, height: Int, bytes: ByteArray): OpenGLTexture {
        val id = glGenTextures()

        glBindTexture(GL_TEXTURE_2D, id)
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1)

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)

        val buffer = MemoryUtil.memAlloc(bytes.size)
        buffer.put(bytes)
        buffer.flip()

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, buffer)

        MemoryUtil.memFree(buffer)

        glBindTexture(GL_TEXTURE_2D, 0)

        return OpenGLTexture(id, width, height)
    }

}