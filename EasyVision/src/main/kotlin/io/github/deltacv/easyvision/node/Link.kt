package io.github.deltacv.easyvision.node

import imgui.extension.imnodes.ImNodes
import imgui.extension.imnodes.flag.ImNodesColorStyle
import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.attribute.TypedAttribute
import io.github.deltacv.easyvision.id.DrawableIdElement
import io.github.deltacv.easyvision.id.IdElementContainer

class Link(val a: Int, val b: Int) : DrawableIdElement {

    override val id by links.nextId { this }

    val aAttrib = Node.attributes[a]!!
    val bAttrib = Node.attributes[b]!!

    override fun draw() {
        if(!aAttrib.links.contains(this))
            aAttrib.links.add(this)

        if(!bAttrib.links.contains(this))
            bAttrib.links.add(this)

        val typedAttrib = when {
            aAttrib is TypedAttribute -> aAttrib
            bAttrib is TypedAttribute -> bAttrib
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
    }

    override fun restore() {
        links[id] = this

        aAttrib.links.add(this)
        bAttrib.links.add(this)
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

}