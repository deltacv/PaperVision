package io.github.deltacv.papervision.node

import imgui.extension.imnodes.ImNodes
import imgui.extension.imnodes.flag.ImNodesColorStyle
import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.TypedAttribute
import io.github.deltacv.papervision.id.DrawableIdElementBase
import io.github.deltacv.papervision.id.IdElementContainerStack
import io.github.deltacv.papervision.serialization.data.DataSerializable
import io.github.deltacv.papervision.serialization.ev.LinkSerializationData

class Link(
    val a: Int,
    val b: Int,
    val isDestroyableByUser: Boolean = true,
) : DrawableIdElementBase<Link>(), DataSerializable<LinkSerializationData> {

    val attribIdElementContainer = IdElementContainerStack.threadStack.peekNonNull<Attribute>()
    override val idElementContainer = IdElementContainerStack.threadStack.peekNonNull<Link>()

    val aAttrib by lazy { attribIdElementContainer[a] }
    val bAttrib by lazy { attribIdElementContainer[b] }

    constructor(data: LinkSerializationData) : this(data.from, data.to)

    override fun draw() {
        if(aAttrib?.links?.contains(this) == false)
            aAttrib?.links?.add(this)

        if(bAttrib?.links?.contains(this) == false)
            bAttrib?.links?.add(this)

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
        aAttrib?.links?.remove(this)
        bAttrib?.links?.remove(this)

        idElementContainer.removeId(id)
        triggerOnChange()
    }

    override fun restore() {
        idElementContainer[id] = this

        aAttrib?.links?.add(this)
        bAttrib?.links?.add(this)

        triggerOnChange()
    }

    internal fun triggerOnChange() {
        aAttrib?.onChange?.run()
    }

    companion object {
        fun getLinksBetween(a: Node<*>, b: Node<*>): List<Link> {
            val l = mutableListOf<Link>()

            for(link in IdElementContainerStack.threadStack.peekNonNull<Link>()) {
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