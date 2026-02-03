/*
 * PaperVision
 * Copyright (C) 2026 Sebastian Erives, deltacv
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.deltacv.papervision.io

import io.github.deltacv.papervision.id.DrawableIdElementBase
import io.github.deltacv.papervision.id.container.IdContainer
import io.github.deltacv.papervision.id.container.IdContainerStacks
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

/**
 * TextureProcessorQueue: Handles queuing, decoding, and creation of textures.
 *
 * Responsibilities:
 *  - Accept raw or JPEG texture data (sync or async)
 *  - Reuse byte[] buffers to reduce allocations
 *  - Decompress JPEGs asynchronously via MackJPEG when available
 *  - Create or update PlatformTexture instances on the render thread via draw()
 *
 * The implementation intentionally mirrors the original behavior but is organized
 * for clarity and annotated with inline comments inside function bodies.
 */
class TextureProcessorQueue(
    val textureFactory: PlatformTextureFactory
) : DrawableIdElementBase<TextureProcessorQueue>() {

    companion object {
        const val REUSABLE_BUFFER_QUEUE_SIZE = 15
        private val logger by loggerFor<TextureProcessorQueue>()
    }

    // Thread pool for JPEG decompression. Daemon threads so they don't block JVM shutdown.
    private val jpegWorkers: ExecutorService = Executors.newFixedThreadPool(5) { r ->
        Thread(r).apply {
            isDaemon = true
            name = "JPEG-Decomp-Worker-$id"
        }
    }

    // Pool that maintains reusable byte[] buffers to avoid frequent GC pressure.
    private val bufferPool = ReusableBufferPool(REUSABLE_BUFFER_QUEUE_SIZE)

    // Queue of pending textures to be processed on the render thread.
    private val queuedTextures = ArrayBlockingQueue<FutureTexture>(REUSABLE_BUFFER_QUEUE_SIZE)

    // Map of current textures by ID.
    private val textures = mutableMapOf<Int, PlatformTexture>()

    // ------------------------ Render/update loop ------------------------

    override fun draw() {
        // Determine how many textures we will attempt to process this frame.
        // Using queuedTextures.size ensures we process the elements that existed
        // at the start of the draw call (avoid unbounded processing if producers keep adding).
        val currentQueueSize = queuedTextures.size

        repeat(currentQueueSize) {
            val future = queuedTextures.poll() ?: return@repeat

            try {
                // Try to update an existing texture in-place when dimensions match.
                // If updated, we don't need to create a new texture object.
                val updated = processExistingTexture(future)
                if (updated) return@repeat

                // If not updated, create a new texture entry (validate id first).
                createNewTexture(future)
            } catch (e: Exception) {
                // Swallow exceptions per original behavior but log for diagnosis.
                logger.error("Error processing texture", e)
            } finally {
                // Return the buffer used to hold pixel data back to the pool.
                returnReusableBuffer(future.data)
            }
        }
    }

    // Attempt to update an existing texture with new bytes.
    // Returns true if the existing texture was updated and no further action is necessary.
    private fun processExistingTexture(future: FutureTexture): Boolean {
        // Lookup existing texture by ID.
        val existing = textures[future.id] ?: return false

        // If dimensions match we can update the texture data in-place.
        // The code differentiates JPEG source vs raw pixel data because textureFactory
        // might expose specialized setters for compressed data.
        if (existing.width == future.width && existing.height == future.height) {
            if (future.jpeg) {
                // Set compressed JPEG bytes (if supported by platform).
                existing.setJpeg(future.data)
            } else {
                // Set raw pixel bytes and indicate color space.
                existing.set(future.data, future.colorSpace)
            }
            return true
        }

        // If dimensions differ, delete the old texture so we can recreate it.
        existing.delete()
        return false
    }

    // Create a new PlatformTexture based on the future's payload and store it in the map.
    private fun createNewTexture(future: FutureTexture) {
        // Ensure ID validity — negative IDs are not allowed per original implementation.
        require(future.id >= 0) { "ID of new texture must be positive !" }

        val newTex: PlatformTexture = if (future.jpeg) {
            // When the incoming payload is labeled as JPEG, create from compressed bytes.
            textureFactory.createFromJpegBytes(ByteBuffer.wrap(future.data))
        } else {
            // Otherwise create from raw pixel data and color space.
            textureFactory.create(future.width, future.height, future.data, future.colorSpace)
        }

        // Store or replace the texture for this ID.
        textures[future.id] = newTex
    }

    // ------------------------ Public API: offering textures ------------------------

    fun offerJpeg(
        id: Int,
        width: Int,
        height: Int,
        data: ByteArray,
        memoryBehavior: MemoryBehavior = MemoryBehavior.ALLOCATE_WHEN_EXHAUSTED
    ) = offer(id, width, height, data, jpeg = true, memoryBehavior = memoryBehavior)

    fun offerJpeg(
        id: Int,
        width: Int,
        height: Int,
        data: ByteBuffer,
        memoryBehavior: MemoryBehavior = MemoryBehavior.ALLOCATE_WHEN_EXHAUSTED
    ) = offer(id, width, height, data, jpeg = true, memoryBehavior = memoryBehavior)

    /**
     * Asynchronously decompress JPEG and enqueue raw pixels for the render thread.
     * If MackJPEG backend is not available we fall back to the synchronous path.
     */
    fun offerJpegAsync(
        // Asynchronous JPEG decompression entry point
        id: Int,
        width: Int,
        height: Int,
        data: ByteArray,
        dataOffset: Int = 0,
        memoryBehavior: MemoryBehavior = MemoryBehavior.ALLOCATE_WHEN_EXHAUSTED
    ) {
        // Check whether we have a JPEG backend; if not, fallback to the synchronous offer.
        val backend = MackJPEG.getSupportedBackend()
        if (backend == null) {
            offerJpeg(id, width, height, data, memoryBehavior)
            return
        }

        // Submit decompression work to the worker pool.
        jpegWorkers.submit {
            val decompressor = backend.makeDecompressor() ?: return@submit

            decompressor.use {
                // If the JPEG data contains an offset we may need to copy the payload
                // into a reusable buffer that meets the decompressor's expectations.
                val offsetData = prepareOffsetData(data, dataOffset, memoryBehavior) ?: return@submit

                try {
                    // Provide the JPEG bytes to the decompressor.
                    decompressor.setJPEG(offsetData, offsetData.size)

                    // Allocate an output buffer sized for RGB pixels.
                    val outputSize = decompressor.decodedWidth * decompressor.decodedHeight * 3
                    val buffer = getOrCreateReusableBuffer(outputSize, memoryBehavior) ?: return@submit

                    try {
                        // Decompress into the provided buffer. If decompression fails we
                        // log and recycle the buffer.
                        decompressor.decompress(buffer, PixelFormat.RGB)
                    } catch (e: Exception) {
                        logger.warn("Failed to decompress JPEG #$id", e)
                        returnReusableBuffer(buffer)
                        return@use
                    }

                    // Enqueue the decompressed RGB buffer as a raw texture (jpeg=false).
                    offerBuffer(
                        id,
                        decompressor.decodedWidth,
                        decompressor.decodedHeight,
                        buffer,
                        ColorSpace.RGB,
                        jpeg = false
                    )
                } finally {
                    // If we created a copied offsetData buffer, return it to the pool now.
                    if (dataOffset != 0) returnReusableBuffer(offsetData)
                }
            }
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
        // Allocate or grab a reusable buffer large enough for the incoming data.
        val size = data.remaining()
        val buffer = getOrCreateReusableBuffer(size, memoryBehavior) ?: return

        // Copy ByteBuffer contents into the byte[] buffer.
        data.get(buffer)

        // Enqueue for processing on the render thread.
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
        // Copy the provided array into a pooled buffer and enqueue.
        val buffer = getOrCreateReusableBuffer(data.size, memoryBehavior) ?: return
        System.arraycopy(data, 0, buffer, 0, data.size)
        offerBuffer(id, width, height, buffer, colorSpace, jpeg)
    }

    operator fun get(id: Int) = textures[id]

    fun clear() {
        // Clear queued items and delete all textures, then clear the buffer pool.
        queuedTextures.clear()
        textures.values.forEach { it.delete() }
        textures.clear()
        bufferPool.clear()
    }

    // ------------------------ Internal helpers ------------------------

    /**
     * Enqueues a FutureTexture. If the queue is full we evict the oldest entry and
     * return its buffer to the pool to ensure we always have room for new items.
     */
    private fun offerBuffer(
        id: Int,
        width: Int,
        height: Int,
        buffer: ByteArray,
        colorSpace: ColorSpace,
        jpeg: Boolean
    ) {
        // Synchronize to avoid concurrent modifications to the queue.
        synchronized(queuedTextures) {
            if (queuedTextures.remainingCapacity() == 0) {
                // Evict oldest queued item and recycle its buffer.
                val evicted = queuedTextures.poll()
                if (evicted != null) returnReusableBuffer(evicted.data)
            }

            // Offer the new texture; if offer fails silently, we should also recycle.
            val offered = queuedTextures.offer(FutureTexture(id, width, height, buffer, colorSpace, jpeg))
            if (!offered) {
                // If insertion failed for any reason, ensure buffer is returned to pool.
                returnReusableBuffer(buffer)
            }
        }
    }

    /**
     * Handles JPEG packets that contain a data offset. If offset is zero, returns
     * the original array. Otherwise, copies the offset content into a rounded
     * reusable buffer and returns that buffer.
     */
    private fun prepareOffsetData(
        data: ByteArray,
        offset: Int,
        memoryBehavior: MemoryBehavior
    ): ByteArray? {
        // If there's no offset we can use the original buffer directly without copying.
        if (offset == 0) return data

        // Compute copy length and round up to 4KiB increments to reduce fragmentation.
        val copyLength = data.size - offset
        val roundedLength = ((copyLength / 4096) + 1) * 4096

        // Acquire a reusable buffer for the rounded size.
        val dest = getOrCreateReusableBuffer(roundedLength, memoryBehavior) ?: return null

        // Copy only the meaningful bytes starting at the offset into dest.
        System.arraycopy(data, offset, dest, 0, copyLength)
        return dest
    }

    private fun returnReusableBuffer(buffer: ByteArray) = bufferPool.returnBuffer(buffer)

    private fun getOrCreateReusableBuffer(size: Int, memoryBehavior: MemoryBehavior) =
        bufferPool.getOrCreate(size, memoryBehavior)

    override val idContainer: IdContainer<TextureProcessorQueue>
            by lazy { IdContainerStacks.local.peekNonNull() }

    // Small data class to represent a queued texture operation.
    @Suppress("ArrayInDataClass")
    private data class FutureTexture(
        val id: Int,
        val width: Int,
        val height: Int,
        val data: ByteArray,
        val colorSpace: ColorSpace,
        val jpeg: Boolean
    )
}
