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

package io.github.deltacv.papervision.node

import imgui.extension.imnodes.ImNodes
import imgui.extension.imnodes.flag.ImNodesCol
import io.github.deltacv.papervision.action.Action
import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.TypedAttribute
import io.github.deltacv.papervision.id.DrawableIdElementBase
import io.github.deltacv.papervision.id.IdElementContainerStack
import io.github.deltacv.papervision.serialization.data.DataSerializable
import io.github.deltacv.papervision.serialization.LinkSerializationData

class Link(
    val a: Int,
    val b: Int,
    val isDestroyableByUser: Boolean = true,
    override val shouldSerialize: Boolean = true
) : DrawableIdElementBase<Link>(), DataSerializable<LinkSerializationData> {

    val attribIdElementContainer = IdElementContainerStack.localStack.peekNonNull<Attribute>()
    override val idElementContainer = IdElementContainerStack.localStack.peekNonNull<Link>()

    val aAttrib get() = attribIdElementContainer[a]
    val bAttrib get() = attribIdElementContainer[b]

    var associatedAction: Action? = null

    constructor(data: LinkSerializationData) : this(data.from, data.to)

    fun getOtherAttribute(me: Attribute) = if(me == aAttrib) bAttrib else aAttrib

    override fun draw() {
        if(aAttrib?.links?.contains(this) == false)
            aAttrib?.links?.add(this)
        if(bAttrib?.links?.contains(this) == false)
            bAttrib?.links?.add(this)

        if(aAttrib == null || bAttrib == null) {
            delete()
            return
        }

        val typedAttrib = when {
            aAttrib is TypedAttribute -> aAttrib as TypedAttribute
            bAttrib is TypedAttribute -> bAttrib as TypedAttribute
            else -> null
        }

        typedAttrib?.run {
            ImNodes.pushColorStyle(ImNodesCol.Link, styleColor)
            ImNodes.pushColorStyle(ImNodesCol.LinkHovered, styleHoveredColor)
            ImNodes.pushColorStyle(ImNodesCol.LinkSelected, styleHoveredColor)
        }

        ImNodes.link(id, a, b)

        if(typedAttrib != null) {
            ImNodes.popColorStyle()
            ImNodes.popColorStyle()
            ImNodes.popColorStyle()
        }
    }

    override fun onEnable() {
    }

    override fun delete() {
        idElementContainer.removeId(id)
        triggerOnChange()
    }

    override fun restore() {
        idElementContainer[id] = this
        triggerOnChange()
    }

    internal fun triggerOnChange() {
        aAttrib?.onChange?.run()
        bAttrib?.onChange?.run()
    }

    companion object {
        fun getLinksBetween(a: Node<*>, b: Node<*>): List<Link> {
            val l = mutableListOf<Link>()

            for(link in IdElementContainerStack.localStack.peekNonNull<Link>()) {
                val linkNodeA = link.aAttrib?.parentNode ?: continue
                val linkNodeB = link.bAttrib?.parentNode ?: continue

                if (
                    (a == linkNodeA && b == linkNodeB) || (b == linkNodeA && a == linkNodeB)
                ) {
                    l.add(link)
                }
            }

            return l
        }
    }

    override fun serialize() = LinkSerializationData(a, b)

    override fun deserialize(data: LinkSerializationData) {
    }

    override fun toString() = "Link(from=$a, to=$b)"

}