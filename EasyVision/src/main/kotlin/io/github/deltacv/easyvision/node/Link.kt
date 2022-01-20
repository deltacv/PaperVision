package io.github.deltacv.easyvision.node

import imgui.extension.imnodes.ImNodes
import imgui.extension.imnodes.flag.ImNodesColorStyle
import io.github.deltacv.easyvision.attribute.TypedAttribute
import io.github.deltacv.easyvision.id.DrawableIdElement
import io.github.deltacv.easyvision.id.IdElementContainer
import io.github.deltacv.easyvision.serialization.data.DataSerializable
import io.github.deltacv.easyvision.serialization.ev.LinkSerializationData
import io.github.deltacv.easyvision.util.event.EventListener

class Link(val a: Int, val b: Int) : DrawableIdElement, DataSerializable<LinkSerializationData> {

    override val id by links.nextId { this }

    val aAttrib by lazy { Node.attributes[a]!! }
    val bAttrib by lazy { Node.attributes[b]!! }

    constructor(data: LinkSerializationData) : this(data.from, data.to)

    override fun draw() {
        if(!aAttrib.links.contains(this))
            aAttrib.links.add(this)

        if(!bAttrib.links.contains(this))
            bAttrib.links.add(this)

        val typedAttrib = when {
            aAttrib is TypedAttribute -> aAttrib as TypedAttribute
            bAttrib is TypedAttribute -> bAttrib as TypedAttribute
            else -> null
        }

        typedAttrib?.run {
            ImNodes.pushColorStyle(ImNodesColorStyle.Link, styleColor)
            ImNodes.pushColorStyle(ImNodesColorStyle.LinkHovered, styleHoveredColor)
            ImNodes.pushColorStyle(ImNodesColorStyle.LinkSelected, styleHoveredColor)
        }

        ImNodes.link(id, a, b)

        if(typedAttrib != null) {
            ImNodes.popColorStyle()
            ImNodes.popColorStyle()
            ImNodes.popColorStyle()
        }
    }

    override fun delete() {
        aAttrib.links.remove(this)
        bAttrib.links.remove(this)

        links.removeId(id)
        triggerOnChange()
    }

    override fun restore() {
        links[id] = this

        aAttrib.links.add(this)
        bAttrib.links.add(this)

        triggerOnChange()
    }

    internal fun triggerOnChange() {
        aAttrib.onChange.run()
        bAttrib.onChange.run()
    }

    companion object {
        val links = IdElementContainer<Link>()

        fun getLinksBetween(a: Node<*>, b: Node<*>): List<Link> {
            val l = mutableListOf<Link>()

            for(link in links) {
                val linkNodeA = link.aAttrib.parentNode
                val linkNodeB = link.bAttrib.parentNode

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