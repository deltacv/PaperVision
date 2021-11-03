package io.github.deltacv.easyvision.id

class IdElementContainer<T> : Iterable<T> {

    private val e = ArrayList<T?>()

    /**
     * Note that the element positions in this list won't necessarily match their ids
     */
    var elements = ArrayList<T>()
        private set

    fun requestId(element: T, id: Int) = lazy {
        if(id >= e.size) {
            // add null elements until the list has a size of "id"
            // and add "element" to the list in the index "id"
            for(i in e.size until id) {
                e.add(null)
            }
        }

        e.add(id, element)
        elements.add(element)

        e.lastIndexOf(element)
    }

    fun nextId(element: () -> T) = lazy {
        nextId(element()).value
    }

    fun nextId(element: T) = lazy {
        e.add(element)
        elements.add(element)

        e.lastIndexOf(element)
    }

    fun nextId() = lazy {
        e.add(null)
        e.lastIndexOf(null)
    }

    fun removeId(id: Int) {
        elements.remove(e[id])
        e[id] = null
    }

    operator fun get(id: Int): T? {
        if(id < 0 || id > e.size) {
            throw ArrayIndexOutOfBoundsException("The id $id has not been allocated in this container")
        }

        return e[id]
    }

    operator fun set(id: Int, element: T) {
        e[id] = element

        if(!elements.contains(element))
            elements.add(element)
    }

    fun clear() {
        e.clear()
        elements.clear()
    }

    override fun iterator() = elements.listIterator()
}