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

    /**
     * Note that the element positions in this list won't necessarily match their ids
     */
    var elements = ArrayList<T>()
        private set

    /**
     * Points to an element in the container.
     * This pointer is used by all the stack operation functions.
     * To get the element pointed by the stackPointer, use [peek].
     *
     * While the container is zero-based as normal, the stack pointer is one-based.
     * This means that the first element in the container is at index 0, but the stack pointer is at 1.
     */
    var stackPointer: Int = 1

    var stackPointerFollowing = true
        private set

    val size get() = e.size

    @Suppress("UNCHECKED_CAST")
    var inmutable: List<T> = elements.clone() as List<T>
        private set

    @Suppress("UNCHECKED_CAST")
    private fun reallocateArray() {
        inmutable = elements.clone() as List<T>
    }

    private fun movePointerToLast() {
        if(stackPointerFollowing) {
            stackPointer = e.size
        }
    }

    open fun requestId(element: T, id: Int) = lazy {
        if(id >= e.size) {
            // add null elements until the list has a size of "id"
            // and add "element" to the list in the index "id"
            for(i in e.size..id) {
                e.add(null)
            }
        }

        e[id] = element
        movePointerToLast()

        elements.add(element)
        reallocateArray()

       id
    }

    fun reserveId(id: Int): Int {
        if(id >= e.size) {
            // add null elements until the list has a size of "id"
            // and add "element" to the list in the index "id"
            for(i in e.size until id) {
                e.add(null)
            }
        }

        e.add(id, null)
        movePointerToLast()

        reallocateArray()

        return id
    }

    fun has(id: Int, elem: T) = try {
        get(id) == elem
    } catch(_: Exception) { false }

    fun nextId(element: () -> T) = lazy {
        nextId(element()).value
    }

    fun nextId(element: T) = lazy {
        e.add(element)

        elements.add(element)
        reallocateArray()
        movePointerToLast()

        e.lastIndexOf(element)
    }

    fun nextId() = lazy {
        e.add(null)

        reallocateArray()
        movePointerToLast()

        e.lastIndexOf(null)
    }

    fun removeId(id: Int) {
        elements.remove(e[id])
        reallocateArray()
        e[id] = null
    }

    operator fun get(id: Int): T? {
        if(id < 0 || id > e.size) {
            throw ArrayIndexOutOfBoundsException("The id $id has not been allocated in this container")
        }

        return e[id]
    }

    /**
     * Peeks the element at the current stack pointer.
     * @return the element at the current stack pointer
     */
    fun peek() = e.getOrNull(stackPointer - 1)

    /**
     * Pushes the stack pointer forward by one if the element at the current pointer is not null.
     */
    fun pushforwardIfNonNull() {
        if(e.getOrNull(stackPointer) != null) {
            stackPointer += 1

            // if we have pushforward to the end of the list, set it to follow
            if(stackPointer == e.size) {
                stackPointerFollowing = true
            }
        }
    }

    /**
     * Peeks the element at the current stack pointer and pushes the pointer back by one
     * only if the element at the current pointer found by [peek] is not null.
     * @return the element at the current stack pointer
     */
    fun peekAndPushback() = peek()?.also {
        stackPointer = max(stackPointer - 1, 1)
        stackPointerFollowing = false
    }

    /**
     * Pops the element at the current stack pointer.
     * May cause issues if the stackPointer is not
     * following the end of the list.
     */
    fun pop() = removeId(max(stackPointer - 1, 0))

    /**
     * Forks the container, creating a new container with the same elements
     * up to the current stack pointer, discarding everything after the pointer
     * This function does nothing if the stack pointer is at the end of the list.
     */
    fun fork() {
        if(stackPointer == e.size) return

        val newE = ArrayList<T?>()

        // Copy all elements up to the current pointer, excluding everything after
        for(i in 0 until stackPointer) {
            newE.add(e[i])
        }

        e.clear()
        e.addAll(newE)

        // recreate elements
        elements.clear()
        elements.addAll(e.filterNotNull())

        reallocateArray()

        stackPointerFollowing = true
    }

    operator fun set(id: Int, element: T) {
        requestId(element, id).value
    }

    fun clear() {
        e.clear()
        elements.clear()
        reallocateArray()
    }

    override fun iterator() = elements.listIterator()
}