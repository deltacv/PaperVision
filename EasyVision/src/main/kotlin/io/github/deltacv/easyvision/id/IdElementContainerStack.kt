package io.github.deltacv.easyvision.id

object IdElementContainerStack {

    private val stacks = mutableMapOf<Class<out IdElement>, ArrayDeque<IdElementContainer<*>>>()

    fun <T: IdElement> push(clazz: Class<T>, container: IdElementContainer<out T>) {
        val stack = stacks[clazz] ?: ArrayDeque()

        stack.addLast(container)

        stacks[clazz] = stack
    }

    inline fun <reified T: IdElement> push(container: IdElementContainer<out T>) = push(T::class.java, container)

    @Suppress("UNCHECKED_CAST")
    fun <T: IdElement> peek(clazz: Class<T>): IdElementContainer<T>? {
        return if(stacks.containsKey(clazz)) {
            stacks[clazz]!!.last() as IdElementContainer<T> // uhhhh.... this is fine lol
        } else null
    }

    inline fun <reified T: IdElement> peek() = peek(T::class.java)

    inline fun <reified T: IdElement> peekNonNull() = peek(T::class.java) ?: throw NullPointerException("No IdElementContainer was found for ${T::class.java.typeName} in the stack")

    fun <T: IdElement> pop(clazz: Class<T>) = stacks[clazz]?.removeLast() != null

    inline fun <reified T: IdElement> pop() = pop(T::class.java)

}