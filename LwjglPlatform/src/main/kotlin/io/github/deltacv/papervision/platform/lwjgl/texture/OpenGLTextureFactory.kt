package io.github.deltacv.papervision.platform.lwjgl.texture

import io.github.deltacv.papervision.platform.ColorSpace
import io.github.deltacv.papervision.platform.PlatformTextureFactory
import io.github.deltacv.papervision.platform.lwjgl.util.loadImageFromResource
import org.lwjgl.opengl.GL12.*
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

object OpenGLTextureFactory : PlatformTextureFactory {

    override fun create(width: Int, height: Int, bytes: ByteArray, colorSpace: ColorSpace): OpenGLTexture {
        val buffer = MemoryUtil.memAlloc(bytes.size)
        buffer.put(bytes)
        buffer.flip()

        val texture = try {
            create(width, height, buffer, colorSpace)
        } finally {
            // make sure buffer is always freed
            MemoryUtil.memFree(buffer)
        }

        return texture
    }

    override fun create(width: Int, height: Int, bytes: ByteBuffer, colorSpace: ColorSpace): OpenGLTexture {
        val id = glGenTextures()

        val expectedSize = width * height * colorSpace.channels
        if(expectedSize != bytes.remaining()) {
            throw IllegalArgumentException("Buffer size does not match resolution (expected $expectedSize, got ${bytes.remaining()}, channels: ${colorSpace.channels}, width: $width, height: $height)")
        }

        glBindTexture(GL_TEXTURE_2D, id)
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1)

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)

        val format = when(colorSpace) {
            ColorSpace.RGB -> GL_RGB
            ColorSpace.RGBA -> GL_RGBA
            ColorSpace.BGR -> GL_BGR
            ColorSpace.BGRA -> GL_BGRA
        }

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height, 0, format, GL_UNSIGNED_BYTE, bytes)

        glBindTexture(GL_TEXTURE_2D, 0)

        return OpenGLTexture(id.toLong(), width, height)
    }

    override fun create(resource: String): OpenGLTexture {
        val image = loadImageFromResource(resource)

        return create(image.width, image.height, image.buffer)
    }

}