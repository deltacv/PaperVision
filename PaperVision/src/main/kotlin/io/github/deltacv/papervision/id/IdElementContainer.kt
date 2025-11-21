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

package io.github.deltacv.papervision.id

import kotlin.math.max

class SingleIdElementContainer<T : IdElement> : IdElementContainer<T>() {
    override fun requestId(element: T, id: Int): Lazy<Int> {
        if(e.isNotEmpty()) {
            throw IllegalStateException("This container can only have one element")
        }

        return super.requestId(element, id)
    }

    fun get() = e.first()
}


open class IdElementContainer<T : IdElement> : Iterable<T> {

    protected val e = ArrayList<T?>()

    var elements = ArrayList<T>()
        private set

    private var hashToId: MutableMap<Int, Int>? = null
    private var idToHash: MutableMap<Int, Int>? = null
    private var nextSequentialId = 0

    private var useHashMapping = false

    private val sparsityThreshold = 1000

    var stackPointer: Int = 1
    var stackPointerFollowing = true
        private set

    val size get() = e.size

    @Suppress("UNCHECKED_CAST")
    var inmutable: List<T> = elements.clone() as List<T>
        private set

    @Suppress("UNCHECKED_CAST")
    private fun reallocateInmutable() {
        inmutable = elements.clone() as List<T>
    }

    private fun movePointerToLast() {
        if (stackPointerFollowing) {
            stackPointer = e.size
        }
    }

    /**
     * FIX #2: Negative IDs automatically force hash-mapping.
     */
    private fun shouldEnableHashMapping(requestedId: Int): Boolean {
        if (useHashMapping) return true
        if (requestedId < 0) return true            // ← FIX #2 HERE
        val gapSize = requestedId - e.size
        return gapSize > sparsityThreshold
    }

    /**
     * FIX #2: If externalId is negative, force hash mode.
     */
    private fun resolveId(externalId: Int): Int {
        if (!useHashMapping) {
            if (externalId < 0) useHashMapping = true   // ← FIX #2 HERE
            else return externalId
        }

        if (hashToId == null) {
            hashToId = HashMap()
            idToHash = HashMap()
            for (i in e.indices) {
                if (e[i] != null) {
                    hashToId!![i] = i
                    idToHash!![i] = i
                    nextSequentialId = maxOf(nextSequentialId, i + 1)
                }
            }
        }

        return hashToId!!.getOrPut(externalId) {
            val internalId = nextSequentialId++
            idToHash!![internalId] = externalId
            internalId
        }
    }

    private fun externalizeId(internalId: Int): Int {
        return idToHash?.get(internalId) ?: internalId
    }

    fun requestId(element: T, id: String) =
        requestId(element, id.hashCode())

    /**
     * FIX #2: Negative ID triggers hash mapping.
     */
    open fun requestId(element: T, id: Int) = lazy {
        if (shouldEnableHashMapping(id)) {
            useHashMapping = true
        }

        val internalId = resolveId(id)

        if (internalId >= e.size) {
            for (i in e.size..internalId) {
                e.add(null)
            }
        }

        e[internalId] = element
        movePointerToLast()

        elements.add(element)
        reallocateInmutable()

        id
    }

    fun reserveId(id: Int): Int {
        if (shouldEnableHashMapping(id)) {
            useHashMapping = true
        }

        val internalId = resolveId(id)

        if (internalId >= e.size) {
            for (i in e.size until internalId) {
                e.add(null)
            }
        }

        e.add(internalId, null)
        movePointerToLast()

        reallocateInmutable()

        return id
    }

    fun has(id: Int) = try { get(id) != null } catch (_: Exception) { false }
    fun has(id: String) = has(id.hashCode())
    fun has(id: Int, elem: T) = try { get(id) == elem } catch (_: Exception) { false }

    fun nextId(element: () -> T) = lazy {
        nextId(element()).value
    }

    fun nextId(element: T) = lazy {
        e.add(element)
        elements.add(element)
        reallocateInmutable()
        movePointerToLast()

        externalizeId(e.lastIndexOf(element))
    }

    fun nextId() = lazy {
        e.add(null)
        reallocateInmutable()
        movePointerToLast()

        externalizeId(e.lastIndexOf(null))
    }

    fun removeId(id: Int) {
        if (!useHashMapping) {
            elements.remove(e[id])
            reallocateInmutable()
            e[id] = null
            return
        }

        val internalId = resolveId(id)
        elements.remove(e[internalId])
        reallocateInmutable()
        e[internalId] = null

        hashToId?.remove(id)
        idToHash?.remove(internalId)
    }

    operator fun get(id: String) = get(id.hashCode())!!

    operator fun get(id: Int): T? {
        if (!useHashMapping) {
            if (id < 0 || id > e.size) {
                throw ArrayIndexOutOfBoundsException("The id $id has not been allocated in this container")
            }
            return e[id]
        }

        val internalId = resolveId(id)

        if (internalId < 0 || internalId > e.size) {
            throw ArrayIndexOutOfBoundsException("The id $id has not been allocated in this container")
        }

        return e[internalId]
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

        if (hashToId != null && idToHash != null) {
            val valid = e.indices.toSet()
            val toRemove = idToHash!!.keys.filter { it !in valid }
            toRemove.forEach { internalId ->
                val externalId = idToHash!![internalId]
                idToHash!!.remove(internalId)
                if (externalId != null) hashToId!!.remove(externalId)
            }
        }

        elements.clear()
        elements.addAll(e.filterNotNull())

        reallocateInmutable()

        stackPointerFollowing = true
    }

    operator fun set(id: Int, element: T) {
        requestId(element, id).value
    }

    fun clear() {
        e.clear()
        elements.clear()
        reallocateInmutable()

        hashToId?.clear()
        idToHash?.clear()
        nextSequentialId = 0
        useHashMapping = false
    }

    override fun iterator() = inmutable.listIterator()
}