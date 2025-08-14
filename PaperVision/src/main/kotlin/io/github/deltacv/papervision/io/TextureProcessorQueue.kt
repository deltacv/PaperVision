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
package io.github.deltacv.papervision.io

import io.github.deltacv.papervision.id.DrawableIdElementBase
import io.github.deltacv.papervision.id.IdElementContainer
import io.github.deltacv.papervision.id.IdElementContainerStack
import io.github.deltacv.papervision.io.turbojpeg.TJLoader
import io.github.deltacv.papervision.platform.ColorSpace
import io.github.deltacv.papervision.platform.PlatformTexture
import io.github.deltacv.papervision.platform.PlatformTextureFactory
import io.github.deltacv.papervision.util.loggerFor
import org.libjpegturbo.turbojpeg.TJ
import org.libjpegturbo.turbojpeg.TJDecompressor
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class TextureProcessorQueue(
    val textureFactory: PlatformTextureFactory
) : DrawableIdElementBase<TextureProcessorQueue>() {

    enum class MemoryBehavior {
        ALLOCATE_WHEN_EXHAUSTED,
        DISCARD_WHEN_EXHAUSTED,
        EXCEPTION_WHEN_EXHAUSTED
    }

    companion object {
        const val REUSABLE_BUFFER_QUEUE_SIZE = 15

        val logger by loggerFor<TextureProcessorQueue>()

        init {
            try {
                TJLoader.load()
            } catch (e: Exception) {
                logger.warn("Failed to load TurboJPEG, there will be reduced performance", e)
            }
        }
    }

    val jpegWorkers = Executors.newFixedThreadPool(5)

    private val reusableBuffers = ConcurrentHashMap<Int, ArrayBlockingQueue<ByteArray>>()

    private val queuedTextures = ArrayBlockingQueue<FutureTexture>(REUSABLE_BUFFER_QUEUE_SIZE)
    private val textures = mutableMapOf<Int, PlatformTexture>()

    val idCache = mutableMapOf<PlatformTexture, Int>()

    @Synchronized
    override fun draw() {
        val currentQueueSize = queuedTextures.size

        repeat(currentQueueSize) {
            val futureTex = queuedTextures.poll()

            try {
                var shouldContinue = false

                textures[futureTex.id]?.let { existingTex ->
                    if (existingTex.width == futureTex.width && existingTex.height == futureTex.height) {
                        if (futureTex.jpeg) {
                            existingTex.setJpeg(futureTex.data)
                        } else {
                            existingTex.set(futureTex.data, futureTex.colorSpace)
                        }
                        shouldContinue = true
                    } else {
                        existingTex.delete()
                    }
                }

                if (shouldContinue) return@repeat

                if (futureTex.id < 0) {
                    throw IllegalArgumentException("ID of new texture must be positive !")
                }

                textures[futureTex.id] = if (futureTex.jpeg) {
                    textureFactory.createFromJpegBytes(ByteBuffer.wrap(futureTex.data))
                } else {
                    textureFactory.create(
                        futureTex.width, futureTex.height, futureTex.data, futureTex.colorSpace
                    )
                }
            } catch (e: Exception) {
                logger.error("Error processing texture", e)
            } finally {
                returnReusableBuffer(futureTex.data)
            }
        }
    }

    fun offerJpeg(
        id: Int, width: Int, height: Int, data: ByteArray,
        memoryBehavior: MemoryBehavior = MemoryBehavior.ALLOCATE_WHEN_EXHAUSTED
    ) =
        offer(id, width, height, data, jpeg = true)

    fun offerJpeg(
        id: Int, width: Int, height: Int, data: ByteBuffer,
        memoryBehavior: MemoryBehavior = MemoryBehavior.ALLOCATE_WHEN_EXHAUSTED
    ) =
        offer(id, width, height, data, jpeg = true)

    fun offerJpegAsync(
        id: Int, width: Int, height: Int, data: ByteArray,
        memoryBehavior: MemoryBehavior = MemoryBehavior.ALLOCATE_WHEN_EXHAUSTED
    ) {
        if (TJLoader.isLoaded) {
            jpegWorkers.submit {
                val buffer = getOrCreateReusableBuffer(width * height * 3, memoryBehavior) ?: return@submit
                val decompressor = TJDecompressor()

                decompressor.setJPEGImage(data, data.size)
                decompressor.decompress(buffer, width, 0, height, TJ.PF_RGB, TJ.FLAG_FASTDCT)

                offerBuffer(id, width, height, buffer, ColorSpace.RGB, jpeg = false)

                decompressor.close()
            }
        } else {
            // fallback to offerJpeg
            offerJpeg(id, width, height, data, memoryBehavior)
        }
    }

    private fun offerBuffer(
        id: Int,
        width: Int,
        height: Int,
        buffer: ByteArray,
        colorSpace: ColorSpace,
        jpeg: Boolean
    ) {
        synchronized(queuedTextures) {
            if (queuedTextures.remainingCapacity() == 0) {
                returnReusableBuffer(queuedTextures.poll().data)
            }

            queuedTextures.offer(FutureTexture(id, width, height, buffer, colorSpace, jpeg))
        }
    }

    fun offer(
        id: Int,
        width: Int,
        height: Int,
        data: ByteBuffer,
        colorSpace: ColorSpace = ColorSpace.RGB,
        jpeg: Boolean = false,
        memoryBehavior: MemoryBehavior = MemoryBehavior.ALLOCATE_WHEN_EXHAUSTED
    ) {
        val size = data.remaining()
        val buffer = getOrCreateReusableBuffer(size, memoryBehavior) ?: return

        data.get(buffer) // memcpy hopefully?
        offerBuffer(id, width, height, buffer, colorSpace, jpeg)
    }

    fun offer(
        id: Int,
        width: Int,
        height: Int,
        data: ByteArray,
        colorSpace: ColorSpace = ColorSpace.RGB,
        jpeg: Boolean = false,
        memoryBehavior: MemoryBehavior = MemoryBehavior.ALLOCATE_WHEN_EXHAUSTED
    ) {
        val size = data.size
        val buffer = getOrCreateReusableBuffer(size, memoryBehavior) ?: return

        System.arraycopy(data, 0, buffer, 0, size)
        offerBuffer(id, width, height, buffer, colorSpace, jpeg)
    }

    private fun returnReusableBuffer(buffer: ByteArray) {
        synchronized(reusableBuffers) {
            reusableBuffers[buffer.size]?.offer(buffer)
                ?: logger.warn("Buffer pool for size ${buffer.size} is null")
        }
    }

    private fun getOrCreateReusableBuffer(size: Int, memoryBehavior: MemoryBehavior): ByteArray? {
        synchronized(reusableBuffers) {
            if (reusableBuffers[size] == null) {
                val queue = ArrayBlockingQueue<ByteArray>(REUSABLE_BUFFER_QUEUE_SIZE)

                repeat(REUSABLE_BUFFER_QUEUE_SIZE) {
                    queue.offer(ByteArray(size))
                }

                reusableBuffers[size] = queue
            }

            val queue = reusableBuffers[size]!!
            return queue.poll() ?: run {
                when (memoryBehavior) {
                    MemoryBehavior.ALLOCATE_WHEN_EXHAUSTED -> ByteArray(size)
                    MemoryBehavior.DISCARD_WHEN_EXHAUSTED -> null
                    MemoryBehavior.EXCEPTION_WHEN_EXHAUSTED -> throw IllegalStateException("Buffer pool for size $size is empty")
                }
            }
        }
    }

    fun getQIdOf(texture: PlatformTexture) = idCache[texture]?.also { return it }
        ?: textures.entries.firstOrNull { it.value == texture }?.key?.also { idCache[texture] = it }

    /**
     * Assigns an ID to a texture. This is used to keep track of textures that are not initially part of this queue.
     * @param texture The texture to assign the ID to
     * @param id The ID to assign to the texture. Must be negative to avoid conflicts with existing textures.
     */
    fun assignQIdTo(texture: PlatformTexture): Int {
        if (getQIdOf(texture) != null) throw IllegalArgumentException("Texture already has a qID assigned")

        textures[-texture.id] = texture
        idCache[texture] = -texture.id - 1

        logger.info("Assigned qID ${-texture.id - 1} to texture $texture")

        return -texture.id - 1
    }

    operator fun get(id: Int) = textures[id]

    fun clear() {
        queuedTextures.clear()
        textures.values.forEach { it.delete() }
        textures.clear()

        synchronized(reusableBuffers) {
            reusableBuffers.values.forEach { it.clear() }
        }
    }

    override val idElementContainer: IdElementContainer<TextureProcessorQueue>
        get() = IdElementContainerStack.threadStack.peekNonNull()

    data class FutureTexture(
        val id: Int,
        val width: Int,
        val height: Int,
        val data: ByteArray,
        val colorSpace: ColorSpace,
        val jpeg: Boolean
    )
}