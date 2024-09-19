package io.github.deltacv.papervision.id

class IdElementContainer<T : IdElement> : Iterable<T> {

    private val e = ArrayList<T?>()

    /**
     * Note that the element positions in this list won't necessarily match their ids
     */
    var elements = ArrayList<T>()
        private set

    @Suppress("UNCHECKED_CAST")
    var inmutable: List<T> = elements.clone() as List<T>
        private set

    @Suppress("UNCHECKED_CAST")
    private fun reallocateArray() {
        inmutable = elements.clone() as List<T>
    }

    fun requestId(element: T, id: Int) = lazy {
        if(id >= e.size) {
            // add null elements until the list has a size of "id"
            // and add "element" to the list in the index "id"
            for(i in e.size..id) {
                e.add(null)
            }
        }

        e[id] = element

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

        e.lastIndexOf(element)
    }

    fun nextId() = lazy {
        e.add(null)
        reallocateArray()
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

    operator fun set(id: Int, element: T) {
        e[id] = element

        if(!elements.contains(element)) {
            elements.add(element)

            reallocateArray()
        }
    }

    fun clear() {
        e.clear()
        elements.clear()
        reallocateArray()
    }

    override fun iterator() = elements.listIterator()
}