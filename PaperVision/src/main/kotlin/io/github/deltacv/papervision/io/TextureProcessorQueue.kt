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
import io.github.deltacv.papervision.platform.ColorSpace
import io.github.deltacv.papervision.platform.PlatformTexture
import io.github.deltacv.papervision.platform.PlatformTextureFactory
import io.github.deltacv.papervision.util.ReusableBufferPool
import io.github.deltacv.papervision.util.ReusableBufferPool.MemoryBehavior
import io.github.deltacv.papervision.util.loggerFor
import org.deltacv.mackjpeg.MackJPEG
import org.deltacv.mackjpeg.PixelFormat
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TextureProcessorQueue(
    val textureFactory: PlatformTextureFactory
) : DrawableIdElementBase<TextureProcessorQueue>() {

    companion object {
        const val REUSABLE_BUFFER_QUEUE_SIZE = 15

        val logger by loggerFor<TextureProcessorQueue>()
    }

    val jpegWorkers: ExecutorService = Executors.newFixedThreadPool(5) {
        val thread = Thread(it)
        thread.isDaemon = true
        thread.name = "JPEG-Decomp-Worker-${thread.id}"

        thread
    }

    private val bufferPool = ReusableBufferPool(REUSABLE_BUFFER_QUEUE_SIZE)

    private val queuedTextures = ArrayBlockingQueue<FutureTexture>(REUSABLE_BUFFER_QUEUE_SIZE)
    private val textures = mutableMapOf<Int, PlatformTexture>()

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
        offer(id, width, height, data, jpeg = true, memoryBehavior = memoryBehavior)

    fun offerJpeg(
        id: Int, width: Int, height: Int, data: ByteBuffer,
        memoryBehavior: MemoryBehavior = MemoryBehavior.ALLOCATE_WHEN_EXHAUSTED
    ) =
        offer(id, width, height, data, jpeg = true, memoryBehavior = memoryBehavior)

    fun offerJpegAsync(
        id: Int, width: Int, height: Int, data: ByteArray, dataOffset: Int = 0,
        memoryBehavior: MemoryBehavior = MemoryBehavior.ALLOCATE_WHEN_EXHAUSTED
    ) {
        if (MackJPEG.getSupportedBackend() != null) {
            jpegWorkers.submit {
                val decompressor = MackJPEG.getSupportedBackend()?.makeDecompressor() ?: return@submit

                decompressor.use {
                    val offsetData = if(dataOffset != 0) {
                        // round in 5k intervals
                        val roundedLength = ((data.size - dataOffset) / 4096 + 1) * 4096

                        val offsetArray = getOrCreateReusableBuffer(roundedLength, memoryBehavior) ?: return@submit
                        System.arraycopy(data, dataOffset, offsetArray, 0, data.size - dataOffset)

                        offsetArray
                    } else data

                    try {
                        decompressor.setJPEG(offsetData, offsetData.size)

                        val buffer = getOrCreateReusableBuffer(
                            decompressor.decodedWidth * decompressor.decodedHeight * 3,
                            memoryBehavior
                        ) ?: return@submit

                        try {
                            decompressor.decompress(buffer, PixelFormat.RGB)
                        } catch (e: Exception) {
                            logger.warn("Failed to decompress JPEG #$id", e)
                            returnReusableBuffer(buffer)
                            return@use
                        }

                        offerBuffer(id, decompressor.decodedWidth, decompressor.decodedHeight, buffer, ColorSpace.RGB, jpeg = false)
                    } finally {
                        if(dataOffset != 0) {
                            returnReusableBuffer(offsetData)
                        }
                    }
                }
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
        bufferPool.returnBuffer(buffer)
    }

    private fun getOrCreateReusableBuffer(size: Int, memoryBehavior: MemoryBehavior) =
        bufferPool.getOrCreate(size, memoryBehavior)

    operator fun get(id: Int) = textures[id]

    fun clear() {
        queuedTextures.clear()
        textures.values.forEach { it.delete() }
        textures.clear()
        bufferPool.clear()
    }

    override val idElementContainer: IdElementContainer<TextureProcessorQueue>
        get() = IdElementContainerStack.localStack.peekNonNull()

    class FutureTexture(
        val id: Int,
        val width: Int,
        val height: Int,
        val data: ByteArray,
        val colorSpace: ColorSpace,
        val jpeg: Boolean
    )
}