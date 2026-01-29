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

class IdContainerStacks {

    companion object {
        // Thread-local instance of IdContainerStack
        private val threadLocalStack = ThreadLocal.withInitial { IdContainerStacks() }

        // Accessor for the current thread's stack
        val local: IdContainerStacks
            get() = threadLocalStack.get()
    }

    private val stacks = mutableMapOf<Class<out IdElement>, ArrayDeque<IdContainer<*>>>()

    fun <T: IdElement> push(clazz: Class<T>, container: IdContainer<out T>) {
        val stack = stacks[clazz] ?: ArrayDeque()

        stack.addLast(container)

        stacks[clazz] = stack
    }

    inline fun <reified T: IdElement> push(container: IdContainer<out T>) = push(T::class.java, container)

    @Suppress("UNCHECKED_CAST")
    fun <T: IdElement> peek(clazz: Class<T>): IdContainer<T>? {
        return if(stacks.containsKey(clazz)) {
            stacks[clazz]!!.last() as IdContainer<T> // uhhhh.... this is fine lol
        } else null
    }

    inline fun <reified T: IdElement> peek() = peek(T::class.java)

    inline fun <reified T: IdElement> peekNonNull() = peek(T::class.java) ?: throw NullPointerException("No IdElementContainer was found for ${T::class.java.typeName} in the stack")

    inline fun <reified T: IdElement> peekSingle(): T? {
        val container = peek<T>() ?: return null

        if(container is SingleIdContainer) {
            return container.get()
        } else {
            throw ClassCastException("The container for ${T::class.java.typeName} is not a SingleIdElementContainer")
        }
    }

    inline fun <reified T: IdElement> peekSingleNonNull() = peekSingle<T>() ?: throw NullPointerException("No IdElement was found for ${T::class.java.typeName} in the stack")

    fun <T: IdElement> pop(clazz: Class<T>) = stacks[clazz]?.removeLast() != null

    inline fun <reified T: IdElement> pop() = pop(T::class.java)

    fun all(): List<IdContainer<*>> {
        val all = mutableListOf<IdContainer<*>>()

        stacks.forEach { (_, stack) ->
            all.addAll(stack)
        }

        return all
    }

}
