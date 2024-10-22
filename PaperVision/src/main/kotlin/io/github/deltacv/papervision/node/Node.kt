package io.github.deltacv.papervision.node

import imgui.ImGui
import imgui.ImVec2
import imgui.extension.imnodes.ImNodes
import io.github.deltacv.papervision.codegen.GeneratorsGenNode
import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.codegen.*
import io.github.deltacv.papervision.codegen.language.Language
import io.github.deltacv.papervision.exception.NodeGenException
import io.github.deltacv.papervision.gui.Font
import io.github.deltacv.papervision.gui.NodeEditor
import io.github.deltacv.papervision.id.DrawableIdElementBase
import io.github.deltacv.papervision.id.IdElementContainerStack
import io.github.deltacv.papervision.node.vision.OutputMatNode
import io.github.deltacv.papervision.serialization.data.DataSerializable
import io.github.deltacv.papervision.serialization.BasicNodeData
import io.github.deltacv.papervision.serialization.NodeSerializationData
import io.github.deltacv.papervision.util.event.PaperVisionEventHandler
import io.github.deltacv.papervision.util.event.EventListener

interface Type {
    val name: String
}

abstract class Node<S: CodeGenSession>(
    private var allowDelete: Boolean = true,
    val joinActionStack: Boolean = true
) : DrawableIdElementBase<Node<*>>(), GeneratorsGenNode<S>, DataSerializable<NodeSerializationData> {

    override val idElementContainer = IdElementContainerStack.threadStack.peekNonNull<Node<*>>()
    override val requestedId get() = serializedId

    val description by lazy { this::class.java.getAnnotation(PaperNode::class.java)?.description }

    var serializedId: Int? = null
        private set

    var showAttributesCircles = true

    // will be set on NodeEditor#draw
    lateinit var editor: NodeEditor
        internal set

    // will be set on NodeEditor#draw or NodeList#draw
    lateinit var fontAwesome: Font
        internal set

    val isOnEditor get() = ::editor.isInitialized && idElementContainer.contains(this)

    // it is the responsibility of the inheriting class to set this value in draw()
    val position = ImVec2()

    override val generators = mutableMapOf<Language, Generator<S>>()

    override val genOptions = CodeGenOptions()

    override var lastGenSession: S? = null

    val onChange = PaperVisionEventHandler("${this::class.java.simpleName}-OnChange")
    val onDelete = PaperVisionEventHandler("OnDelete-${this::class.simpleName}")

    private val attribOnChangeListener = EventListener {
        onChange.run()
    }

    @Transient
    private val attribs = mutableListOf<Attribute>() // internal mutable list

    val nodeAttributes = attribs as List<Attribute> // public read-only

    protected fun drawAttributes() {
        for((i, attribute) in nodeAttributes.withIndex()) {
            attribute.parentNode = this
            attribute.draw()

            if(i < nodeAttributes.size - 1 && !attribute.wasLastDrawCancelled) {
                ImGui.newLine() // make a new blank line if this isn't the last attribute
            }
        }
    }

    override fun delete() {
        if(allowDelete) {
            forceDelete()
        }
    }

    fun forceDelete() {
        for (attribute in nodeAttributes.toTypedArray()) {
            attribute.delete()
        }

        idElementContainer.removeId(id)
        onDelete.run()
    }

    override fun restore() {
        if(allowDelete) {
            forceRestore()
        }
    }

    fun forceRestore() {
        for (attribute in nodeAttributes.toTypedArray()) {
            attribute.restore()
        }

        idElementContainer[id] = this

        if(this is DrawNode<*>) {
            nextNodePosition = position
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

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        raise("Node doesn't have output attributes")
    }

    fun hasDeadEnd(initialNode: Node<*> = this): Boolean {
        for(attribute in nodeAttributes) {
            if(attribute.mode == AttributeMode.INPUT) {
                continue
            }

            for(linkedAttribute in attribute.enabledLinkedAttributes()) {
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
                for(linkedAttribute in attribute.enabledLinkedAttributes()) {
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

        // Propagate FIRST to all nodes that are not dead ends
        completePathNodes.forEach { it.receivePropagation(current) }
        // Propagate to dead ends, so they can be processed last
        deadEndNodes.forEach { it.receivePropagation(current) }
    }

    open fun makeSerializationData() = BasicNodeData(id, ImVec2().apply {
        ImNodes.getNodeEditorSpacePos(id) // TODO: Fix this to get the actual position when SpaiR decides to merge my PR...
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

        // val pos = ImVec2()
        // ImNodes.getNodeEditorSpacePos(id) // TODO: Fix this to get the actual position when SpaiR decides to merge my PR...
        data.nodePos = position

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
                    return true
            }

            return false
        }
    }

}