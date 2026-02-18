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

package io.github.deltacv.papervision.id.container

import io.github.deltacv.papervision.id.IdElement
import io.github.deltacv.papervision.util.loggerForThis
import kotlin.math.max

/**
 * Optimized ID container supporting sparse external IDs.
 *
 * Features:
 * - Array mode for dense IDs
 * - Hash mode for sparse IDs
 * - Lazy immutable snapshot list
 * - Stable external IDs
 */
open class IdContainer<T : IdElement> : Collection<T> {

    private val logger by loggerForThis()

    protected val slots = mutableListOf<T?>()
    private var elements = LinkedHashSet<T>()

    private var extToInt: MutableMap<Int, Int>? = null
    private var intToExt: MutableMap<Int, Int>? = null

    private var nextSequentialId = 0
    private var nextExternalId = 0

    private var useHashMapping = false
    private val sparsityThreshold = 1000

    var stackPointer: Int = 1
    var stackPointerFollowing = true
        private set

    private var _inmutable: List<T> = elements.toList()
    private var inmutableDirty = false

    /**
     * Unique list of elements currently stored in the container.
     * Modifying the container invalidates this snapshot, which is then recomputed on next access.
     * Avoids ConcurrentModificationException during iteration and provides stable view of elements.
     */
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
            stackPointer = slots.size
        }
    }

    private fun shouldEnableHashMapping(requestedId: Int): Boolean {
        if (useHashMapping) return false // dont enable again if already enabled
        if (requestedId < 0) return true // negative IDs are always sparse (slots can't have negative indices)
        return requestedId - slots.size > sparsityThreshold // if requested ID is far beyond current slots, likely sparse
    }

    private fun resolveIdAllocating(externalId: Int): Int {
        if (shouldEnableHashMapping(externalId)) {
            enableHashMappingAndMigrate()
        }

        if (!useHashMapping) {
            return externalId
        }

        return extToInt!!.getOrPut(externalId) {
            val internalId = nextSequentialId++
            intToExt!![internalId] = externalId
            internalId
        }
    }

    private fun enableHashMappingAndMigrate() {
        val initialCapacity = max(16, elements.size * 2)

        val newExtToInt = HashMap<Int, Int>(initialCapacity)
        val newIntToExt = HashMap<Int, Int>(initialCapacity)
        val newSlots = ArrayList<T?>(elements.size)

        var newInternal = 0

        for (externalId in slots.indices) {
            val elem = slots[externalId] ?: continue

            newExtToInt[externalId] = newInternal
            newIntToExt[newInternal] = externalId
            newSlots.add(elem)

            newInternal++
        }

        extToInt = newExtToInt
        intToExt = newIntToExt
        slots.clear()
        slots.addAll(newSlots)

        nextSequentialId = newInternal
        useHashMapping = true

        logger.debug("Enabled hash mapping and migrated ${elements.size} elements (type: ${elements.firstOrNull()?.javaClass?.simpleName ?: "empty, can't know"})")
    }

    /**
     * Assigns an element to an external ID.
     */
    open fun requestId(element: T, id: Int): Int {
        if (has(id, element)) return id

        val internalId = resolveIdAllocating(id)

        if (internalId >= slots.size) {
            for (i in slots.size..internalId) slots.add(null)
        }

        val old = slots[internalId]
        if (old != null) elements.remove(old)

        slots[internalId] = element

        if (!elements.contains(element))
            elements.add(element)

        markInmutableDirty()
        movePointerToLast()

        if (id >= nextExternalId) nextExternalId = id + 1

        return id
    }

    /**
     * String-based ID request.
     */
    fun requestId(element: T, id: String): Int =
        requestId(element, id.hashCode())

    /**
     * Allocates next free external ID and assigns element.
     */
    fun nextId(element: T): Int {
        val externalId = nextExternalId++

        if (useHashMapping) {
            val internalId = resolveIdAllocating(externalId)

            if (internalId >= slots.size) {
                for (i in slots.size..internalId) slots.add(null)
            }

            slots[internalId] = element
        } else {
            slots.add(element)
        }

        if (!elements.contains(element))
            elements.add(element)

        markInmutableDirty()
        movePointerToLast()

        return externalId
    }

    /**
     * Supplier-based allocation.
     */
    fun nextId(element: () -> T): Int =
        nextId(element())

    /**
     * Allocates next ID without assigning element.
     */
    fun nextId(): Int {
        val externalId = nextExternalId++

        if (useHashMapping) {
            val internalId = resolveIdAllocating(externalId)

            if (internalId >= slots.size) {
                for (i in slots.size..internalId) slots.add(null)
            }

            slots[internalId] = null
        } else {
            slots.add(null)
        }

        markInmutableDirty()
        movePointerToLast()

        return externalId
    }

    /**
     * Lazy ID request.
     */
    fun requestIdLazy(element: T, id: String) =
        requestIdLazy(element, id.hashCode())

    /**
     * Lazy ID request.
     */
    open fun requestIdLazy(element: T, id: Int) = lazy {
        requestId(element, id)
    }

    /**
     * Lazy next ID.
     */
    fun nextIdLazy(element: () -> T) = lazy {
        nextId(element())
    }

    /**
     * Lazy next ID.
     */
    fun nextIdLazy(element: T) = lazy {
        nextId(element)
    }

    /**
     * Lazy empty slot allocation.
     */
    fun nextIdLazy() = lazy {
        nextId()
    }

    /**
     * Reserves an external ID.
     */
    fun reserveId(id: Int): Int {
        if (shouldEnableHashMapping(id)) {
            enableHashMappingAndMigrate()
        }

        val internalId = resolveIdAllocating(id)

        if (internalId >= slots.size) {
            for (i in slots.size..internalId) slots.add(null)
        }

        if (id >= nextExternalId) nextExternalId = id + 1

        movePointerToLast()
        markInmutableDirty()
        return id
    }

    /**
     * Returns true if ID exists and has element.
     */
    fun has(id: Int): Boolean {
        if (!useHashMapping) {
            return id in 0 until slots.size && slots[id] != null
        }
        val internal = extToInt?.get(id) ?: return false
        return internal in 0 until slots.size && slots[internal] != null
    }

    fun has(id: String) = has(id.hashCode())

    /**
     * Checks ID → element match.
     */
    fun has(id: Int, elem: T): Boolean {
        if (!useHashMapping) {
            return id in 0 until slots.size && slots[id] == elem
        }
        val internal = extToInt?.get(id) ?: return false
        return internal in 0 until slots.size && slots[internal] == elem
    }

    /**
     * Gets element by string ID.
     */
    operator fun get(id: String): T? = get(id.hashCode())

    /**
     * Gets element by external ID.
     */
    operator fun get(id: Int): T? {
        if (!useHashMapping) {
            return slots.getOrNull(id)
        }

        val internalId = extToInt?.get(id) ?: return null
        return slots[internalId]
    }

    /**
     * Removes ID assignment.
     */
    fun removeId(id: Int) {
        if (!useHashMapping) {
            if (id < 0 || id >= slots.size) return
            val elem = slots[id]
            if (elem != null) {
                elements.remove(elem)
                markInmutableDirty()
                slots[id] = null
            }
            return
        }

        val internalId = extToInt?.get(id) ?: return
        if (internalId < 0 || internalId >= slots.size) return

        val elem = slots[internalId]
        if (elem != null) {
            elements.remove(elem)
            markInmutableDirty()
            slots[internalId] = null
        }

        extToInt?.remove(id)
        intToExt?.remove(internalId)
    }

    /**
     * Returns element at stack pointer.
     */
    fun peek() = slots.getOrNull(stackPointer - 1)

    /**
     * Advances pointer if next slot has element.
     */
    fun pushforwardIfNonNull() {
        if (slots.getOrNull(stackPointer) != null) {
            stackPointer += 1
            if (stackPointer == slots.size) {
                stackPointerFollowing = true
            }
        }
    }

    /**
     * Returns current element and moves pointer backward.
     */
    fun peekAndPushback() = peek()?.also {
        stackPointer = max(stackPointer - 1, 1)
        stackPointerFollowing = false
    }

    /**
     * Removes element at pointer.
     */
    fun pop() = removeId(max(stackPointer - 1, 0))

    /**
     * Truncates container to pointer.
     */
    fun fork() {
        if (stackPointer == slots.size) return

        val newE = ArrayList<T?>()
        for (i in 0 until stackPointer) newE.add(slots[i])

        slots.clear()
        slots.addAll(newE)

        if (extToInt != null && intToExt != null) {
            val validInternalIds = slots.indices.toSet()
            val toRemove = intToExt!!.keys.filter { it !in validInternalIds }
            toRemove.forEach { internalId ->
                val ext = intToExt!!.remove(internalId)
                if (ext != null) extToInt!!.remove(ext)
            }
            nextSequentialId = (intToExt!!.keys.maxOrNull() ?: -1) + 1
        }

        elements.clear()
        elements.addAll(slots.filterNotNull())
        markInmutableDirty()

        stackPointerFollowing = true
    }

    /**
     * Assign operator.
     */
    operator fun set(id: Int, element: T) {
        requestId(element, id)
    }

    /**
     * Clears container.
     */
    fun clear() {
        slots.clear()
        elements.clear()
        _inmutable = elements.toList()
        inmutableDirty = false

        extToInt?.clear()
        intToExt?.clear()
        extToInt = null
        intToExt = null

        nextSequentialId = 0
        nextExternalId = 0
        useHashMapping = false

        stackPointer = 1
        stackPointerFollowing = true
    }

    // ------------------------------------------------------------
    // Collection implementation
    // ------------------------------------------------------------

    override val size get() = elements.size

    override fun contains(element: T) = elements.contains(element)

    override fun containsAll(elements: Collection<T>) = elements.containsAll(elements)

    override fun isEmpty() = elements.isEmpty()

    // ------------------------------------------------------------
    // Iterable implementation
    // ------------------------------------------------------------

    /**
     * Iterator over snapshot list.
     */
    override fun iterator() = inmutable.iterator()
}
