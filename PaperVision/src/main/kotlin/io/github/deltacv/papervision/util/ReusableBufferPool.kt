/*
 * PaperVision
 * Copyright (C) 2025 Sebastian Erives, deltacv

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

package io.github.deltacv.papervision.util

import org.slf4j.LoggerFactory
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap

/**
 * Utility class for managing reusable byte array buffers of different sizes.
 *
 * Each buffer size has its own pool with a fixed capacity.
 */
class ReusableBufferPool(
    private val poolSize: Int = 15
) {

    enum class MemoryBehavior {
        ALLOCATE_WHEN_EXHAUSTED,
        DISCARD_WHEN_EXHAUSTED,
        EXCEPTION_WHEN_EXHAUSTED
    }

    private val logger = LoggerFactory.getLogger(ReusableBufferPool::class.java)

    private val reusableBuffers = ConcurrentHashMap<Int, ArrayBlockingQueue<ByteArray>>()

    /**
     * Gets or creates a buffer of the given size.
     */
    fun getOrCreate(size: Int, behavior: MemoryBehavior): ByteArray? {
        synchronized(reusableBuffers) {
            if (reusableBuffers[size] == null) {
                val queue = ArrayBlockingQueue<ByteArray>(poolSize)
                repeat(poolSize) { queue.offer(ByteArray(size)) }
                reusableBuffers[size] = queue
            }

            val queue = reusableBuffers[size]!!
            return queue.poll() ?: run {
                when (behavior) {
                    MemoryBehavior.ALLOCATE_WHEN_EXHAUSTED -> ByteArray(size)
                    MemoryBehavior.DISCARD_WHEN_EXHAUSTED -> null
                    MemoryBehavior.EXCEPTION_WHEN_EXHAUSTED ->
                        throw IllegalStateException("Buffer pool for size $size is empty")
                }
            }
        }
    }

    /**
     * Returns a buffer back to the pool.
     */
    fun returnBuffer(buffer: ByteArray) {
        synchronized(reusableBuffers) {
            reusableBuffers[buffer.size]?.offer(buffer)
                ?: logger.warn("Buffer pool for size ${buffer.size} is null")
        }
    }

    /**
     * Clears all buffer pools.
     */
    fun clear() {
        synchronized(reusableBuffers) {
            reusableBuffers.values.forEach { it.clear() }
        }
    }
}
