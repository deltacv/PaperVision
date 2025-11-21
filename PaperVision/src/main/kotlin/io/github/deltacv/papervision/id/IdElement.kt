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

interface StatedIdElement : IdElement {
    fun enable()
    fun delete()
    fun restore()
    fun onEnable() { }
}

// Internal helper class to encapsulate the common logic
internal class IdElementState<T: IdElement>(
    private val idElementContainer: IdElementContainer<T>,
    private val requestedId: Int?,
    private val self: T,
    private val onEnableCallback: () -> Unit
) {
    var hasEnabled = false
        private set

    private var internalId: Int? = null

    val id: Int
        get() {
            if (internalId == null) {
                enable()
            }
            return internalId!!
        }

    val isEnabled: Boolean
        get() = internalId?.let { idElementContainer.has(it, self) } ?: false

    fun enable() {
        if (internalId == null) {
            internalId = provideId()
            onEnableCallback()
            hasEnabled = true
        } else if (!idElementContainer.has(internalId!!, self)) {
            onEnableCallback()
            hasEnabled = true
        }
    }

    private fun provideId() =
        if (requestedId == null) {
            idElementContainer.nextId(self).value
        } else {
            idElementContainer.requestId(self, requestedId).value
        }

    fun delete() {
        internalId?.let { idElementContainer.removeId(it) }
    }

    fun restore() {
        internalId?.let { idElementContainer[it] = self }
    }
}

@Suppress("UNCHECKED_CAST")
abstract class StatedIdElementBase<T : StatedIdElementBase<T>> : StatedIdElement {

    abstract val idElementContainer: IdElementContainer<T>

    open val requestedId: Int? = null

    private val state by lazy {
        IdElementState(
            idElementContainer,
            requestedId,
            this as T,
            ::onEnable
        )
    }

    var hasEnabled: Boolean
        get() = state.hasEnabled
        private set(_) {}

    val isEnabled: Boolean
        get() = state.isEnabled

    override val id: Int
        get() = state.id

    override fun enable() = state.enable()

    override fun delete() = state.delete()

    override fun restore() = state.restore()
}

interface DrawableIdElement : StatedIdElement {
    fun draw()
    fun pollChange(): Boolean
}

@Suppress("UNCHECKED_CAST")
abstract class DrawableIdElementBase<T : DrawableIdElementBase<T>> : DrawableIdElement {

    abstract val idElementContainer: IdElementContainer<T>

    open val requestedId: Int? = null

    private val state by lazy {
        IdElementState(
            idElementContainer,
            requestedId,
            this as T,
            ::onEnable
        )
    }

    var hasEnabled: Boolean
        get() = state.hasEnabled
        private set(_) {}

    val isEnabled: Boolean
        get() = state.isEnabled

    override val id: Int
        get() = state.id

    override fun enable() = state.enable()

    override fun delete() = state.delete()

    override fun restore() = state.restore()

    override fun pollChange() = false
}

object Misc : IdElement {
    override val id = 0xDAFC
    fun newMiscId() = IdElementContainerStack.local.peekNonNull<Misc>().nextId()
}