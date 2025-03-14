package io.github.deltacv.papervision.platform.lwjgl.texture

import io.github.deltacv.papervision.platform.ColorSpace
import io.github.deltacv.papervision.platform.PlatformTexture
import org.lwjgl.opengl.GL12.*
import org.lwjgl.stb.STBImage.stbi_failure_reason
import org.lwjgl.stb.STBImage.stbi_image_free
import org.lwjgl.stb.STBImage.stbi_load_from_memory
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger
import kotlin.use

data class OpenGLTexture(
    override val textureId: Long,
    override val width: Int,
    override val height: Int
) : PlatformTexture() {

    private val activeAsyncs = AtomicInteger(0)
    private var queueId: Int? = null

    @Synchronized
    private fun ensureQueueId(): Int {
        return queueId ?: run {
            // Get existing ID or create a new one
            val id = textureProcessorQueue.getQIdOf(this) ?: textureProcessorQueue.assignQIdTo(this)
            queueId = id
            id
        }
    }

    override fun set(bytes: ByteArray, colorSpace: ColorSpace) {
        val expectedSize = width * height * colorSpace.channels
        if (expectedSize != bytes.size) {
            throw IllegalArgumentException("Buffer size does not match resolution (expected $expectedSize, got ${bytes.size}, channels: ${colorSpace.channels}, width: $width, height: $height)")
        }

        val buffer = MemoryUtil.memAlloc(bytes.size)
        try {
            buffer.put(bytes).flip()
            set(buffer, colorSpace)
        } finally {
            MemoryUtil.memFree(buffer)
        }
    }

    override fun set(bytes: ByteBuffer, colorSpace: ColorSpace) {
        val expectedSize = width * height * colorSpace.channels
        if (expectedSize != bytes.remaining()) {
            throw IllegalArgumentException("Buffer size does not match resolution (expected $expectedSize, got ${bytes.remaining()}, channels: ${colorSpace.channels}, width: $width, height: $height)")
        }

        glBindTexture(GL_TEXTURE_2D, textureId.toInt())

        val format = when (colorSpace) {
            ColorSpace.RGB -> GL_RGB
            ColorSpace.RGBA -> GL_RGBA
            ColorSpace.BGR -> GL_BGR
            ColorSpace.BGRA -> GL_BGRA
        }

        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, format, GL_UNSIGNED_BYTE, bytes)
        glBindTexture(GL_TEXTURE_2D, 0)
    }

    private fun loadJpeg(bytes: ByteBuffer, callback: (ByteBuffer) -> Unit) {
        MemoryStack.stackPush().use {
            val comp = it.mallocInt(1)
            val w = it.mallocInt(1)
            val h = it.mallocInt(1)
            val img = stbi_load_from_memory(bytes, w, h, comp, 3)

            if (img == null) {
                throw RuntimeException("Failed to load image due to ${stbi_failure_reason()}")
            }

            try {
                callback(img)
            } finally {
                stbi_image_free(img)
            }
        }
    }

    override fun setJpeg(bytes: ByteArray) {
        val buffer = MemoryUtil.memAlloc(bytes.size).put(bytes).flip()
        try {
            loadJpeg(buffer) { set(it, ColorSpace.RGB) }
        } finally {
            MemoryUtil.memFree(buffer)
        }
    }

    override fun setJpeg(bytes: ByteBuffer) {
        loadJpeg(bytes) { set(it, ColorSpace.RGB) }
    }

    override fun setJpegAsync(bytes: ByteArray) {
        // Check if we're already processing too many async requests
        if (activeAsyncs.get() >= 4) {
            return
        }

        activeAsyncs.incrementAndGet()

        // Create a safe copy of the bytes that the worker thread can use
        val safeCopy = ByteArray(bytes.size)
        System.arraycopy(bytes, 0, safeCopy, 0, bytes.size)

        asyncWorkers.execute {
            try {
                val buffer = MemoryUtil.memAlloc(safeCopy.size).put(safeCopy).flip()
                try {
                    loadJpeg(buffer) { img ->
                        val qid = ensureQueueId()
                        textureProcessorQueue.offer(qid, width, height, img, ColorSpace.RGB)
                    }
                } finally {
                    MemoryUtil.memFree(buffer)
                }
            } catch (e: Exception) {
                throw RuntimeException("Error in async JPEG processing", e)
            } finally {
                activeAsyncs.decrementAndGet()
            }
        }
    }

    override fun setJpegAsync(bytes: ByteBuffer) {
        if (activeAsyncs.get() >= 4) {
            return
        }

        activeAsyncs.incrementAndGet()

        // Create a copy that will be safe for the worker thread
        val size = bytes.remaining()
        val safeCopy = ByteArray(size)
        bytes.get(safeCopy)
        bytes.position(bytes.position() - size) // Reset position

        asyncWorkers.execute {
            try {
                val buffer = MemoryUtil.memAlloc(safeCopy.size).put(safeCopy).flip()
                try {
                    loadJpeg(buffer) { img ->
                        val qid = ensureQueueId()
                        textureProcessorQueue.offer(qid, width, height, img, ColorSpace.RGB)
                    }
                } finally {
                    MemoryUtil.memFree(buffer)
                }
            } catch (e: Exception) {
                throw RuntimeException("Error in async JPEG processing", e)
            } finally {
                activeAsyncs.decrementAndGet()
            }
        }
    }

    override fun delete() {
        glDeleteTextures(textureId.toInt())
    }
}