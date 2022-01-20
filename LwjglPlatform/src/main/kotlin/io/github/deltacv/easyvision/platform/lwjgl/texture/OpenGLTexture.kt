package io.github.deltacv.easyvision.platform.lwjgl.texture

import io.github.deltacv.easyvision.platform.PlatformTexture
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

data class OpenGLTexture(
    override val id: Int,
    override val width: Int,
    override val height: Int
) : PlatformTexture {

    override fun set(bytes: ByteArray) {
        glBindTexture(GL_TEXTURE_2D, id)

        val buffer = MemoryUtil.memAlloc(bytes.size)
        buffer.put(bytes)
        buffer.flip()

        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGB, GL_UNSIGNED_BYTE, buffer)

        MemoryUtil.memFree(buffer)

        glBindTexture(GL_TEXTURE_2D, 0)
    }

    override fun delete() {
        glDeleteTextures(id)
    }


}