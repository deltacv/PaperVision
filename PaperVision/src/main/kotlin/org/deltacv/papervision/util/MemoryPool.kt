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

package org.deltacv.papervision.util

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory

/**
 * A tiered byte-array pool that avoids per-exact-size pool fragmentation.
 *
 * Instead of maintaining a separate pool for every unique byte-array size seen
 * (which causes unbounded memory growth during resolution switches), this pool
 * organizes buffers into a small set of power-of-two size tiers.  A request for
 * N bytes is satisfied from the smallest tier whose capacity is >= N.  This means
 * a buffer retrieved from the pool may be slightly larger than requested, but no
 * pool slot is ever wasted on a size that differs by only a single byte.
 *
 * Each tier holds at most [tierCapacity] live buffers.  Once a tier is full,
 * excess returns are simply discarded and left to the GC.
 *
 * Thread safety is achieved with two lock levels:
 *  - [tiersLock] serialises structural mutations of the [tiers] list (add + sort).
 *  - Each tier carries its own [TierEntry.mutex] that guards only that tier's deque,
 *    so threads working on different tiers never block each other.
 */
class MemoryPool(
    private val tierCapacity: Int = 8
) {
    enum class MemoryBehavior { ALLOCATE_WHEN_EXHAUSTED, DISCARD_WHEN_EXHAUSTED }
    enum class AllocationMode { EXACT, POWER_OF_TWO }

    private val logger = LoggerFactory.getLogger(MemoryPool::class.java)

    private class TierEntry(val size: Int) {
        val deque  = ArrayDeque<ByteArray>()
        val mutex  = Mutex()
    }

    private val tiers      = mutableListOf<TierEntry>()
    private val tiersLock  = Mutex()

    /** Returns (and lazily creates) the tier that exactly matches [size]. */
    private fun tierFor(size: Int): TierEntry = runBlocking {
        tiersLock.withLock {
            tiers.firstOrNull { it.size == size }
                ?: TierEntry(size).also { entry ->
                    tiers.add(entry)
                    tiers.sortBy { it.size }
                }
        }
    }

    /**
     * Returns a buffer whose size is >= [size], taken from the smallest fitting tier.
     * Null is returned only when `behavior == DISCARD_WHEN_EXHAUSTED` and the pool is empty.
     */
    fun getOrCreate(
        size: Int,
        behavior: MemoryBehavior = MemoryBehavior.ALLOCATE_WHEN_EXHAUSTED,
        mode: AllocationMode = AllocationMode.POWER_OF_TWO
    ): ByteArray? {
        val tierSize = if (mode == AllocationMode.POWER_OF_TWO) {
            var s = 1
            while (s < size) s = s shl 1
            s
        } else {
            size
        }

        val entry = tierFor(tierSize)

        // Pop under the per-tier mutex; allocate outside to avoid holding the lock during GC.
        val pooled = runBlocking { entry.mutex.withLock { entry.deque.removeLastOrNull() } }
        return pooled ?: when (behavior) {
            MemoryBehavior.ALLOCATE_WHEN_EXHAUSTED -> ByteArray(tierSize)
            MemoryBehavior.DISCARD_WHEN_EXHAUSTED  -> null
        }
    }

    /**
     * Returns a buffer to its natural tier.  The buffer's size must match an existing
     * tier exactly (i.e. be a power-of-two when the pool is used in POWER_OF_TWO mode);
     * buffers that don't match any tier are silently discarded rather than corrupting state.
     */
    fun returnBuffer(buffer: ByteArray) {
        val entry = runBlocking { tiersLock.withLock { tiers.firstOrNull { it.size == buffer.size } } } ?: return
        runBlocking {
            entry.mutex.withLock {
                if (entry.deque.size < tierCapacity) {
                    entry.deque.addLast(buffer)
                }
                // If tier is full, discard — the GC will handle it.
            }
        }
    }

    /** Drop all pooled buffers, allowing them to be GC'd immediately. */
    fun clear() {
        // Snapshot the tier list under the structural lock, then clear each tier
        // under its own mutex so in-flight getOrCreate / returnBuffer calls stay safe.
        val snapshot = runBlocking { tiersLock.withLock { tiers.toList() } }
        snapshot.forEach { entry -> runBlocking { entry.mutex.withLock { entry.deque.clear() } } }
    }
}
