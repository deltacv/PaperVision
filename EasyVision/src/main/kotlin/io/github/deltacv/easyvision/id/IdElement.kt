package io.github.deltacv.easyvision.id

interface IdElement {
    val id: Int
}

interface DrawableIdElement : IdElement {

    fun draw()

    fun delete()

    fun restore()

    fun onEnable() { }

    fun enable(): DrawableIdElement {
        ::id.get()
        onEnable()
        return this
    }

}