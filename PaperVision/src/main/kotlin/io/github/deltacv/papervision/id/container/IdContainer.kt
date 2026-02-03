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

package io.github.deltacv.papervision.id.container

import io.github.deltacv.papervision.id.IdElement
import kotlin.math.max

/**
 * Single-element container that reuses IdElementContainer behavior but enforces single-element constraint.
 */
class SingleIdContainer<T : IdElement> : IdContainer<T>() {

    // ------------------------
    // NON-LAZY VERSION (new)
    // ------------------------
    override fun requestId(element: T, id: Int): Int {
        if (e.any { it != null }) {
            throw IllegalStateException("This container can only have one element")
        }
        return super.requestId(element, id)
    }

    override fun requestIdLazy(element: T, id: Int): Lazy<Int> {
        if (e.any { it != null }) {
            throw IllegalStateException("This container can only have one element")
        }
        return super.requestIdLazy(element, id)
    }

    fun get(): T? = e.firstOrNull { it != null }
}


/**
 * Optimized IdElement container.
 *
 * Key improvements:
 * - `get(id)` does not allocate in hash mode (lookup-only).
 * - Migration to hash mode maps only existing assigned external ids -> internal ids.
 * - `reserveId` does not insert/shift; it ensures capacity and marks slot.
 * - `inmutable` is lazily refreshed (dirty flag) to avoid cloning on every insert.
 *
 * Public API kept same as previous implementation except get() semantics for missing hash ids.
 */
open class IdContainer<T : IdElement> : Iterable<T> {

    protected val e = ArrayList<T?>()
    var elements = ArrayList<T>()
        private set

    private var extToInt: MutableMap<Int, Int>? = null
    private var intToExt: MutableMap<Int, Int>? = null
    private var nextSequentialId = 0
    private var useHashMapping = false
    private val sparsityThreshold = 1000

    var stackPointer: Int = 1
    var stackPointerFollowing = true
        private set

    val size get() = e.size

    private var _inmutable: List<T> = elements.toList()
    private var inmutableDirty = false

    var inmutable: List<T>
        private set(value) { _inmutable = value; inmutableDirty = false }
        get() {
            if (inmutableDirty) {
                _inmutable = elements.toList()
                inmutableDirty = false
            }
            return _inmutable
        }

    private fun markInmutableDirty() {
        inmutableDirty = true
    }

    private fun movePointerToLast() {
        if (stackPointerFollowing) {
            stackPointer = e.size
        }
    }

    private fun shouldEnableHashMapping(requestedId: Int): Boolean {
        if (useHashMapping) return true
        if (requestedId < 0) return true
        val gapSize = requestedId - e.size
        return gapSize > sparsityThreshold
    }

    private fun resolveIdAllocating(externalId: Int): Int {
        if (!useHashMapping) {
            if (externalId < 0) {
                enableHashMappingAndMigrate()
            } else return externalId
        }

        if (extToInt == null || intToExt == null) {
            enableHashMappingAndMigrate()
        }

        return extToInt!!.getOrPut(externalId) {
            val internalId = nextSequentialId++
            intToExt!![internalId] = externalId
            internalId
        }
    }

    private fun resolveIdLookupOnly(externalId: Int): Int? {
        if (!useHashMapping)
            return if (externalId in 0 until e.size) externalId else null

        return extToInt?.get(externalId)
    }

    private fun enableHashMappingAndMigrate() {
        if (useHashMapping && extToInt != null && intToExt != null) return

        val existingCount = e.count { it != null }
        val initialCapacity = max(16, existingCount * 2)

        extToInt = HashMap(initialCapacity)
        intToExt = HashMap(initialCapacity)
        nextSequentialId = 0

        var maxAssigned = -1
        for (i in e.indices) {
            val elem = e[i]
            if (elem != null) {
                extToInt!![i] = i
                intToExt!![i] = i
                maxAssigned = max(maxAssigned, i)
            }
        }
        nextSequentialId = maxAssigned + 1
        useHashMapping = true
    }

    private fun externalizeId(internalId: Int): Int =
        intToExt?.get(internalId) ?: internalId


    // ============================================================
    // -------------   NON-LAZY PUBLIC API (new)   ----------------
    // ============================================================

    /**
     * NON-LAZY: Request id and immediately assign element.
     */
    open fun requestId(element: T, id: Int): Int {
        if (shouldEnableHashMapping(id)) {
            useHashMapping = true
        }

        val internalId = resolveIdAllocating(id)

        if (internalId >= e.size) {
            for (i in e.size..internalId) e.add(null)
        }

        e[internalId] = element
        movePointerToLast()

        elements.add(element)
        markInmutableDirty()

        return id
    }

