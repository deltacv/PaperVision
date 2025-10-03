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

object Misc : IdElement {
    override val id = 0xDAFC
    fun newMiscId() = IdElementContainerStack.localStack.peekNonNull<Misc>().nextId()
}