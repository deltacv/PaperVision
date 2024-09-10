package io.github.deltacv.papervision.node

import imgui.ImGui
import imgui.ImVec2
import imgui.extension.imnodes.ImNodes
import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.codegen.*
import io.github.deltacv.papervision.exception.NodeGenException
import io.github.deltacv.papervision.gui.NodeEditor
import io.github.deltacv.papervision.id.DrawableIdElementBase
import io.github.deltacv.papervision.id.IdElementContainerStack
import io.github.deltacv.papervision.node.vision.OutputMatNode
import io.github.deltacv.papervision.serialization.data.DataSerializable
import io.github.deltacv.papervision.serialization.ev.BasicNodeData
import io.github.deltacv.papervision.serialization.ev.NodeSerializationData
import io.github.deltacv.papervision.util.event.EventHandler
import io.github.deltacv.papervision.util.event.EventListener

interface Type {
    val name: String
}

abstract class Node<S: CodeGenSession>(
    private var allowDelete: Boolean = true
) : DrawableIdElementBase<Node<*>>(), GenNode<S>, DataSerializable<NodeSerializationData> {

    override val idElementContainer = IdElementContainerStack.threadStack.peekNonNull<Node<*>>()
    override val requestedId get() = serializedId

    var serializedId: Int? = null
        private set

    var showAttributesCircles = true

    // will be set on NodeEditor#draw
    lateinit var editor: NodeEditor
        internal set

    val isOnEditor get() = ::editor.isInitialized

    override val genOptions = CodeGenOptions()

    override var lastGenSession: S? = null

    val onChange = EventHandler("${this::class.java.simpleName}-OnChange")
    val onDelete = EventHandler("OnDelete-${this::class.simpleName}")

    private val attribOnChangeListener = EventListener {
        onChange.run()
    }

    @Transient
    private val attribs = mutableListOf<Attribute>() // internal mutable list

    val nodeAttributes = attribs as List<Attribute> // public read-only

    protected fun drawAttributes() {
        for((i, attribute) in nodeAttributes.withIndex()) {
            attribute.draw()

            if(i < nodeAttributes.size - 1 && !attribute.wasLastDrawCancelled) {
                ImGui.newLine() // make a new blank line if this isn't the last attribute
            }
        }
    }

    override fun delete() {
        if(allowDelete) {
            for (attribute in nodeAttributes.toTypedArray()) {
                for(link in attribute.links.toTypedArray()) {
                    link.delete()
                }

                attribute.delete()
                attribs.remove(attribute)
            }

            idElementContainer.removeId(id)
            onDelete.run()
        }
    }

    override fun restore() {
        if(allowDelete) {
            for (attribute in nodeAttributes.toTypedArray()) {
                for(link in attribute.links.toTypedArray()) {
                    link.restore()
                }

                attribute.restore()
                attribs.add(attribute)
            }

            idElementContainer[id] = this
        }
    }

    fun addAttribute(attribute: Attribute) {
        if(!attribs.contains(attribute)) {
            attribute.parentNode = this
            attribute.onChange(attribOnChangeListener)
            attribs.add(attribute)
        }
    }

    fun removeAttribute(attribute: Attribute) {
        if(attribs.contains(attribute)) {
            //attribute.parentNode = null
            attribute.onChange.removePersistentListener(attribOnChangeListener)
            attribs.remove(attribute)
        }
    }

    operator fun Attribute.unaryPlus() = addAttribute(this)

    open override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        raise("Node doesn't have output attributes")
    }

    fun hasDeadEnd(initialNode: Node<*> = this): Boolean {
        nodeAttributesLoop@
        for(attribute in nodeAttributes) {
            if(attribute.mode == AttributeMode.INPUT) {
                continue
            }

            for(linkedAttribute in attribute.linkedAttributes()) {
               if(linkedAttribute != null) {
                   if(linkedAttribute.mode == AttributeMode.OUTPUT || linkedAttribute.parentNode == initialNode) {
                       continue // uh oh
                   }

                   if(linkedAttribute.parentNode is OutputMatNode || !linkedAttribute.parentNode.hasDeadEnd(initialNode)) {
                       return false // not a dead end
                   }
               }
            }
        }

        return true
    }

    override fun propagate(current: CodeGen.Current) {
        val linkedNodes = mutableListOf<Node<*>>()

        for(attribute in attribs) {
            if(attribute.mode == AttributeMode.OUTPUT) {
                for(linkedAttribute in attribute.linkedAttributes()) {
                    if(linkedAttribute != null && !linkedNodes.contains(linkedAttribute.parentNode)) {
                        linkedNodes.add(linkedAttribute.parentNode)
                    }
                }
            }
        }

        val deadEndNodes = mutableListOf<Node<*>>()
        val completePathNodes = mutableListOf<Node<*>>()

        for(linkedNode in linkedNodes) {
            if(linkedNode.hasDeadEnd()) {
                deadEndNodes.add(linkedNode)
            } else {
                completePathNodes.add(linkedNode)
            }
        }

        completePathNodes.forEach { it.receivePropagation(current) }
        deadEndNodes.forEach { it.receivePropagation(current) }
    }

    open fun makeSerializationData() = BasicNodeData(id, ImVec2().apply {
        ImNodes.getNodeEditorSpacePos(id, this)
    })

    open fun takeDeserializationData(data: NodeSerializationData) { /* do nothing */ }

    /**
     * Call before enable()
     */
    final override fun deserialize(data: NodeSerializationData) {
        serializedId = data.id
        if(this is DrawNode<*>) {
            nextNodePosition = data.nodePos
        }

        takeDeserializationData(data)
    }

    final override fun serialize(): NodeSerializationData {
        val data = makeSerializationData()
        data.id = id

        val pos = ImVec2()
        ImNodes.getNodeEditorSpacePos(id, pos)
        data.nodePos = pos

        return data
    }

    fun raise(message: String): Nothing = throw NodeGenException(this, message)

    fun warn(message: String) {
        println("WARN: $message") // TODO: Warnings system...
    }

    fun raiseAssert(condition: Boolean, message: String) {
        if(!condition) {
            raise(message)
        }
    }

    fun warnAssert(condition: Boolean, message: String) {
        if(!condition) {
            warn(message)
        }
    }

    override fun toString() = "Node(type=${this::class.java.typeName}, id=$id)"

    companion object {
        @JvmStatic protected val INPUT = AttributeMode.INPUT
        @JvmStatic protected val OUTPUT = AttributeMode.OUTPUT

        fun checkRecursion(from: Node<*>, to: Node<*>): Boolean {
            val linksBetween = Link.getLinksBetween(from, to)

            var hasOutputToInput = false
            var hasInputToOutput = false

            for(link in linksBetween) {
                val aNode = link.aAttrib?.parentNode ?: continue

                val fromAttrib = (if(aNode == from) link.aAttrib else link.bAttrib) ?: continue
                val toAttrib   = (if(aNode == to) link.aAttrib else link.bAttrib) ?: continue

                if(!hasOutputToInput)
                    hasOutputToInput = fromAttrib.mode == OUTPUT && toAttrib.mode == INPUT

                if(!hasInputToOutput)
                    hasInputToOutput = fromAttrib.mode == INPUT && toAttrib.mode == OUTPUT

                if(hasOutputToInput && hasInputToOutput)
                    break
            }

            return hasOutputToInput && hasInputToOutput
        }
    }

}