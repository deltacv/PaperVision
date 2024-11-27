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

import io.github.deltacv.papervision.platform.ColorSpace
import io.github.deltacv.papervision.platform.PlatformTexture
import io.github.deltacv.papervision.platform.PlatformTextureFactory
import io.github.deltacv.papervision.util.event.PaperVisionEventHandler
import io.github.deltacv.papervision.util.loggerForThis
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue

class TextureProcessorQueue(
    val textureFactory: PlatformTextureFactory
) {

    enum class MemoryBehavior {
        ALLOCATE_WHEN_EXHAUSTED,
        DISCARD_WHEN_EXHAUSTED,
        EXCEPTION_WHEN_EXHAUSTED
    }

    companion object {
        const val REUSABLE_BUFFER_QUEUE_SIZE = 60
    }

    val logger by loggerForThis()

    private val reusableBuffers = mutableMapOf<Int, ArrayBlockingQueue<ByteArray>>()

    private val queuedTextures = ArrayBlockingQueue<FutureTexture>(5)
    private val textures = mutableMapOf<Int, PlatformTexture>()

    private var currentHandler: PaperVisionEventHandler? = null

    fun subscribeTo(handler: PaperVisionEventHandler) {
        handler {
            if (currentHandler != handler) { // can only subscribe to one event loop at a time
                it.removeThis()
                return@handler
            }

            while (queuedTextures.isNotEmpty()) {
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

                    if (shouldContinue) continue

                    textures[futureTex.id] = if (futureTex.jpeg) {
                        textureFactory.createFromJpegBytes(ByteBuffer.wrap(futureTex.data))
                    } else {
                        textureFactory.create(
                            futureTex.width, futureTex.height, futureTex.data, futureTex.colorSpace
                        )
                    }
                } catch (e: Exception) {
                    logger.error("Error processing texture: ${e.message}", e)
                } finally {
                    returnReusableBuffer(futureTex.data)
                }
            }
        }

        currentHandler = handler
    }

    fun offerJpeg(id: Int, width: Int, height: Int, data: ByteArray,
                  memoryBehavior: MemoryBehavior = MemoryBehavior.ALLOCATE_WHEN_EXHAUSTED) =
        offer(id, width, height, data, jpeg = true)

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

        synchronized(queuedTextures) {
            if (queuedTextures.remainingCapacity() == 0) {
                queuedTextures.poll()
            }
            queuedTextures.offer(FutureTexture(id, width, height, buffer, colorSpace, jpeg))
        }
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

    operator fun get(id: Int) = textures[id]

    fun clear() {
        queuedTextures.clear()
        textures.values.forEach { it.delete() }
        textures.clear()

        synchronized(reusableBuffers) {
            reusableBuffers.values.forEach { it.clear() }
        }
    }

    data class FutureTexture(
        val id: Int,
        val width: Int,
        val height: Int,
        val data: ByteArray,
        val colorSpace: ColorSpace,
        val jpeg: Boolean
    )
}