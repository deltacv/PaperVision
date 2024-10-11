package io.github.deltacv.papervision.id

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

    fun pollChange(): Boolean

}

@Suppress("UNCHECKED_CAST")
abstract class DrawableIdElementBase<T : DrawableIdElementBase<T>> : DrawableIdElement {

    abstract val idElementContainer: IdElementContainer<T>

    var hasEnabled = false
        private set

    val isEnabled get() = idElementContainer.has(id, this as T)

    open val requestedId: Int? = null

    private var internalId: Int? = null

    override val id: Int get() {
        if(internalId == null) {
            enable()
        }

        return internalId!!
    }

    override fun enable() {
        if(internalId == null || !idElementContainer.has(id, this as T)) {
            internalId = provideId()
            onEnable()
            hasEnabled = true
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

    override fun pollChange() = false

}