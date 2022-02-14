package io.github.deltacv.easyvision.id

object NoneIdElement : IdElement {
    override val id = 0xDAFC
}

interface IdElement {
    val id: Int
}

interface DrawableIdElement : IdElement {

    fun draw()

    fun delete()

    fun restore()

    fun onEnable() { }

    fun enable()

}

@Suppress("UNCHECKED_CAST")
abstract class DrawableIdElementBase<T : DrawableIdElementBase<T>> : DrawableIdElement {

    abstract val idElementContainer: IdElementContainer<T>

    open val requestedId: Int? = null

    private var internalId: Int? = null

    override val id: Int get() {
        if(internalId == null) {
            enable()
        }

        return internalId!!
    }

    override fun enable() {
        if(internalId == null) {
            internalId = provideId()

            onEnable()
        }
    }

    protected open fun provideId() =
        if(requestedId == null) {
            idElementContainer.nextId(this as T).value
        } else idElementContainer.requestId(this as T, requestedId!!).value

    override fun delete() {
        idElementContainer.removeId(id)
    }

    override fun restore() {
        idElementContainer[id] = this as T
    }

}