    fun requestId(element: T, id: String): Int =
        requestId(element, id.hashCode())

    /**
     * NON-LAZY version of nextId()
     */
    fun nextId(element: T): Int {
        e.add(element)
        elements.add(element)
        markInmutableDirty()
        movePointerToLast()
        return externalizeId(e.lastIndexOf(element))
    }

    fun nextId(element: () -> T): Int =
        nextId(element())

    fun nextId(): Int {
        e.add(null)
        markInmutableDirty()
        movePointerToLast()
        return externalizeId(e.lastIndexOf(null))
    }


    // ============================================================
    // -----------  LAZY VERSIONS (original behavior) -------------
    // ============================================================

    fun requestIdLazy(element: T, id: String) =
        requestIdLazy(element, id.hashCode())

    open fun requestIdLazy(element: T, id: Int) = lazy {
        requestId(element, id) // delegate to non-lazy
    }

    fun nextIdLazy(element: () -> T) = lazy {
        nextId(element())
    }

    fun nextIdLazy(element: T) = lazy {
        nextId(element)
    }

    fun nextIdLazy() = lazy {
        nextId()
    }

    // ============================================================

    fun reserveId(id: Int): Int {
        if (shouldEnableHashMapping(id)) useHashMapping = true

        val internalId = resolveIdAllocating(id)

        if (internalId >= e.size) {
            for (i in e.size..internalId) e.add(null)
        }

        movePointerToLast()
        markInmutableDirty()
        return id
    }

    fun has(id: Int) = try { get(id) != null } catch (_: Exception) { false }
    fun has(id: String) = has(id.hashCode())
    fun has(id: Int, elem: T) = try { get(id) == elem } catch (_: Exception) { false }

    operator fun get(id: String) = get(id.hashCode())!!

    operator fun get(id: Int): T? {
        if (!useHashMapping) {
            if (id < 0 || id >= e.size)
                throw ArrayIndexOutOfBoundsException("The id $id has not been allocated in this container")
            return e[id]
        }

        val internalId = extToInt?.get(id)
            ?: throw ArrayIndexOutOfBoundsException("The id $id has not been allocated in this container")

        if (internalId !in 0 until e.size)
            throw ArrayIndexOutOfBoundsException("The id $id has not been allocated in this container")

        return e[internalId]
    }

    fun removeId(id: Int) {
        if (!useHashMapping) {
            if (id < 0 || id >= e.size) return
            val elem = e[id]
            if (elem != null) {
                elements.remove(elem)
                markInmutableDirty()
                e[id] = null
            }
            return
        }

        val internalId = extToInt?.get(id) ?: return
        if (internalId < 0 || internalId >= e.size) return

        val elem = e[internalId]
        if (elem != null) {
            elements.remove(elem)
            markInmutableDirty()
            e[internalId] = null
        }

        extToInt?.remove(id)
        intToExt?.remove(internalId)
    }

    fun peek() = e.getOrNull(stackPointer - 1)

    fun pushforwardIfNonNull() {
        if (e.getOrNull(stackPointer) != null) {
            stackPointer += 1
            if (stackPointer == e.size) {
                stackPointerFollowing = true
            }
        }
    }

    fun peekAndPushback() = peek()?.also {
        stackPointer = max(stackPointer - 1, 1)
        stackPointerFollowing = false
    }

    fun pop() = removeId(max(stackPointer - 1, 0))

    fun fork() {
        if (stackPointer == e.size) return

        val newE = ArrayList<T?>()
        for (i in 0 until stackPointer) newE.add(e[i])

        e.clear()
        e.addAll(newE)

        if (extToInt != null && intToExt != null) {
            val validInternalIds = e.indices.toSet()
            val toRemove = intToExt!!.keys.filter { it !in validInternalIds }
            toRemove.forEach { internalId ->
                val ext = intToExt!!.remove(internalId)
                if (ext != null) extToInt!!.remove(ext)
            }
        }

        elements.clear()
        elements.addAll(e.filterNotNull())
        markInmutableDirty()

        stackPointerFollowing = true
    }

    operator fun set(id: Int, element: T) {
        requestId(element, id)
    }

    fun clear() {
        e.clear()
        elements.clear()
        _inmutable = elements.toList()
        inmutableDirty = false

        extToInt?.clear()
        intToExt?.clear()
        extToInt = null
        intToExt = null

        nextSequentialId = 0
        useHashMapping = false

        stackPointer = 1
        stackPointerFollowing = true
    }

    override fun iterator() = inmutable.listIterator()
}
